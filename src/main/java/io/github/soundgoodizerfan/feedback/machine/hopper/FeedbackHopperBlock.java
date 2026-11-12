// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.hopper;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The hopper, with no inventory: it holds nothing. Every {@link #PERIOD} ticks it takes one item from the container above (or an item lying
 * on it) and puts it straight into the container it faces, in one step, and only if that container takes it; otherwise nothing moves. So a
 * hopper is a short conveyor between two blocks, never a buffer, and a chain of hoppers cannot sort, store or hold a queue (a
 * vanilla hopper does all three). A redstone signal switches it off, as vanilla's does. Vanilla's hopper is swapped for it
 * ({@code VanillaVesselSwap}); its facing carries over. Stateless: a scheduled tick, no block entity.
 */
public class FeedbackHopperBlock extends Block {

    public static final DirectionProperty FACING = HopperBlock.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    /** Ticks between moves: vanilla's cooldown. */
    public static final int PERIOD = 8;

    private static final VoxelShape BOWL = Block.box(0, 10, 0, 16, 16, 16), NECK = Block.box(4, 4, 4, 12, 10, 12);

    public FeedbackHopperBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(ENABLED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction side = context.getClickedFace().getOpposite();
        return defaultBlockState().setValue(FACING, side == Direction.UP ? Direction.DOWN : side)
                .setValue(ENABLED, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction f = state.getValue(FACING);
        VoxelShape spout = switch (f) {
            case DOWN -> Block.box(6, 0, 6, 10, 4, 10);
            case NORTH -> Block.box(6, 4, 0, 10, 8, 4);
            case SOUTH -> Block.box(6, 4, 12, 10, 8, 16);
            case WEST -> Block.box(0, 4, 6, 4, 8, 10);
            default -> Block.box(12, 4, 6, 16, 8, 10);
        };
        return Shapes.or(BOWL, NECK, spout);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!old.is(this) && !level.isClientSide)
            level.scheduleTick(pos, this, PERIOD);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        boolean on = !level.hasNeighborSignal(pos);
        if (on != state.getValue(ENABLED))
            level.setBlock(pos, state.setValue(ENABLED, on), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(ENABLED))
            move(level, pos, state.getValue(FACING));
        level.scheduleTick(pos, this, PERIOD);
    }

    /** One item from above to the block faced: a simulated insert first, so nothing is taken that cannot go. */
    private static void move(ServerLevel level, BlockPos pos, Direction facing) {
        BlockPos to = pos.relative(facing);
        IItemHandler dest = level.getCapability(Capabilities.ItemHandler.BLOCK, to, facing.getOpposite());
        if (dest == null)
            return;
        IItemHandler from = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.above(), Direction.DOWN);
        if (from != null) {
            for (int slot = 0; slot < from.getSlots(); slot++) {
                ItemStack probe = from.extractItem(slot, 1, true);
                if (probe.isEmpty() || !ItemHandlerHelper.insertItem(dest, probe, true).isEmpty())
                    continue;
                ItemStack taken = from.extractItem(slot, 1, false);
                if (!taken.isEmpty()) {
                    ItemStack left = ItemHandlerHelper.insertItem(dest, taken, false);
                    if (!left.isEmpty())
                        ItemHandlerHelper.insertItem(from, left, false);
                    return;
                }
            }
            return;
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos.above()).inflate(0, 0.0, 0), e -> e.isAlive() && !e.getItem().isEmpty())) {
            ItemStack one = item.getItem().copyWithCount(1);
            if (!ItemHandlerHelper.insertItem(dest, one, true).isEmpty())
                continue;
            ItemHandlerHelper.insertItem(dest, one, false);
            item.getItem().shrink(1);
            if (item.getItem().isEmpty())
                item.discard();
            return;
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }
}
