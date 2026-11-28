// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import java.util.List;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The blow perks that change the blow itself or the wielder, so they read the damage event: held only (a plate cannot swing and a machine
 * head has no wielder). Cooldowns live on the wielder ({@link PerkEffects#ready}) and cost nothing between blows.
 */
public final class BlowPerks {

    private BlowPerks() {
    }

    /** Etch: acid in the edge eats the target's armour, so the blow ignores a share of it. */
    public static final class Etch implements ModularTrait {
        @Override
        public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
            event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * (1f - ModularTuning.PERK_ETCH_ARMOUR_SHARE));
        }
    }

    /** Leech: the wielder takes a share of the blow's damage back as health. */
    public static final class Leech implements ModularTrait {
        private static final WielderState<Long> COOL = new WielderState<>();

        @Override
        public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
            if (attacker.getHealth() < attacker.getMaxHealth() && PerkEffects.ready(COOL, attacker, ModularTuning.PERK_LEECH_COOLDOWN_TICKS)) {
                attacker.heal(event.getAmount() * ModularTuning.PERK_LEECH_SHARE);
                if (attacker.level() instanceof ServerLevel level)
                    level.sendParticles(ParticleTypes.HEART, attacker.getX(), attacker.getY(1.0), attacker.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
            }
        }
    }

    /** Exchange: wielder and target trade places. */
    public static final class Exchange implements ModularTrait {
        private static final WielderState<Long> COOL = new WielderState<>();

        @Override
        public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
            LivingEntity target = event.getEntity();
            if (!(attacker.level() instanceof ServerLevel level) || target.distanceTo(attacker) > ModularTuning.PERK_EXCHANGE_MAX_BLOCKS
                    || PerkEffects.cooling(COOL, attacker))
                return;
            double ax = attacker.getX(), ay = attacker.getY(), az = attacker.getZ();
            double tx = target.getX(), ty = target.getY(), tz = target.getZ();
            if (!attacker.randomTeleport(tx, ty, tz, true))
                return;
            // The target must land where the wielder stood, or the wielder goes back: no one is left stacked on the other.
            if (!target.randomTeleport(ax, ay, az, true))
                attacker.randomTeleport(ax, ay, az, true);
            else {
                PerkEffects.start(COOL, attacker, ModularTuning.PERK_EXCHANGE_COOLDOWN_TICKS);
                PerkEffects.sound(level, attacker, SoundEvents.CHORUS_FRUIT_TELEPORT, 1.1f);
            }
        }
    }

    /** Arc: the blow jumps to creatures beside the target, each taking a share. */
    public static final class Arc implements ModularTrait {
        private static final WielderState<Long> COOL = new WielderState<>();

        @Override
        public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
            if (!(attacker.level() instanceof ServerLevel level) || PerkEffects.cooling(COOL, attacker))
                return;
            List<LivingEntity> next = PerkEffects.beside(level, event.getEntity(), attacker, ModularTuning.PERK_ARC_BLOCKS).stream()
                    .limit(ModularTuning.PERK_ARC_TARGETS).toList();
            if (next.isEmpty())
                return;
            PerkEffects.start(COOL, attacker, ModularTuning.PERK_ARC_COOLDOWN_TICKS);
            // No direct entity on the arc's damage (credit still goes to the wielder), so none of the wielder's perks fires again on it.
            for (LivingEntity victim : next) {
                victim.hurt(level.damageSources().indirectMagic(null, attacker), event.getAmount() * ModularTuning.PERK_ARC_SHARE);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY(0.6), victim.getZ(), 10, 0.3, 0.4, 0.3, 0.2);
            }
        }
    }

    /** Brace: landing a blow, and (worn) being struck, braces the holder with a moment of resistance. */
    public static final class Brace implements ModularTrait {
        private static final WielderState<Long> COOL = new WielderState<>();

        private static void brace(LivingEntity holder) {
            if (PerkEffects.ready(COOL, holder, ModularTuning.PERK_BRACE_COOLDOWN_TICKS))
                holder.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ModularTuning.PERK_BRACE_TICKS, 0, true, true));
        }

        @Override
        public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
            brace(attacker);
        }

        @Override
        public void onWornHurt(LivingIncomingDamageEvent event, LivingEntity wearer, ItemStack piece, Params params) {
            brace(wearer);
        }
    }

    /** Swift: a landed blow quickens the wielder. */
    public static final class Swift implements ModularTrait {
        private static final WielderState<Long> COOL = new WielderState<>();

        @Override
        public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
            if (PerkEffects.ready(COOL, attacker, ModularTuning.PERK_SWIFT_COOLDOWN_TICKS))
                attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ModularTuning.PERK_SWIFT_TICKS, 1, true, true));
        }
    }

    /** Exact: a swing that was not fully charged lands as a full one would. */
    public static final class Exact implements ModularTrait {
        /** The charge each player's swing began with, read when the swing started ({@link PerkEvents#onAttackStart}): by the time damage lands vanilla may have reset it. */
        private static final WielderState<Float> CHARGE = new WielderState<>();

        static void capture(Player player) {
            CHARGE.put(player.getUUID(), player.getAttackStrengthScale(0.5f));
        }

        @Override
        public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
            if (attacker instanceof Player player) {
                Float seen = CHARGE.get(player.getUUID());
                float scale = seen != null ? seen : player.getAttackStrengthScale(0.5f);
                if (scale < 1f)
                    event.setAmount(event.getAmount() / (0.2f + scale * scale * 0.8f));
            }
        }
    }

    /** Ratchet: each blow on the same target soon after the last hits a little harder, up to a most. */
    public static final class Ratchet implements ModularTrait {
        private record Run(UUID target, int stack, long last) {
        }

        private static final WielderState<Run> RUNS = new WielderState<>();

        @Override
        public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
            long now = attacker.level().getGameTime();
            Run run = RUNS.get(attacker.getUUID());
            UUID target = event.getEntity().getUUID();
            int stack = run != null && run.target().equals(target) && now - run.last() <= ModularTuning.PERK_RATCHET_WINDOW_TICKS
                    ? Math.min(ModularTuning.PERK_RATCHET_MAX, run.stack() + 1) : 0;
            RUNS.put(attacker.getUUID(), new Run(target, stack, now));
            event.setAmount(event.getAmount() * (1f + stack * ModularTuning.PERK_RATCHET_PER_STACK));
        }
    }
}
