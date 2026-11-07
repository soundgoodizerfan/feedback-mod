// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.electrolyser;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * Splits a liquid under current. The front face ({@link #FACING}) gives what collects at the anode, the back what
 * collects at the cathode, and the liquid goes in at any other face, by bucket or pipe; a solute the liquid dissolves
 * goes in by hand or hopper.
 */
public class ElectrolyserBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public ElectrolyserBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        if (!stack.isEmpty() && level.getBlockEntity(pos) instanceof ElectrolyserBlockEntity cell
                && cell.solute().isItemValid(0, stack)) {
            if (!level.isClientSide) {
                ItemStack left = cell.solute().insertItem(0, stack.copy(), false);
                if (!player.getAbilities().instabuild)
                    player.setItemInHand(hand, left);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** An empty hand strips the cathode: whatever metal has plated out on it. */
    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                                  BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ElectrolyserBlockEntity cell && !cell.plate().getStackInSlot(0).isEmpty()) {
            if (!level.isClientSide)
                player.getInventory().placeItemBackInInventory(cell.plate().extractItem(0, 64, false));
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ElectrolyserBlockEntity cell)
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                    cell.solute().getStackInSlot(0));
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ElectrolyserBlockEntity cell)
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                    cell.plate().getStackInSlot(0));
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectrolyserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((ElectrolyserBlockEntity) be).tickServer();
    }
}
