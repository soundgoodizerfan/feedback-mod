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

    @Override
    public int teeth(BlockState state) {
        return CogBlock.LARGE;
    }

    @Override
    public float meshRatioTowards(BlockState state, Direction face, BlockState other) {
        return CogBlock.faceMesh(CogBlock.LARGE, getRotationAxis(state), face, other);
    }

    @Override
    public float meshRatioAt(BlockState state, Vec3i offset, BlockState other) {
        return CogBlock.diagonalMesh(CogBlock.LARGE, getRotationAxis(state), offset, other);
    }
}
