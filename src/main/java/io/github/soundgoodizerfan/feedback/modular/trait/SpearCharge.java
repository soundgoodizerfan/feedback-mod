// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularTraits;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.net.SpearStagePayload;

import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Spec 5.4, the spear: the newest Minecraft spear. Design from the Backported Spears port (CC0),
 * written afresh; nothing is copied.
 *
 * <p><b>Jab</b>: the ordinary attack at the baked reach, which cannot land on anything within
 * {@code min_range} blocks of the wielder (a point-blank spear is a stick).
 *
 * <p><b>Charge</b>: hold the use key (not vanilla item use, which would slow the wielder and stop
 * the sprint a charge needs). The client sends {@code SpearChargePayload} on every change of the key
 * and a heartbeat every few ticks; the server ends a charge that misses its heartbeat, that is
 * broken, or whose wielder dies, logs out, spectates or changes item. Each server player tick, every living thing in the reach cone (between
 * {@code min_range} and {@code charge_reach}) that the spear has not already struck this charge is
 * hit if the wielder and it close along the look line at {@code min_speed} blocks a second or
 * more. Velocity is tracked from per-tick positions here (a player's server delta movement is
 * not the player's motion). Damage is {@code damage_mult x strike x sqrt(head mass / ref_mass) x
 * relative speed}: the type's strike and the baked head mass, so a heavy head hits harder at the
 * same speed. It reads no attribute, so Strength and Weakness do nothing, and it cannot crit.
 * From {@code knockback_speed} the target is knocked back; in the first stage a rider is dismounted.
 *
 * <p><b>Stages</b> by ticks held: engaged ({@code engaged}: damage, knockback, dismount), tired
 * ({@code tired}: damage, knockback), disengaged ({@code disengaged}: damage only), then it ends.
 * Each length is scaled by {@code min(1, joint_ref / JOINT)}, floored at a quarter: a head heavier
 * than its binding tires sooner.
 *
 * <p><b>Cost</b>: a spear held out hits only at speed, a hit costs {@code 1 + relative speed /
 * wear_speed} points of wear, and the charge ends by itself. The jab loses its close range.
 *
 * <p><b>Tines and barb</b> (axes of the spear head): more tines spread the thrust, so every blow, jab or charge, is cut by
 * {@link #tineFactor} (a thinner section pierces armour less, and each tine past the first costs a share of the thrust) and
 * lands in a wider cone, on up to {@code tines} things at once. A barb tears the wound (a pulled target takes more), pulls the
 * target toward the wielder and holds the weapon up while it is worked free; a thrown barbed head on a lanyard wounds what it
 * reels ({@link ModularTuning#HELD_WOUND_MULT}), a line alone does not. A build balanced for the hand (baked {@code BALANCE}
 * below {@link ModularTuning#SPEAR_THROW_BALANCE}) is thrown on release and neither charges nor loses its jab's close range.
 *
 * <p>Animation: client-only ({@code client/SpearClient}): the arm held out in front while charging,
 * lowered when disengaged; other players are told the stage by {@code SpearStagePayload}.
 * <b>[OPEN]</b>: hitbox inflation and attacking through non-solid blocks (both need client-side
 * picking).
 */
public final class SpearCharge implements ModularTrait {

    /** One wielder's charge: ticks held, who has been struck, and last tick's positions. */
    private static final class Charge {
        int ticks;
        int stage;
        long beat;
        Vec3 wielder;
        final Set<UUID> struck = new HashSet<>();
        Map<UUID, Vec3> seen = new HashMap<>();
    }

    private static final Map<UUID, Charge> CHARGES = new HashMap<>();

    /** Wielders whose charge ran its course and who have not let go of the key since. */
    private static final Set<UUID> SPENT = new HashSet<>();

    /** True while the charge itself is hurting something, so the jab's close-range rule does not cancel it. */
    private static boolean charging;

    /** True while the jab's other tines are hurting their targets, so they do not fork in turn. */
    private static boolean tining;

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        if (charging || tining)
            return;
        LivingEntity target = event.getEntity();
        Vec3 eye = attacker.getEyePosition();
        if (!thrower(weapon) && closest(target.getBoundingBox(), eye).distanceTo(eye) < params.get("min_range", 2f)) {
            event.setCanceled(true);
            return;
        }
        float tines = tines(weapon);
        float barb = barb(weapon);
        float blow = event.getAmount();
        event.setAmount(blow * tineFactor(tines, target) * (1f + ModularTuning.BARB_TEAR_PER_BARB * barb));
        barbed(attacker, weapon, target, barb);
        int extra = Math.round(tines) - 1;
        if (extra <= 0)
            return;
        Vec3 look = attacker.getLookAngle();
        float reach = params.get("charge_reach", 4.5f);
        List<LivingEntity> caught = attacker.level().getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(reach),
                e -> e != attacker && e != target && e.isAlive() && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand)
                        && !attacker.isAlliedTo(e) && closest(e.getBoundingBox(), eye).distanceTo(eye) <= reach
                        && e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) > cone(tines));
        var source = io.github.soundgoodizerfan.feedback.modular.StrikeSource.of(
                attacker instanceof Player p ? attacker.damageSources().playerAttack(p) : attacker.damageSources().mobAttack(attacker), attacker, weapon);
        tining = true;
        try {
            caught.stream().sorted(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(attacker))).limit(extra)
                    .forEach(e -> {
                        e.hurt(source, blow * tineFactor(tines, e) * (1f + ModularTuning.BARB_TEAR_PER_BARB * barb));
                        barbed(attacker, weapon, e, barb);
                    });
        } finally {
            tining = false;
        }
    }

    /** The spear head part of {@code weapon}, the front face of it that is one. */
    private static ModularDesign.Part head(ItemStack weapon) {
        ModularDesign design = ModularStacks.design(weapon);
        if (design == null)
            return null;
        for (ModularDesign.Part face : io.github.soundgoodizerfan.feedback.modular.ModularFaces.faces(design))
            if (face.kind().equals("spear_head"))
                return face;
        return null;
    }

    /** The head's tine count (1 for a plain spear). */
    public static float tines(ItemStack weapon) {
        ModularDesign.Part head = head(weapon);
        return head == null ? 1f : Math.max(1f, head.axis("tines", 1f));
    }

    /** The head's barb, 0 for none. */
    public static float barb(ItemStack weapon) {
        ModularDesign.Part head = head(weapon);
        return head == null ? 0f : Math.max(0f, head.axis("barb", 0f));
    }

    /** Whether the build is thrown on release rather than charged: a spring on it, or balanced toward the middle of the haft. */
    public static boolean thrower(ItemStack weapon) {
        if (io.github.soundgoodizerfan.feedback.modular.SpringPart.of(weapon).isPresent())
            return true;
        ModularFigures figures = ModularStacks.figures(weapon);
        return figures != null && figures.get(Fig.BALANCE) < ModularTuning.SPEAR_THROW_BALANCE;
    }

    /**
     * The share of a blow one target takes from a head of {@code tines}: every tine past the first costs a share of the thrust, and
     * the thinner section pierces armour less, by {@code 1 - 1 / sqrt(tines)} of what the target's armour would have stopped.
     */
    public static float tineFactor(float tines, LivingEntity target) {
        float drag = 1f / (1f + ModularTuning.TINE_DRAG * (tines - 1f));
        double armour = target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        float stopped = (float) (armour / (armour + ModularTuning.TINE_ARMOUR_HALF));
        return drag * (1f - (1f - 1f / (float) Math.sqrt(tines)) * stopped);
    }

    /** The look-line match a blow needs to land: dead-on for one tine, wider as tines spread. */
    public static float cone(float tines) {
        return Mth.lerp(Mth.clamp((tines - 1f) / 4f, 0f, 1f), ModularTuning.TINE_CONE_NARROW, ModularTuning.TINE_CONE_WIDE);
    }

    /** A barbed hit pulls the target toward the wielder and holds the weapon up while the barb is worked out. */
    private static void barbed(LivingEntity attacker, ItemStack weapon, LivingEntity target, float barb) {
        if (barb <= 0f)
            return;
        Vec3 toward = attacker.position().subtract(target.position());
        if (toward.lengthSqr() > 1.0E-4) {
            toward = toward.normalize().scale(ModularTuning.BARB_PULL_PER_BARB * barb);
            target.push(toward.x, 0.05, toward.z);
            target.hurtMarked = true;
        }
        if (attacker instanceof Player player)
            player.getCooldowns().addCooldown(weapon.getItem(), Math.round(ModularTuning.BARB_WITHDRAW_TICKS * barb));
    }

    /** Ticks a charge may go without a heartbeat from the client before the server ends it. */
    private static final int BEAT_TIMEOUT = 25;

    /** The stage lengths in ticks, cumulative: engaged, tired, end. Client and server both read them. */
    public static int[] stages(ItemStack weapon, Params params) {
        ModularFigures figures = ModularStacks.figures(weapon);
        float joint = figures == null ? 0f : figures.get(Fig.JOINT);
        float scale = joint <= 0f ? 1f : Mth.clamp(params.get("joint_ref", 0.5f) / joint, 0.25f, 1f);
        int engaged = Math.round(params.get("engaged", 40f) * scale);
        int tired = engaged + Math.round(params.get("tired", 30f) * scale);
        return new int[] {engaged, tired, tired + Math.round(params.get("disengaged", 30f) * scale)};
    }

    /** The spear trait's numbers if {@code stack} is a spear, for the client's key handling. */
    public static java.util.Optional<Params> of(ItemStack stack) {
        return ModularTraits.of(stack).filter(a -> a.trait() instanceof SpearCharge && !thrower(stack)).map(ModularTraits.Active::params);
    }

    /** The client holds (or lets go of) the use key. Starts, refreshes or ends the sender's charge. */
    public static void hold(ServerPlayer player, boolean held) {
        if (!held) {
            SPENT.remove(player.getUUID());
            end(player);
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!player.isAlive() || player.isSpectator() || ModularStacks.broken(weapon) || of(weapon).isEmpty()
                || SPENT.contains(player.getUUID()))
            return;
        Charge charge = CHARGES.get(player.getUUID());
        if (charge == null) {
            charge = new Charge();
            charge.wielder = player.position();
            CHARGES.put(player.getUUID(), charge);
            tellStage(player, charge, 1);
        }
        charge.beat = player.level().getGameTime();
    }

    private static void end(ServerPlayer player) {
        Charge charge = CHARGES.remove(player.getUUID());
        if (charge != null)
            tellStage(player, charge, 0);
    }

    private static void tellStage(ServerPlayer player, Charge charge, int stage) {
        if (charge.stage != stage) {
            charge.stage = stage;
            PacketDistributor.sendToPlayersTrackingEntity(player, new SpearStagePayload(player.getId(), stage));
        }
    }

    private static void tick(ServerPlayer user, Charge charge) {
        ItemStack weapon = user.getMainHandItem();
        Params params = of(weapon).orElse(null);
        if (params == null || !user.isAlive() || user.isSpectator() || ModularStacks.broken(weapon)
                || user.level().getGameTime() - charge.beat > BEAT_TIMEOUT) {
            end(user);
            return;
        }
        int[] stages = stages(weapon, params);
        int engaged = stages[0];
        int tired = stages[1];
        if (charge.ticks >= stages[2]) {
            SPENT.add(user.getUUID());
            end(user);
            return;
        }
        tellStage(user, charge, charge.ticks < engaged ? 1 : charge.ticks < tired ? 2 : 3);
        Level level = user.level();

        Vec3 look = user.getLookAngle();
        Vec3 eye = user.getEyePosition();
        float reach = params.get("charge_reach", 4.5f);
        float minRange = params.get("min_range", 2f);
        Vec3 wielderVelocity = user.position().subtract(charge.wielder);
        charge.wielder = user.position();

        Map<UUID, Vec3> now = new HashMap<>();
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(reach + 2),
                e -> e != user && e.isAlive() && !e.isPassengerOfSameVehicle(user));
        for (LivingEntity target : near) {
            Vec3 before = charge.seen.get(target.getUUID());
            now.put(target.getUUID(), target.position());
            if (before == null || charge.struck.contains(target.getUUID()))
                continue;
            Vec3 point = closest(target.getBoundingBox(), eye);
            double distance = point.distanceTo(eye);
            if (distance < minRange || distance > reach || point.subtract(eye).normalize().dot(look) < cone(tines(weapon)))
                continue;
            // blocks a second the two close along the look line; per-tick deltas are x20
            double speed = wielderVelocity.subtract(target.position().subtract(before)).dot(look) * 20;
            if (speed < params.get("min_speed", 4.6f))
                continue;
            charge.struck.add(target.getUUID());
            strike(user, weapon, target, (float) speed, charge.ticks < engaged, charge.ticks < tired, params);
        }
        charge.seen = now;
        charge.ticks++;
    }

    private static void strike(LivingEntity user, ItemStack weapon, LivingEntity target, float speed,
                               boolean dismounts, boolean knocks, Params params) {
        float tines = tines(weapon);
        float barb = barb(weapon);
        float damage = damage(weapon, speed, params) * tineFactor(tines, target) * (1f + ModularTuning.BARB_TEAR_PER_BARB * barb);
        var source = user instanceof Player p ? user.damageSources().playerAttack(p) : user.damageSources().mobAttack(user);
        charging = true;
        try {
            target.hurt(source, damage);
        } finally {
            charging = false;
        }
        io.github.soundgoodizerfan.feedback.core.EventTrigger.fire(user instanceof Player pl ? pl : null, "progress/combat/spear_point");
        if (knocks && speed >= params.get("knockback_speed", 5.1f))
            target.knockback(0.4 + 0.05 * speed, Mth.sin(user.getYRot() * Mth.DEG_TO_RAD), -Mth.cos(user.getYRot() * Mth.DEG_TO_RAD));
        if (dismounts && target.isPassenger())
            target.stopRiding();
        barbed(user, weapon, target, barb);
        weapon.hurtAndBreak(ModularStacks.cap(weapon, 1 + (int) (speed / params.get("wear_speed", 6f))), user, EquipmentSlot.MAINHAND);
    }

    /** The charge's damage for a relative speed (blocks a second): the formula in the class javadoc. */
    private static float damage(ItemStack weapon, float speed, Params params) {
        ModularDesign design = ModularStacks.design(weapon);
        ModularType type = design == null ? null : ModularTypes.known(design.type());
        ModularFigures figures = ModularStacks.figures(weapon);
        float strike = type == null ? 1f : type.strike();
        float mass = figures == null ? params.get("ref_mass", 2f) : figures.get(Fig.MASS);
        return params.get("damage_mult", 0.6f) * strike * (float) Math.sqrt(mass / params.get("ref_mass", 2f)) * speed;
    }

    private static Vec3 closest(AABB box, Vec3 p) {
        return new Vec3(Mth.clamp(p.x, box.minX, box.maxX), Mth.clamp(p.y, box.minY, box.maxY), Mth.clamp(p.z, box.minZ, box.maxZ));
    }

    /** The events a trait class cannot hold itself: a charge is dropped with its wielder, and a spear cannot crit. */
    @EventBusSubscriber(modid = Feedback.MOD_ID)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            CHARGES.remove(event.getEntity().getUUID());
            SPENT.remove(event.getEntity().getUUID());
        }

        @SubscribeEvent
        public static void onTick(PlayerTickEvent.Post event) {
            if (!CHARGES.isEmpty() && event.getEntity() instanceof ServerPlayer player) {
                Charge charge = CHARGES.get(player.getUUID());
                if (charge != null)
                    tick(player, charge);
            }
        }

        @SubscribeEvent
        public static void onDeath(LivingDeathEvent event) {
            CHARGES.remove(event.getEntity().getUUID());
            SPENT.remove(event.getEntity().getUUID());
        }

        @SubscribeEvent
        public static void onCrit(CriticalHitEvent event) {
            if (ModularTraits.of(event.getEntity().getMainHandItem()).filter(a -> a.trait() instanceof SpearCharge).isPresent())
                event.setCriticalHit(false);
        }
    }
}
