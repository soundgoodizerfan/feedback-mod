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
package io.github.soundgoodizerfan.feedback.machine.xp;

import io.github.soundgoodizerfan.feedback.core.xp.XpNode;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * XP wire. An arm shows towards every neighbour that is on the XP network; the arms are looks only, the run joins on
 * adjacency ({@code XpNetworks}). One block serves every material: it rides on the item as {@code MadeOf}.
 */
public class XpWireBlock extends Block implements EntityBlock {

    private static final BooleanProperty[] ARMS = {
            BooleanProperty.create("down"), BooleanProperty.create("up"), BooleanProperty.create("north"),
            BooleanProperty.create("south"), BooleanProperty.create("west"), BooleanProperty.create("east")};

    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape[] ARM_SHAPES = {
            Block.box(6, 0, 6, 10, 6, 10), Block.box(6, 10, 6, 10, 16, 10), Block.box(6, 6, 0, 10, 10, 6),
            Block.box(6, 6, 10, 10, 10, 16), Block.box(0, 6, 6, 6, 10, 10), Block.box(10, 6, 6, 16, 10, 10)};
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    public XpWireBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState();
        for (BooleanProperty arm : ARMS)
            state = state.setValue(arm, false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ARMS);
    }

    private static BooleanProperty arm(Direction d) {
        return ARMS[d.get3DDataValue()];
    }

    private BlockState armed(Level level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values())
            state = state.setValue(arm(d), level.getBlockEntity(pos.relative(d)) instanceof XpNode);
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return armed(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from,
                                   boolean moving) {
        BlockState next = armed(level, pos, state);
        if (next != state)
            level.setBlock(pos, next, 2);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int key = 0;
        for (Direction d : Direction.values())
            if (state.getValue(arm(d)))
                key |= 1 << d.get3DDataValue();
        VoxelShape shape = SHAPES[key];
        if (shape == null) {
            shape = CORE;
            for (Direction d : Direction.values())
                if (state.getValue(arm(d)))
                    shape = Shapes.or(shape, ARM_SHAPES[d.get3DDataValue()]);
            SHAPES[key] = shape;
        }
        return shape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new XpWireBlockEntity(pos, state);
    }
}
