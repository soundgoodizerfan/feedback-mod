// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.shaft;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A heavy rim on a shaft: a shaft with a great deal of {@code Inertia} and a little more bearing
 * drag for carrying it, and nothing else -- the Bearing's precedent, one class, its figures
 * stamped on it. What it does is the rotation network's, unwritten: a run with more inertia
 * changes speed more slowly, so it rides through a hammer's blows and a pulsing source, and it
 * coasts longer when the drive goes -- which is also why it is slower to stop (philosophy 10, a
 * mechanical stop coasts). Smoothing with mass, not a smarter controller (philosophy 13).
 */
public class FlywheelBlock extends ShaftBlock {

    private static final VoxelShape X = box(6, 0, 0, 10, 16, 16);
    private static final VoxelShape Y = box(0, 6, 0, 16, 10, 16);
    private static final VoxelShape Z = box(0, 0, 6, 16, 16, 10);

    public FlywheelBlock(Properties properties) {
        super(properties, FTuning.FLYWHEEL_DRAG_SU_PER_RPM, FTuning.FLYWHEEL_INERTIA);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> X;
            case Y -> Y;
            case Z -> Z;
        };
    }
}
