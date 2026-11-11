// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.rotation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Implemented by any block that participates in a rotation network.
 * <p>
 * A rotatable block answers two questions and nothing else: which way does it spin, and
 * does it present a shaft to a given face. Everything about speed, load and who is driving
 * whom lives in {@link RotationPropagator} and {@link RotationNetwork}, never in the block.
 */
public interface Rotatable {

    /** The axis this block turns about, in its current state. */
    Direction.Axis getRotationAxis(BlockState state);

    /** Whether a shaft sticks out of this block toward {@code face}, so a neighbour can couple to it. */
    boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face);

    /**
     * Which way the shaft on {@code face} turns relative to this block's own rotation: +1 with it,
     * -1 against it.
     * <p>
     * Almost everything answers +1, because a shaft bolted through a block turns with it. A
     * gearbox does not: its faces are driven by separate bevels off a common crown, so the shafts
     * it couples are not all turning the same way.
     */
    default float shaftSignTowards(BlockState state, Direction face) {
        return 1;
    }

    /**
     * Gear ratio when this block meshes teeth with the neighbour across {@code face}, or 0 when
     * they do not mesh.
     * <p>
     * Meshing is not shaft coupling and the two must not be confused: coupled blocks share a
     * shaft and therefore one speed, while meshed teeth drive each other at a ratio and in
     * opposite directions. Returned as speed-of-neighbour over speed-of-self, negative because
     * meshing reverses.
     */
    default float meshRatioTowards(BlockState state, Direction face, BlockState other) {
        return 0;
    }

    /**
     * Relative tooth count of this block's teeth, or 0 for a block with none. Cogs and a ring gear
     * answer; meshing asks this rather than what block the neighbour is.
     */
    default int teeth(BlockState state) {
        return 0;
    }

    /**
     * Whether this block can mesh with a neighbour standing diagonally (an edge diagonal: one step
     * along two axes). The propagator scans those twelve positions only for a block that says yes,
     * so a shaft or a machine pays nothing for them.
     */
    default boolean meshesDiagonally(BlockState state) {
        return false;
    }

    /**
     * Gear ratio, neighbour's speed over this block's, when this block's teeth mesh with
     * {@code other} at the edge-diagonal {@code offset}, or 0 when they do not. Speeds are turns
     * about each block's positive axis direction; see {@code docs/specs/feedback_micro_spec.md}, <em>Cog
     * meshing</em>.
     */
    default float meshRatioAt(BlockState state, Vec3i offset, BlockState other) {
        return 0;
    }
}
