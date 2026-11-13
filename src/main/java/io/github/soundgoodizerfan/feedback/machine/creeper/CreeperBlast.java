// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.creeper;

import io.github.soundgoodizerfan.feedback.machine.column.PetrochemTuning;
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

    /** Explosion power of {@code units} of charge. Under {@link PetrochemTuning#CREEPER_POWER_MIN} it only smokes. */
    public static float power(float units) {
        return Math.min(PetrochemTuning.CREEPER_POWER_MAX, PetrochemTuning.CREEPER_POWER_PER_SQRT * (float) Math.sqrt(Math.max(0f, units)));
    }

    /** Blow {@code units} of charge up at {@code at}; {@code charged} adds the electrical discharge. */
    public static void explode(ServerLevel level, Vec3 at, float units, boolean charged) {
        float power = power(units);
        if (power < PetrochemTuning.CREEPER_POWER_MIN) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.02);
            return;
        }
        level.explode(null, at.x, at.y, at.z, power, false, Level.ExplosionInteraction.MOB);
        if (charged) {
            double r = power * 2;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at.x - r, at.y - r, at.z - r, at.x + r, at.y + r, at.z + r)))
                Shock.apply(e, PetrochemTuning.CREEPER_DISCHARGE_BAND, PetrochemTuning.CREEPER_DISCHARGE_PACKETS
                        * io.github.soundgoodizerfan.feedback.core.electric.ElectricRules.packetEu(PetrochemTuning.CREEPER_DISCHARGE_BAND));
        }
    }

    public static void explode(ServerLevel level, BlockPos at, float units, boolean charged) {
        explode(level, Vec3.atCenterOf(at), units, charged);
    }
}
