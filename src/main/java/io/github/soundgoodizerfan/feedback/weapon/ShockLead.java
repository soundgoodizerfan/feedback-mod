// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.kick.Kicking;
import io.github.soundgoodizerfan.feedback.combat.tether.Tether;
import io.github.soundgoodizerfan.feedback.combat.tether.Tethers;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.modular.bake.ElectricGunSpec;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.item.ModularElectricGun;
import io.github.soundgoodizerfan.feedback.net.EnergyBeamPayload;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The shock lead: a reel and a plug on a line from the gun, a taser's wire probes. The reload key throws the plug at the body in front of
 * the wielder (within the reel's line); from then the gun's battery drains down the line, and the charge that builds on the body grows with
 * time on the line, not at once. It discharges on the next press of the key, when the charge reaches what the line can carry (the line's
 * ampacity), when the cell runs flat, or after {@link ModularTuning#EGUN_LEAD_MAX_TICKS}. The discharge hurts the body as a pulse does and
 * arcs on from it as an arc fork's does ({@link PulseShot#arc}, one rule). A wet or metal-armoured body takes charge faster and hurts more;
 * a line the wielder trails through water shorts to them (unless insulated) and goes dead. The line breaks when it is pulled past its
 * length or something solid comes between the plug and the gun. The line is a {@link Tether} (its own source id, no reel, no pull); the
 * tether does the drawing. One lead per wielder.
 *
 * <p>Not built: a plug biting a block (a lightning rod).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ShockLead {

    private record Lead(UUID target, int ticks, float joules) {
    }

    private static final Map<UUID, Lead> LEADS = new HashMap<>();

    private ShockLead() {
    }

    /** The tether source id of {@code wielder}'s lead: not their uuid, which a grab already uses. */
    private static UUID lineId(UUID wielder) {
        return new UUID(wielder.getMostSignificantBits() ^ 0x5E0CAD1EADL, wielder.getLeastSignificantBits());
    }

    /** The reload key on a gun with a lead: throws the plug, or discharges the one that is in. */
    public static void press(ServerLevel level, Player player, ItemStack gun, ElectricGunSpec spec) {
        Lead lead = LEADS.get(player.getUUID());
        if (lead != null) {
            if (level.getEntity(lead.target()) instanceof LivingEntity target)
                discharge(level, player, gun, target, lead.joules());
            end(level.getServer(), player.getUUID(), level.getEntity(lead.target()));
            return;
        }
        var picked = Kicking.pick(player, player.getEyePosition(), player.getLookAngle(), spec.leadLength());
        ItemStack cell = ModularElectricGun.source(player, gun, spec).battery();
        if (picked.isEmpty() || spec.leadAmp() <= 0f || cell.isEmpty() || !BatteryStack.ready(BatteryStack.state(cell)) || BatteryStack.charge(BatteryStack.state(cell)) <= 0f
                || blocked(level, player, picked.get())) {
            level.playSound(null, player.blockPosition(), FSounds.GUN_EGUN_FLAT.get(), SoundSource.PLAYERS, 0.6f, 1.4f);
            return;
        }
        LivingEntity target = picked.get();
        Tethers.add(target, new Tether(lineId(player.getUUID()), new Tether.Anchor.OnEntity(player.getId()), spec.leadLength(), 0f, 1f,
                new Tether.Release(0, ModularTuning.EGUN_LEAD_MAX_TICKS + 20)));
        LEADS.put(player.getUUID(), new Lead(target.getUUID(), 0, 0f));
        level.playSound(null, player.blockPosition(), FSounds.GUN_EGUN_SPLIT.get(), SoundSource.PLAYERS, 0.8f, 1.6f);
    }

    /** Whether a solid block stands between the wielder's eye and the body's middle. */
    private static boolean blocked(ServerLevel level, LivingEntity player, LivingEntity target) {
        return level.clip(new ClipContext(player.getEyePosition(), target.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty())).getType() != HitResult.Type.MISS;
    }

    private static void end(MinecraftServer server, UUID wielder, net.minecraft.world.entity.Entity target) {
        LEADS.remove(wielder);
        if (target instanceof LivingEntity living)
            Tethers.remove(living, lineId(wielder));
    }

    /** The gun in {@code player}'s hands that carries a lead, or empty. */
    private static ItemStack leadGun(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = player.getItemInHand(hand);
            ElectricGunSpec spec = s.getItem() instanceof ModularElectricGun ? ModularElectricGun.specOf(s) : null;
            if (spec != null && spec.lead())
                return s;
        }
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (LEADS.isEmpty())
            return;
        MinecraftServer server = event.getServer();
        for (Iterator<Map.Entry<UUID, Lead>> it = LEADS.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            Lead lead = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            LivingEntity target = player == null ? null : player.serverLevel().getEntity(lead.target()) instanceof LivingEntity l ? l : null;
            ItemStack gun = player == null ? ItemStack.EMPTY : leadGun(player);
            ElectricGunSpec spec = gun.isEmpty() ? null : ModularElectricGun.specOf(gun);
            if (target != null && !target.isAlive() || target == null || spec == null || !player.isAlive()
                    || player.distanceTo(target) > spec.leadLength() || blocked(player.serverLevel(), player, target)) {
                // Past the line's length, round a corner, or one end gone: the line breaks and nothing discharges.
                it.remove();
                if (target != null)
                    Tethers.remove(target, lineId(entry.getKey()));
                else
                    server.getAllLevels().forEach(l -> {
                        if (l.getEntity(lead.target()) instanceof LivingEntity held)
                            Tethers.remove(held, lineId(entry.getKey()));
                    });
                continue;
            }
            ServerLevel level = player.serverLevel();
            ModularElectricGun.Source source = ModularElectricGun.source(player, gun, spec);
            CompoundTag drawn = BatteryStack.state(source.battery()).copy();
            var band = BatteryStack.caseBand(drawn);
            // A line trailed through the wielder's own water shorts to them, unless they are insulated; the line goes dead.
            if (player.isInWaterOrRain() && !PulseShot.insulated(player, band)) {
                level.playSound(null, player.blockPosition(), FSounds.GUN_EGUN_SHOCK.get(), SoundSource.PLAYERS, 0.9f, 1.2f);
                player.hurt(player.damageSources().lightningBolt(), ModularTuning.EGUN_LEAD_SHORT_DAMAGE);
                it.remove();
                Tethers.remove(target, lineId(entry.getKey()));
                continue;
            }
            float took = source.battery().isEmpty() || !BatteryStack.ready(drawn) ? 0f : BatteryStack.draw(drawn, ModularTuning.EGUN_LEAD_EU_PER_TICK);
            if (took > 0f) {
                ItemStack out = source.battery().copy();
                out.set(FDataComponents.BATTERY.get(), drawn);
                source.write().accept(out);
            }
            boolean wet = target.isInWaterOrRain() || PulseShot.conductive(target);
            float joules = lead.joules() + took * ModularTuning.EGUN_J_PER_EU * ModularTuning.EGUN_LEAD_EFFICIENCY
                    * (wet ? 1f + ModularTuning.EGUN_SHOCK_BONUS : 1f);
            float cap = ModularTuning.EGUN_LEAD_CAP_J * Mth.clamp(spec.leadAmp(), 0.1f, 3f);
            int ticks = lead.ticks() + 1;
            if (took <= 0f || joules >= cap || ticks >= ModularTuning.EGUN_LEAD_MAX_TICKS) {
                discharge(level, player, gun, target, Math.min(joules, cap));
                it.remove();
                Tethers.remove(target, lineId(entry.getKey()));
                continue;
            }
            entry.setValue(new Lead(lead.target(), ticks, joules));
            if (ticks % 4 == 0) {
                Vec3 muzzle = player.getEyePosition().add(player.getLookAngle().scale(0.4));
                Vec3 at = target.getBoundingBox().getCenter();
                EnergyBeamPayload.send(level, EnergyStyle.ARC, ModularElectricGun.colourOf(gun), muzzle, at, 0.25f);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 2, 0.2, 0.3, 0.2, 0.05);
            }
            if (ticks % 12 == 0)
                level.playSound(null, target.blockPosition(), FSounds.GUN_EGUN_FORK.get(), SoundSource.PLAYERS, 0.35f, 1.2f + 0.8f * joules / cap);
        }
    }

    /** What the line built goes into the body, and arcs on from it to the nearest wet or conductive one as an arc fork's does. */
    private static void discharge(ServerLevel level, Player player, ItemStack gun, LivingEntity target, float joules) {
        if (joules <= 0f)
            return;
        ElectricGunSpec spec = ModularElectricGun.specOf(gun);
        var cell = spec == null ? ItemStack.EMPTY : ModularElectricGun.source(player, gun, spec).battery();
        var band = cell.isEmpty() ? ModularTuning.EGUN_REF_BAND : BatteryStack.caseBand(BatteryStack.state(cell));
        int colour = ModularElectricGun.colourOf(gun);
        PulseShot.hurt(level, player, target, joules * ModularTuning.EGUN_DAMAGE_PER_J, joules, band);
        Vec3 at = target.getBoundingBox().getCenter();
        GunImpacts.energyBody(level, at, new Vec3(0, 1, 0), 0.9f, colour, EnergyStyle.ARC);
        level.playSound(null, target.blockPosition(), FSounds.GUN_EGUN_SHOCK.get(), SoundSource.PLAYERS, 1f, 0.9f);
        PulseShot.arc(level, player, colour, band, target, joules);
    }

    /** A wielder who left drops the line. */
    public static void drop(MinecraftServer server, UUID wielder) {
        Lead lead = LEADS.remove(wielder);
        if (lead == null)
            return;
        for (ServerLevel l : server.getAllLevels())
            if (l.getEntity(lead.target()) instanceof LivingEntity held)
                Tethers.remove(held, lineId(wielder));
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        LEADS.clear();
    }
}
