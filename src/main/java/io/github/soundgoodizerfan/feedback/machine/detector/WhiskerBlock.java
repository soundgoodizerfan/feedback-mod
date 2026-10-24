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
package io.github.soundgoodizerfan.feedback.machine.detector;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

/**
 * A cat's-whisker detector. Right-click with a rectifying crystal seats it; empty hand steps the
 * whisker to the next spot and reports a noisy band; sneak with an empty hand takes the crystal out,
 * then the circuit. A Basic Electronic Circuit right-clicked on it is fitted on the contact.
 */
public class WhiskerBlock extends Block implements EntityBlock {

    public WhiskerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.has(FDataComponents.CIRCUIT.get()) && level.getBlockEntity(pos) instanceof WhiskerBlockEntity stage && !stage.hasStage()) {
            if (!level.isClientSide) {
                stage.fit(stack);
                if (!player.getAbilities().instabuild)
                    stack.shrink(1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof WhiskerBlockEntity whisker)
                || whisker.hasCrystal() || WhiskerBlockEntity.rectifying(stack) <= 0f)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            whisker.seat(stack);
            if (!player.getAbilities().instabuild)
                stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof WhiskerBlockEntity whisker))
            return InteractionResult.sidedSuccess(level.isClientSide);
        if (player.isShiftKeyDown() && (whisker.hasCrystal() || whisker.hasStage())) {
            ItemStack out = whisker.hasCrystal() ? whisker.unseat() : whisker.unfit();
            if (!player.getInventory().add(out))
                player.drop(out, false);
        } else if (!whisker.hasCrystal()) {
            player.displayClientMessage(Component.translatable("feedback.whisker.no_crystal"), true);
        } else {
            whisker.step();
            // Scalars are fine inside a minigame: the band is the true figure plus a little noise, so a spot is judged by sampling it again.
            float heard = whisker.sensitivity() + (level.random.nextFloat() - 0.5f) * whisker.bandSpread();
            String band = whisker.spotFused() ? "burnt" : heard < 0.1f ? "dead" : heard < 0.4f ? "faint" : heard < 0.7f ? "clear" : "sharp";
            player.displayClientMessage(Component.translatable("feedback.whisker." + band), true);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WhiskerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((WhiskerBlockEntity) be).tickServer();
    }
}
