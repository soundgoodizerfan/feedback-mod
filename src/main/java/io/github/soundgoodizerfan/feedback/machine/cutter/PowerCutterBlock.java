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
package io.github.soundgoodizerfan.feedback.machine.cutter;

import io.github.soundgoodizerfan.feedback.item.DataStickItem;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
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

/**
 * The Power Cutter. Right click with a cutter head or a data stick to seat it (the one it replaces comes back); empty-handed takes the
 * stick back, sneaking takes the head. Any other item is fed in by hand like a hopper would, and the output is taken with an empty
 * hand once the stick and head are out.
 */
public class PowerCutterBlock extends Block implements EntityBlock {

    public PowerCutterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PowerCutterBlockEntity cutter))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        boolean head = stack.getItem() instanceof CutterHeadItem, stick = stack.getItem() instanceof DataStickItem;
        if (!level.isClientSide) {
            if (head)
                give(player, cutter.seatHead(stack));
            else if (stick)
                give(player, cutter.seatStick(stack));
            else if (!stack.isEmpty()) {
                ItemStack left = cutter.insertInput(stack.copy(), false);
                if (!player.hasInfiniteMaterials())
                    stack.setCount(left.getCount());
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PowerCutterBlockEntity cutter))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemStack out = cutter.takeOutput(64, false);
            give(player, !out.isEmpty() ? out : player.isShiftKeyDown() ? cutter.removeHead() : cutter.removeStick());
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
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PowerCutterBlockEntity cutter) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), cutter.getHead().get());
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), cutter.getStick());
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), cutter.getInput());
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), cutter.getOutput());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PowerCutterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != FBlockEntities.POWER_CUTTER.get() ? null : (l, p, s, be) -> ((PowerCutterBlockEntity) be).tickServer();
    }
}
