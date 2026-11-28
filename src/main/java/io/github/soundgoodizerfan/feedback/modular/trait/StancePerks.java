// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

/**
 * The perks that are a stance or a kill, not a blow: worn ones (anchor, glide, shroud), kill ones (covet, relentless, verdant, shard).
 * Perks that other code reads by presence (phase, salvage, versatile, rapport) are {@link #MARKER}s; {@link PerkEvents},
 * {@code ModularStacks} and {@code VillageEconomy} ask for them with {@link ModularTraits#has}.
 */
public final class StancePerks {

    private StancePerks() {
    }

    /** A perk with no hook of its own: something else asks the stack whether it has it. */
    public static final ModularTrait MARKER_PHASE = new ModularTrait() { };
    public static final ModularTrait MARKER_SALVAGE = new ModularTrait() { };
    public static final ModularTrait MARKER_VERSATILE = new ModularTrait() { };
    public static final ModularTrait MARKER_RAPPORT = new ModularTrait() { };

    /** Anchor: worn, the wearer is not knocked back. */
    public static final class Anchor implements ModularTrait {
        @Override
        public void onWornKnockback(LivingKnockBackEvent event, LivingEntity wearer, ItemStack piece, Params params) {
            event.setCanceled(true);
        }
    }

    /** Glide: worn, sneaking in the air holds the fall to a slow drift. */
    public static final class Glide implements ModularTrait {
        @Override
        public void onWornTick(Player wearer, ItemStack piece, Params params) {
            if (wearer.onGround() || !wearer.isShiftKeyDown() || wearer.isInWater() || wearer.isFallFlying()
                    || wearer.getDeltaMovement().y >= -ModularTuning.PERK_GLIDE_FALL_SPEED)
                return;
            net.minecraft.world.phys.Vec3 move = wearer.getDeltaMovement();
            wearer.setDeltaMovement(move.x, -ModularTuning.PERK_GLIDE_FALL_SPEED, move.z);
            wearer.fallDistance = 0f;
            wearer.hurtMarked = true;
        }
    }

    /** Shroud: worn, creatures see the wearer less in the dark. */
    public static final class Shroud implements ModularTrait {
        @Override
        public void onWornVisibility(LivingEvent.LivingVisibilityEvent event, LivingEntity wearer, ItemStack piece, Params params) {
            if (wearer.level().getMaxLocalRawBrightness(wearer.blockPosition()) <= ModularTuning.PERK_SHROUD_LIGHT)
                event.modifyVisibility(ModularTuning.PERK_SHROUD_VISIBILITY);
        }
    }

    /** Covet: a kill rolls the victim's loot table once more. */
    public static final class Covet implements ModularTrait {
        @Override
        public void onKill(LivingDeathEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
            LivingEntity victim = event.getEntity();
            if (!(victim.level() instanceof ServerLevel level) || !level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT))
                return;
            LootParams.Builder builder = new LootParams.Builder(level);
            if (attacker instanceof Player player)
                builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player);
            LootParams loot = builder
                    .withParameter(LootContextParams.THIS_ENTITY, victim)
                    .withParameter(LootContextParams.ORIGIN, victim.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, event.getSource())
                    .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, attacker)
                    .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, event.getSource().getDirectEntity())
                    .create(LootContextParamSets.ENTITY);
            level.getServer().reloadableRegistries().getLootTable(victim.getLootTable()).getRandomItems(loot).forEach(victim::spawnAtLocation);
        }
    }

    /** Relentless: a kill leaves the next swing fully charged. */
    public static final class Relentless implements ModularTrait {
        @Override
        public void onKill(LivingDeathEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
            if (attacker instanceof Player player)
                player.attackStrengthTicker = (int) player.getCurrentItemAttackStrengthDelay() + 1;
        }
    }

    /** Verdant: a kill feeds the ground where the victim fell as bone meal does. */
    public static final class Verdant implements ModularTrait {
        @Override
        public void onKill(LivingDeathEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
            LivingEntity victim = event.getEntity();
            if (!(victim.level() instanceof ServerLevel level))
                return;
            net.minecraft.core.BlockPos feet = victim.blockPosition();
            ItemStack meal = new ItemStack(net.minecraft.world.item.Items.BONE_MEAL);
            if (net.minecraft.world.item.BoneMealItem.growCrop(meal, level, feet) || net.minecraft.world.item.BoneMealItem.growCrop(meal, level, feet.below()))
                level.levelEvent(1505, feet, 15);
        }
    }

    /** Shard: a kill bursts into glass that cuts the creatures beside the victim. */
    public static final class Shard implements ModularTrait {
        private static boolean bursting;

        @Override
        public void onKill(LivingDeathEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
            LivingEntity victim = event.getEntity();
            if (bursting || !(victim.level() instanceof ServerLevel level))
                return;
            float cut = victim.getMaxHealth() * ModularTuning.PERK_SHARD_HEALTH_SHARE;
            // A shard's own kills do not burst again (no chain through a pack); no direct entity, so no blow perk fires on a cut.
            bursting = true;
            try {
                PerkEffects.beside(level, victim, attacker, ModularTuning.PERK_SHARD_BLOCKS)
                        .forEach(near -> near.hurt(level.damageSources().indirectMagic(null, attacker), cut));
            } finally {
                bursting = false;
            }
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()),
                    victim.getX(), victim.getY(0.5), victim.getZ(), 24, 0.4, 0.4, 0.4, 0.2);
            PerkEffects.sound(level, victim, SoundEvents.GLASS_BREAK, 1.2f);
        }
    }
}
