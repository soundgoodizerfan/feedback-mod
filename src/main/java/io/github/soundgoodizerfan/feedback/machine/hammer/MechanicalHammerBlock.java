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
package io.github.soundgoodizerfan.feedback.machine.hammer;

import io.github.soundgoodizerfan.feedback.net.HudReadoutPayload;
import io.github.soundgoodizerfan.feedback.modular.MachineHeadItem;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

/**
 * The machine that does not know when to stop.
 * <p>
 * Right click with something to put it in, empty-handed to take it back out. Taking it out in
 * time is the entire game here. A machine head right-clicked on seats as the head (the one it
 * replaces comes back to the hand); sneaking with an empty hand pulls the head.
 */
public class MechanicalHammerBlock extends Block implements EntityBlock {

    public MechanicalHammerBlock(Properties properties) {
        super(properties);
    }

    /**
     * Vanilla calls this even when the hand is empty, and only falls through to
     * {@link #useWithoutItem} when told to with PASS_TO_DEFAULT_BLOCK_INTERACTION. Returning
     * success for an empty hand therefore swallows the interaction -- which is exactly how
     * extraction came to be unreachable without breaking the block.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || player.isShiftKeyDown())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof MechanicalHammerBlockEntity hammer))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (stack.getItem() instanceof MachineHeadItem) {
            if (!level.isClientSide)
                give(player, hammer.seatHead(stack));
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // An occupied hammer gives its workpiece back whatever you are holding. Requiring an
        // empty hand reads as "there is no way to get it out" the first time you try it with a
        // stack of ingots in hand, which is the only thing you would plausibly be holding.
        if (!level.isClientSide) {
            if (hammer.getWorkpiece().isEmpty()) {
                hammer.insert(stack);
                if (hammer.getHead().isEmpty() && player instanceof ServerPlayer sp)
                    HudReadoutPayload.send(sp, Component.translatable("message.feedback.machine.no_head"), 60);
            } else
                give(player, hammer.removeWorkpiece());
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MechanicalHammerBlockEntity hammer))
            return InteractionResult.PASS;
        if (!level.isClientSide)
            give(player, player.isShiftKeyDown() ? hammer.removeHead() : hammer.removeWorkpiece());
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
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MechanicalHammerBlockEntity hammer) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), hammer.getWorkpiece());
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), hammer.getHead().get());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MechanicalHammerBlockEntity(pos, state);
    }
}
