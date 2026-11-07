// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.controller;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.registry.FTags;
import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Right click with a Punch Card to insert it, empty-handed to take it back -- the same shape as
 * the Firebox's fuel slot, for the same reason: one slot doesn't need a screen.
 * <p>
 * Couples on all six faces, like the gearbox: a switch box has no natural "front" for a shaft
 * and does not visibly turn, so there is nothing for a single facing to buy the player.
 */
public class ControllerBlock extends Block implements EntityBlock, Rotatable {

    public ControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(FTags.PUNCH_CARDS) || player.isShiftKeyDown())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof ControllerBlockEntity controller) || !controller.getItem(0).isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        Optional<String> refusal = controller.refusalFor(stack);
        if (refusal.isPresent()) {
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("feedback.controller.card_refused",
                        Component.translatable("feedback.programmer.error." + refusal.get())), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide)
            controller.setItem(0, stack.split(1));
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ControllerBlockEntity controller))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemStack taken = controller.removeItemNoUpdate(0);
            if (!taken.isEmpty() && !player.getInventory().add(taken))
                player.drop(taken, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ControllerBlockEntity controller) {
            Containers.dropContents(level, pos, controller);
            Fittable.dropAll(controller, level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Unread -- the box has no visible rotation, same as the gearbox. */
    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((ControllerBlockEntity) be).tickServer();
    }
}
