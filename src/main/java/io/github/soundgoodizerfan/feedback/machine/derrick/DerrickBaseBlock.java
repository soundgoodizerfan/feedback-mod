// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.derrick;

import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/**
 * The derrick's base: takes a shaft on its east and west faces, a bit by hand, and casing on top.
 * Hand empty reads the bore; sneaking with an empty hand pulls the bit back out.
 */
public class DerrickBaseBlock extends Block implements EntityBlock, Rotatable {

    public DerrickBaseBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.X;
    }

    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == Direction.Axis.X;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof DerrickBitItem && level.getBlockEntity(pos) instanceof DerrickBaseBlockEntity base) {
            if (!level.isClientSide)
                base.insertBit(stack);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                                   Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof DerrickBaseBlockEntity base) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    ItemStack taken = base.takeBit();
                    if (!taken.isEmpty())
                        player.getInventory().placeItemBackInInventory(taken);
                } else {
                    io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, pos, base.describe());
                }
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    /** A broken base gives the bit back (the bit is a part, not a fluid in the machine). */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DerrickBaseBlockEntity base) {
            ItemStack bit = base.takeBit();
            if (!bit.isEmpty())
                Block.popResource(level, pos, bit);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DerrickBaseBlockEntity(pos, state);
    }
}
