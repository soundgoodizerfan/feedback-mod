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
package io.github.soundgoodizerfan.feedback.machine.lathe;

import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Stock chucked and spun by the network, cut by a mounted tool bit. Sits directly on the network
 * like a Gearbox. Two slots, both item-typed rather than sharing one interaction: a Lathe Tool
 * Bit inserts or swaps the bit, anything else inserts or swaps the workpiece -- the same "which
 * physical component does this go into" question {@code CrankLinkageBlock#useItemOn} already asks
 * of a shaft in hand, not a sorting rule. Sneak with an empty hand swaps light/heavy pass.
 */
public class LatheBlock extends Block implements EntityBlock, Rotatable {

    public static final BooleanProperty HEAVY = BooleanProperty.create("heavy");

    public LatheBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HEAVY, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HEAVY);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || player.isShiftKeyDown())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof LatheBlockEntity lathe))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (!level.isClientSide) {
            if (stack.is(FItems.LATHE_TOOL_BIT.get())) {
                if (lathe.getToolBit().isEmpty())
                    lathe.insertToolBit(stack);
                else
                    give(player, lathe.removeToolBit());
            } else {
                if (lathe.getWorkpiece().isEmpty())
                    lathe.insertWorkpiece(stack);
                else
                    give(player, lathe.removeWorkpiece());
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && player.isShiftKeyDown())
            level.setBlockAndUpdate(pos, state.setValue(HEAVY, !state.getValue(HEAVY)));
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
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LatheBlockEntity lathe) {
            if (!lathe.getWorkpiece().isEmpty())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), lathe.getWorkpiece());
            if (!lathe.getToolBit().isEmpty())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), lathe.getToolBit());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

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
        return new LatheBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((LatheBlockEntity) be).tickServer();
    }
}
