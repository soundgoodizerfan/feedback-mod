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
package io.github.soundgoodizerfan.feedback.machine.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

/**
 * A pipe segment. An arm shows towards every neighbour it joins: another pipe, or anything that
 * exposes a fluid handler on the facing side. The arms are looks only -- the run joins on pipe
 * adjacency (see {@code FluidNetworks}) and pushes into any handler that touches it.
 * <p>
 * Using it with an empty hand reads the run's exact contents, the truth tier of the quantity
 * (digest: every quantity prints at the truth tier).
 */
public class PipeBlock extends Block implements EntityBlock {

    private static final BooleanProperty[] ARMS = {
            BooleanProperty.create("down"), BooleanProperty.create("up"), BooleanProperty.create("north"),
            BooleanProperty.create("south"), BooleanProperty.create("west"), BooleanProperty.create("east")};

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape[] ARM_SHAPES = {
            Block.box(5, 0, 5, 11, 5, 11), Block.box(5, 11, 5, 11, 16, 11), Block.box(5, 5, 0, 11, 11, 5),
            Block.box(5, 5, 11, 11, 11, 16), Block.box(0, 5, 5, 5, 11, 11), Block.box(11, 5, 5, 16, 11, 11)};
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    public PipeBlock(Properties properties) {
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
        for (Direction d : Direction.values()) {
            BlockPos beyond = pos.relative(d);
            boolean joins = level.getBlockState(beyond).getBlock() instanceof PipeBlock
                    || level.getCapability(Capabilities.FluidHandler.BLOCK, beyond, d.getOpposite()) != null;
            state = state.setValue(arm(d), joins);
        }
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PipeBlockEntity pipe && pipe.network != null) {
            FluidStack held = pipe.network.ledger.getFluid();
            player.displayClientMessage(held.isEmpty()
                    ? Component.translatable("message.feedback.pipe.empty", pipe.network.members.size(),
                            pipe.network.volume())
                    : Component.translatable("message.feedback.pipe.contents", held.getHoverName(), held.getAmount(),
                            pipe.network.volume(), pipe.network.ledger.temperature().value(), Math.round(pipe.network.pressure().value() * 10f) / 10f,
                            pipe.network.members.size()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }
}
