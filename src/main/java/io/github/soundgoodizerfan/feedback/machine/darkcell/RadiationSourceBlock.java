// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.darkcell;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.hazard.Radiation;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

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

/** A radioactive source set in a cell's wall: any item that gives off radiation, loaded by hand. */
public class RadiationSourceBlock extends Block implements EntityBlock, KillMethodBlock {

    public RadiationSourceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.isEmpty() && level.getBlockEntity(pos) instanceof RadiationSourceBlockEntity be && be.source().isEmpty()
                && Radiation.activity(stack, MaterialTable.get().materials()) > 0f) {
            if (!level.isClientSide) {
                be.load(stack.copyWithCount(1));
                if (!player.getAbilities().instabuild)
                    stack.shrink(1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof RadiationSourceBlockEntity be) {
            ItemStack out = be.unload();
            if (!out.isEmpty() && !player.getInventory().add(out))
                player.drop(out, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RadiationSourceBlockEntity be)
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.unload());
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadiationSourceBlockEntity(pos, state);
    }

    @Override
    public KillMethod method(Level level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof RadiationSourceBlockEntity be ? be : null;
    }
}
