// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.belt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** A block that is one cell of a belt run: a belt block or a head roller. Both face along the run and may climb or fall one block. */
public interface BeltPart {

    Direction beltFacing(BlockState state);

    BeltBlock.Slope beltSlope(BlockState state);

    /** The cell cargo leaves this one into: ahead, one higher on a rise, one lower on a fall. */
    default BlockPos next(BlockPos pos, BlockState state) {
        BlockPos ahead = pos.relative(beltFacing(state));
        return switch (beltSlope(state)) {
            case UP -> ahead.above();
            case DOWN -> ahead.below();
            case FLAT -> ahead;
        };
    }

    static BeltPart of(BlockState state) {
        return state.getBlock() instanceof BeltPart part ? part : null;
    }
}
