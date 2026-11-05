/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
