// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.creative;

import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The creative rotation source. Without {@link Rotatable} the propagator never couples it to
 * anything, so it ran its own one-block network and drove nothing. A shaft stub on every face:
 * a test bench source should plug into whatever is beside it.
 */
public class CreativeRotationBlock extends CreativeSourceBlock implements Rotatable {

    public CreativeRotationBlock(Properties properties) {
        super(properties, CreativeRotationBlockEntity::new);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return true;
    }
}
