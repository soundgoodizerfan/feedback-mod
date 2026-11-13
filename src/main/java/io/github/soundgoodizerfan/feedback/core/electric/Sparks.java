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

    public static void emit(Level level, BlockPos pos, float ampsBroken) {
        if (!level.isClientSide && ampsBroken >= ElectricTuning.SPARK_MIN_AMPS)
            level.gameEvent(null, FWeapons.SPARK, pos);
    }
}
