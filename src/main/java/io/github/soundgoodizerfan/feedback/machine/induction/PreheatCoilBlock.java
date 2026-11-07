// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.induction;

import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/** The forging preheat coil: right click with a piece to lay it in the field, empty-handed to take it back. An empty hand on an empty coil, or sneaking, steps its target temperature. */
public class PreheatCoilBlock extends Block implements EntityBlock {

    public PreheatCoilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PreheatCoilBlockEntity coil) || !coil.held().isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            coil.lay(player.hasInfiniteMaterials() ? stack.copyWithCount(1) : stack.split(1));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PreheatCoilBlockEntity coil))
            return InteractionResult.PASS;
        // An empty hand on an empty coil, or a sneaking one, steps the target; otherwise it takes the piece back.
        if (coil.held().isEmpty() || player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                coil.cycleTarget();
                player.displayClientMessage(coil.targetTu() <= 0f ? Component.translatable("message.feedback.preheat_off")
                        : Component.translatable("message.feedback.preheat_target", Math.round(coil.targetTu())), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            ItemStack taken = coil.take();
            if (!player.getInventory().add(taken))
                player.drop(taken, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PreheatCoilBlockEntity coil && !coil.held().isEmpty())
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), coil.held());
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PreheatCoilBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != FBlockEntities.PREHEAT_COIL.get() ? null : (l, p, s, be) -> ((PreheatCoilBlockEntity) be).tickServer();
    }
}
