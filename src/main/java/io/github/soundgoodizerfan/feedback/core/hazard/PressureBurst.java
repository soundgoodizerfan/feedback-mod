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
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * What a sealed vessel does when it holds more than it was built to, shared by everything that
 * holds a pressure: the air tank and the boiler.
 *
 * <h2>A ladder, not a cliff</h2>
 * Philosophy 6's top rung is a hazard, and GregTech's boiler is the precedent it names. Below the
 * danger line nothing happens however long it sits there. Between danger and critical the chance
 * of letting go climbs linearly each tick, so a vessel near the line survives a while and a
 * vessel past it does not; at critical it is certain. The shape is read from PneumaticCraft:
 * Repressurized's air handler (GPL-3.0, see THIRD-PARTY-LICENSES.md).
 *
 * <p>This is the one place a machine is allowed to be destroyed outright, and it is reached only
 * by a pressure the player let build. Scalding steam is the warning that comes first -- see
 * {@link #scald}.
 */
public final class PressureBurst {

    private PressureBurst() {
    }

    /** Whether a vessel at this pressure lets go this tick. */
    public static boolean fails(Pu pressure, Pu danger, Pu critical, RandomSource random) {
        float p = pressure.value();
        if (p <= danger.value())
            return false;
        if (p >= critical.value())
            return true;
        float range = critical.value() - danger.value();
        float chance = range <= 0 ? 1f : (p - danger.value()) / range;
        return random.nextFloat() < chance;
    }

    /**
     * Scald everything alive within {@code reach} blocks of {@code pos}, once. Steam gets in under
     * a glove, so no protection is asked about -- only fire resistance answers, through the
     * damage type's tag.
     */
    public static void scald(Level level, BlockPos pos, float reach, float damage) {
        if (level.isClientSide)
            return;
        AABB area = new AABB(pos).inflate(reach);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area))
            victim.hurt(Hazards.source(level, Hazards.SCALD), damage);
    }

    /** The hiss and the cloud of a seam letting go, so a weeping boiler is heard before it is felt. */
    public static void hiss(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    6, 0.25, 0.2, 0.25, 0.02);
            server.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 1.6f);
        }
    }

    /** A burst: the blast, and the steam that goes with it reaching further than a seam's. */
    public static void burst(Level level, BlockPos pos, float power) {
        scald(level, pos, FTuning.STEAM_BURST_REACH, FTuning.STEAM_BURST_DAMAGE);
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power,
                Level.ExplosionInteraction.BLOCK);
    }
}
