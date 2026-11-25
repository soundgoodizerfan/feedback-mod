// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.bearing;

import io.github.soundgoodizerfan.feedback.machine.cog.CogBlock;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A ring gear with a large cog's teeth: twice a small ring's, so a small cog driving it turns the body at
 * half the speed, and a large cog beside it at the same speed. Everything else is {@link RingGearBlock}'s.
 */
public class LargeRingGearBlock extends RingGearBlock {

    public LargeRingGearBlock(Properties properties) {
        super(properties);
    }

    /** Twice a small ring's teeth. */
    public static final int LARGE_RING_TEETH = 2 * RING_TEETH;

    @Override
    public int teeth(BlockState state) {
        return LARGE_RING_TEETH;
    }

    @Override
    public float meshRatioTowards(BlockState state, Direction face, BlockState other) {
        return CogBlock.faceMesh(teeth(state), getRotationAxis(state), face, other);
    }

    @Override
    public float meshRatioAt(BlockState state, Vec3i offset, BlockState other) {
        return CogBlock.diagonalMesh(teeth(state), getRotationAxis(state), offset, other);
    }
}
