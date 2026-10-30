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
 * fuller: the level stays, the length falls off with distance, and the potion's potency applies as when drunk. The explosion node is a
 * fast heat release instead of an effect: bodies in reach take fire-tagged heat damage by distance and are set alight, flammable
 * neighbours of the burst catch ({@code HotIgnition}'s rule), and the burst hisses ({@code PressureBurst.hiss}); no blocks are blown out.
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
        int amplifier = state == null || state.dud() ? -1 : state.amplifierNow(server);
        if (amplifier >= 0) {
            var id = state.effect().get();
            float reach = PotionTuning.throwReach();
            boolean blast = PotionEffects.EXPLOSION.equals(id);
            for (LivingEntity body : server.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(reach))) {
                float near = 1f - (float) Math.sqrt(body.distanceToSqr(position())) / reach;
                if (near <= 0f)
                    continue;
                if (blast) {
                    body.hurt(Hazards.source(server, Hazards.HOT_CONTACT), PotionTuning.BLAST_DAMAGE * (1 + amplifier) * near);
                    body.igniteForSeconds(PotionTuning.BLAST_BURN_SECONDS * near);
                } else
                    PotionEffects.apply(id, body, amplifier, Math.max(20, Math.round(state.duration() * PotionTuning.THROW_DURATION * near)));
            }
            if (blast)
                burst(server, blockPosition());
        }
        server.sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 12, 0.3, 0.2, 0.3, 0.1);
        server.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 0.8f, 1f);
        discard();
    }

    private static void burst(ServerLevel level, BlockPos at) {
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
    }
}
