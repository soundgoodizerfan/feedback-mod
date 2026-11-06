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
package io.github.soundgoodizerfan.feedback.machine.redstone;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.control.Analog;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * The sided redstone fitting (author ruling, 2026-11-05): a small block set on one face of a machine that declares {@link Analog}
 * channels. Redstone power at the fitting sets the machine's first channel to {@code power / 15}: 15 is 100 per cent, 0 is 0, the rest
 * evenly between. It is a block on the face rather than an item in a holder, because only a few machines hold fittings and every
 * analog machine can carry a block. Redstone on a machine's own {@link io.github.soundgoodizerfan.feedback.control.Switchable}
 * still starts and stops it; this sets the level.
 */
public class AnalogAdapterBlock extends Block {

    /** The way the fitting looks: toward the machine it sets. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public AnalogAdapterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        apply(state, level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        apply(state, level, pos);
    }

    /** Sets the target's first channel to the redstone power here, over 15. */
    private static void apply(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide)
            return;
        if (level.getBlockEntity(pos.relative(state.getValue(FACING))) instanceof Analog target)
            target.setLevel(0, Math.min(15, level.getBestNeighborSignal(pos)) / 15f);
    }
}
