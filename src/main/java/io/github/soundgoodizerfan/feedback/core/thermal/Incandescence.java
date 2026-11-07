// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.thermal;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A body hot enough to glow gives off light ({@code docs/spec_incandescence.md}). The bands are the free-sense bands
 * ({@link FTuning#HEAT_BAND_TOPS}); light starts at the third (faint red) and each band gives a real block light level, so
 * the game treats the glow as real: vanilla melts ice and snow and keeps hostile mobs from spawning in it. A block that
 * glows carries {@link #GLOW}, a block state property its light emission reads, and moves it only when the band changes.
 * What glows is read from the material: a metal does, and any other material opts in with the incandescent trait.
 */
public final class Incandescence {

    /** The light level a glowing block gives, 0 to 15: the block's {@code lightEmission} reads it. */
    public static final IntegerProperty GLOW = IntegerProperty.create("glow", 0, 15);

    private Incandescence() {
    }

    /** The free-sense band {@code tu} falls in, 0 for the coldest. */
    public static int band(float tu) {
        for (int band = 0; band < FTuning.HEAT_BAND_TOPS.size(); band++)
            if (tu < FTuning.HEAT_BAND_TOPS.get(band).value())
                return band;
        return FTuning.HEAT_BAND_TOPS.size() - 1;
    }

    /** The block light level a body at {@code tu} gives, 0 below the faint red band. */
    public static int light(float tu) {
        return FTuning.INCANDESCENCE_LIGHT[Math.min(band(tu), FTuning.INCANDESCENCE_LIGHT.length - 1)];
    }

    /** Whether a material gives off light when hot: any metal, and anything that opts in. */
    public static boolean glows(Material material) {
        return material.kind().orElse(null) == Material.Kind.METAL || material.traits().contains(Material.Trait.INCANDESCENT);
    }

    /** Set the block's glow to {@code level}, if it has the property and it differs: a lighting update only when a band changes. */
    public static void apply(Level level, BlockPos pos, int level_) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(GLOW) && state.getValue(GLOW) != level_)
            level.setBlock(pos, state.setValue(GLOW, level_), 3);
    }
}
