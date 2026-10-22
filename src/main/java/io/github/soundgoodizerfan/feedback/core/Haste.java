/*
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.soundgoodizerfan.feedback.core;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * How fast a body works: vanilla's Haste and Mining Fatigue read as one rate, and every timer the
 * mod owns (a gun's cycle and reload, a shield bash, a hoe's recovery) is divided by it here, so
 * none of them reads the effect itself. Vanilla's own attack cooldown already follows these
 * effects through the attack-speed attribute, so melee swing cadence and hand-tool swings need
 * nothing of ours. Both sides read it: effects are synced to the client, which predicts the same cycle.
 */
public final class Haste {

    private Haste() {
    }

    /** Work rate: 1 with no effect, {@link FTuning#HASTE_RATE_PER_LEVEL} more per Haste level, as much less per Fatigue level. */
    public static float rate(LivingEntity entity) {
        var haste = entity.getEffect(MobEffects.DIG_SPEED);
        var fatigue = entity.getEffect(MobEffects.DIG_SLOWDOWN);
        int levels = (haste == null ? 0 : haste.getAmplifier() + 1) - (fatigue == null ? 0 : fatigue.getAmplifier() + 1);
        return Math.max(FTuning.HASTE_RATE_FLOOR, 1f + FTuning.HASTE_RATE_PER_LEVEL * levels);
    }

    /** {@code time} (ms or ticks) as it takes {@code entity}. */
    public static long time(long time, LivingEntity entity) {
        return Math.round(time / rate(entity));
    }
}
