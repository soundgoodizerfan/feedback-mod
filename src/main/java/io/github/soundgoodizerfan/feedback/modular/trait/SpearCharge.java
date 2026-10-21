/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        if (charging)
            return;
        Vec3 eye = attacker.getEyePosition();
        if (closest(event.getEntity().getBoundingBox(), eye).distanceTo(eye) < params.get("min_range", 2f))
            event.setCanceled(true);
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
        return ModularTraits.of(stack).filter(a -> a.trait() instanceof SpearCharge).map(ModularTraits.Active::params);
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
            if (distance < minRange || distance > reach || point.subtract(eye).normalize().dot(look) < 0.9)
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
        float damage = damage(weapon, speed, params);
        var source = user instanceof Player p ? user.damageSources().playerAttack(p) : user.damageSources().mobAttack(user);
        charging = true;
        try {
            target.hurt(source, damage);
        } finally {
            charging = false;
        }
        if (knocks && speed >= params.get("knockback_speed", 5.1f))
            target.knockback(0.4 + 0.05 * speed, Mth.sin(user.getYRot() * Mth.DEG_TO_RAD), -Mth.cos(user.getYRot() * Mth.DEG_TO_RAD));
        if (dismounts && target.isPassenger())
            target.stopRiding();
        weapon.hurtAndBreak(ModularStacks.cap(weapon, 1 + (int) (speed / params.get("wear_speed", 6f))), user, EquipmentSlot.MAINHAND);
    }

    /** The charge's damage for a relative speed (blocks a second): the formula in the class javadoc. */
    private static float damage(ItemStack weapon, float speed, Params params) {
        ModularDesign design = ModularStacks.design(weapon);
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
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
