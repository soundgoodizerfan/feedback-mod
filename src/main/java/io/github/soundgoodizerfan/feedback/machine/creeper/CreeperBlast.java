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
package io.github.soundgoodizerfan.feedback.machine.creeper;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Shock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What a creeper's reserve does when it goes up: one rule for a pen, a sac and a cut charge, so the figures cannot drift.
 * Power is the square root of the charge units (twice the charge is not twice the blast), capped; a charged creeper's
 * blast also discharges into everything inside it.
 */
public final class CreeperBlast {

    private CreeperBlast() {
    }

    /** Explosion power of {@code units} of charge. Under {@link FTuning#CREEPER_POWER_MIN} it only smokes. */
    public static float power(float units) {
        return Math.min(FTuning.CREEPER_POWER_MAX, FTuning.CREEPER_POWER_PER_SQRT * (float) Math.sqrt(Math.max(0f, units)));
    }

    /** Blow {@code units} of charge up at {@code at}; {@code charged} adds the electrical discharge. */
    public static void explode(ServerLevel level, Vec3 at, float units, boolean charged) {
        float power = power(units);
        if (power < FTuning.CREEPER_POWER_MIN) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.02);
            return;
        }
        level.explode(null, at.x, at.y, at.z, power, false, Level.ExplosionInteraction.BLOCK);
        if (charged) {
            double r = power * 2;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at.x - r, at.y - r, at.z - r, at.x + r, at.y + r, at.z + r)))
                Shock.apply(e, FTuning.CREEPER_DISCHARGE_VOLTS);
        }
    }

    public static void explode(ServerLevel level, BlockPos at, float units, boolean charged) {
        explode(level, Vec3.atCenterOf(at), units, charged);
    }
}
