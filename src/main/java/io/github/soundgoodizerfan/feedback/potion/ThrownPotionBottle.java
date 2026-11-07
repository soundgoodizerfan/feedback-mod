// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.core.hazard.Hazards;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureBurst;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FEntities;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

/**
 * A thrown bottle (potions spec 5). On impact its potion reaches every body within {@link PotionTuning#throwReach()}, the nearer the
 * fuller: the level stays, the length falls off with distance, and the potion's potency applies as when drunk. The fire node is a
 * fast heat release instead of an effect: bodies in reach take fire-tagged heat damage by distance and are set alight, flammable
 * neighbours of the burst catch ({@code HotIgnition}'s rule), and the burst hisses ({@code PressureBurst.hiss}); no blocks are blown out.
 * The explosion node detonates where the bottle lands.
 */
public class ThrownPotionBottle extends ThrowableItemProjectile {

    public ThrownPotionBottle(EntityType<? extends ThrownPotionBottle> type, Level level) {
        super(type, level);
    }

    public ThrownPotionBottle(Level level, LivingEntity thrower) {
        super(FEntities.POTION_BOTTLE.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return FItems.POTION.get();
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(level() instanceof ServerLevel server))
            return;
        PotionState state = getItem().get(FDataComponents.POTION_STATE.get());
        PotionEffects.impact(server, getOwner(), this, position(), state, PotionTuning.throwReach());
        server.sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 12, 0.3, 0.2, 0.3, 0.1);
        server.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 0.8f, 1f);
        discard();
    }
}
