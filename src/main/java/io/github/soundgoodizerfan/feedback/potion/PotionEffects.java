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
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.Feedback;

import io.github.soundgoodizerfan.feedback.core.hazard.Hazards;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureBurst;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * What an effect node's {@code effect} id does to a body. A registered {@code MobEffect} is applied as one; an id no registry holds is a
 * special the potion map can name without a new class: {@link #IGNITE} sets the body alight, {@link #EXPLOSION} is a fast heat release
 * (see {@link #burst}, {@link ThrownPotionBottle}); {@link #apply} leaves a body untouched by it, {@link #consume} is the drinker's share.
 */
public final class PotionEffects {

    public static final ResourceLocation IGNITE = Feedback.id("ignite"), EXPLOSION = Feedback.id("explosion");

    private PotionEffects() {}

    /** Applies the effect to {@code target} for {@code ticks} at level {@code amplifier} (0 is I). */
    public static void apply(ResourceLocation id, LivingEntity target, int amplifier, int ticks) {
        if (IGNITE.equals(id))
            target.igniteForSeconds(ticks / 20f);
        else
            BuiltInRegistries.MOB_EFFECT.getHolder(id).ifPresent(e -> target.addEffect(new MobEffectInstance(e, ticks, amplifier)));
    }

    /**
     * Drinking or eating the potion: the same effect with the body at the centre. {@link #IGNITE} burns for the length times one plus the
     * level; {@link #EXPLOSION} is the thrown burst at distance zero, full undistanced heat damage (fire-tagged, so armour and fire
     * resistance answer it) and no block damage.
     */
    public static void consume(ResourceLocation id, LivingEntity target, int amplifier, int ticks) {
        if (EXPLOSION.equals(id)) {
            if (target.level() instanceof ServerLevel server) {
                target.hurt(Hazards.source(server, Hazards.HOT_CONTACT), PotionTuning.BLAST_DAMAGE * PotionTuning.DRINK_BLAST_FACTOR * (1 + amplifier));
                target.igniteForSeconds(PotionTuning.BLAST_BURN_SECONDS * PotionTuning.DRINK_BLAST_FACTOR);
                burst(server, target.blockPosition());
            }
        } else if (IGNITE.equals(id))
            target.igniteForSeconds(ticks / 20f * (1 + amplifier));
        else
            apply(id, target, amplifier, ticks);
    }

    public static Component name(ResourceLocation id) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(id).map(h -> h.value().getDisplayName()).orElseGet(() -> Component.translatable(id.toLanguageKey("effect")));
    }

    public static void burst(ServerLevel level, BlockPos at) {
        PressureBurst.hiss(level, at);
        level.sendParticles(ParticleTypes.EXPLOSION, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 1, 0, 0, 0, 0);
        level.playSound(null, at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 0.8f, 1.4f);
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1)))
            if (level.getBlockState(p).isAir() && BaseFireBlock.canBePlacedAt(level, p, Direction.UP))
                for (Direction d : Direction.values())
                    if (level.getBlockState(p.relative(d)).isFlammable(level, p.relative(d), d.getOpposite())) {
                        level.setBlockAndUpdate(p.immutable(), BaseFireBlock.getState(level, p));
                        break;
                    }
    }}
