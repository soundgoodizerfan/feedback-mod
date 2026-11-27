// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.radio;

import io.github.soundgoodizerfan.feedback.item.RadioSetItem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/** The loudspeaker block. Use it with a radio set to take the set's band; use it bare to step the band. See {@link LoudspeakerBlockEntity}. */
public class LoudspeakerBlock extends Block implements EntityBlock {

    public LoudspeakerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LoudspeakerBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof RadioSetItem) || !(level.getBlockEntity(pos) instanceof LoudspeakerBlockEntity speaker))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            speaker.setBand(RadioSets.band(stack));
            player.displayClientMessage(Component.translatable("message.feedback.loudspeaker.band", speaker.band()), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LoudspeakerBlockEntity speaker))
            return net.minecraft.world.InteractionResult.PASS;
        if (!level.isClientSide) {
            speaker.setBand(speaker.band() % RadioTuning.BANDS + 1);
            player.displayClientMessage(Component.translatable("message.feedback.loudspeaker.band", speaker.band()), true);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
}
