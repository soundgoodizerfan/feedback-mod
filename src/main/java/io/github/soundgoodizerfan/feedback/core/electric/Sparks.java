// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.electric;

import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * An electrical spark as a game event: an arc where a line was broken under load. Nothing
 * routes it; anything with a game-event listener within {@link ElectricTuning#SPARK_RANGE_BLOCKS} may
 * hear it -- the coherer and the cat's whisker, which is what makes them
 * wireless. Quiet arcs (under {@link ElectricTuning#SPARK_MIN_AMPS}) make none.
 */
public final class Sparks {

    private Sparks() {
    }

    /** Amps of the arc being delivered right now: set only while {@link #emit} dispatches, which is synchronous on the server thread. */
    private static float heard;

    public static void emit(Level level, BlockPos pos, float ampsBroken) {
        if (level.isClientSide || ampsBroken < ElectricTuning.SPARK_MIN_AMPS)
            return;
        heard = ampsBroken;
        try {
            level.gameEvent(null, FWeapons.SPARK, pos);
        } finally {
            heard = 0f;
        }
    }

    /** The amps of the arc a listener is hearing: how hard the spark was. Zero outside a delivery. */
    public static float heardAmps() {
        return heard;
    }
}
