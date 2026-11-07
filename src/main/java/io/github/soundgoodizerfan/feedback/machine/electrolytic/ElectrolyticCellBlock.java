// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.electrolytic;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * A tank of water with an anode and a cathode in it. Right click with a bucket or pipe in water;
 * right click with metal to hang it as the anode; an empty hand strips the cathode, and sneaking
 * with an empty hand takes the anode back.
 */
public class ElectrolyticCellBlock extends Block implements EntityBlock {

    public ElectrolyticCellBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectrolyticCellBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ElectrolyticCellBlockEntity cell))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        boolean side = hit.getDirection().getAxis().isHorizontal();
        if (!(side ? ElectrolyticCellBlockEntity.hangable(stack) : ElectrolyticCellBlockEntity.isAnode(stack)))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        // The top takes the anode, a side hangs a piece as the cathode to be plated.
        if (!level.isClientSide) {
            int before = stack.getCount();
            if (side ? cell.hangWork(stack) : cell.insertAnode(stack).getCount() < before)
                player.displayClientMessage(Component.translatable(side
                        ? "feedback.electrolytic_cell.cathode" : "feedback.electrolytic_cell.anode"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ElectrolyticCellBlockEntity cell))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (player.isShiftKeyDown())
                give(player, cell.removeAnode());
            else if (!cell.getWork().isEmpty())
                cell.takeWork().forEach(stack -> give(player, stack));
            else
                cell.takeCathode().forEach(stack -> give(player, stack));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void give(Player player, ItemStack stack) {
        if (stack.isEmpty())
            return;
        if (!player.getInventory().add(stack))
            player.drop(stack, false);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ElectrolyticCellBlockEntity cell) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), cell.removeAnode());
            for (ItemStack stack : cell.takeWork())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            for (ItemStack stack : cell.takeCathode())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
