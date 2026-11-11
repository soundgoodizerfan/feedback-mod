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
package io.github.soundgoodizerfan.feedback.machine.bearing;

import io.github.soundgoodizerfan.feedback.machine.cog.CogBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A hollow gear on a block's face ({@code docs/feedback_micro_spec.md}, <em>Bearings and ring
 * gears</em>): a {@link RotaryBearingBlock} whose input is its rim, not its back. The back couples
 * to nothing -- a shaft or gearbox face behind turns inside the ring -- and the rim meshes with cogs
 * by the cog rules as a small cog would, so the assembled body is driven from the side by a cog.
 */
public class RingGearBlock extends RotaryBearingBlock {

    public RingGearBlock(Properties properties) {
        super(properties);
    }

    /** Hollow: nothing behind or in front drives it through its centre. */
    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return false;
    }

    /** A small cog's teeth, at its block's edge. shortcut: one ring size; a large ring is [OPEN] in the spec. */
    @Override
    public int teeth(BlockState state) {
        return CogBlock.SMALL;
    }

    @Override
    public float meshRatioTowards(BlockState state, Direction face, BlockState other) {
        return CogBlock.faceMesh(CogBlock.SMALL, getRotationAxis(state), face, other);
    }

    @Override
    public boolean meshesDiagonally(BlockState state) {
        return true;
    }

    @Override
    public float meshRatioAt(BlockState state, Vec3i offset, BlockState other) {
        return CogBlock.diagonalMesh(CogBlock.SMALL, getRotationAxis(state), offset, other);
    }
}
