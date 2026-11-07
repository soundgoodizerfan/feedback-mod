// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.xp;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
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

/** The Crystal Holder: use with an XP container to seat it, with an empty hand to take it out. */
public class CrystalHolderBlock extends Block implements EntityBlock {

    public CrystalHolderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof CrystalHolderBlockEntity holder && XpItems.store(stack) != null) {
            if (!level.isClientSide && holder.seat(stack) && !player.getAbilities().instabuild)
                stack.shrink(1);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof CrystalHolderBlockEntity holder && !holder.seated().isEmpty()) {
            if (!level.isClientSide)
                player.getInventory().placeItemBackInInventory(holder.take());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof CrystalHolderBlockEntity holder && !holder.seated().isEmpty())
            Block.popResource(level, pos, holder.take());
        super.onRemove(state, level, pos, next, moving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrystalHolderBlockEntity(pos, state);
    }
}
