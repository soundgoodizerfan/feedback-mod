// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.vice;

import java.util.List;

import io.github.soundgoodizerfan.feedback.item.HandTool;
import io.github.soundgoodizerfan.feedback.machine.ThreadScrew;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A bench vice. Right-click with a thread rod seats it as the jaw screw; right-click with a piece closes the jaws on it (needs a screw);
 * an empty hand takes the piece out, then the screw. A hand tool held while looking at the vice works the clamped piece
 * ({@link io.github.soundgoodizerfan.feedback.item.HandWork#begin}): the tool is in one hand and the other stays free.
 */
public class ViceBlock extends Block implements EntityBlock {

    /** Mirrors models/block/vice.json: base, fixed jaw, moving jaw, guide bar, screw and handle. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 3, 14), Block.box(4, 3, 3, 12, 9, 5), Block.box(4, 3, 8, 12, 9, 10),
            Block.box(7, 3, 5, 9, 5, 8), Block.box(7, 5, 10, 9, 7, 13.5), Block.box(4, 5.5, 13.5, 12, 6.5, 14.5));

    public ViceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof ViceBlockEntity vice))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        // A hand tool is for working the piece: the use of the tool itself does that, so the block does not act on it.
        if (stack.getItem() instanceof HandTool)
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (ThreadScrew.of(stack, level).isPresent()) {
            if (!level.isClientSide)
                give(player, vice.seatScrew(stack));
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide)
            vice.insert(stack);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ViceBlockEntity vice) {
            if (!vice.getWorkpiece().isEmpty())
                give(player, vice.removeWorkpiece());
            else
                give(player, vice.removeScrew());
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
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ViceBlockEntity vice) {
            if (!vice.getWorkpiece().isEmpty())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), vice.getWorkpiece());
            if (!vice.getScrew().isEmpty())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), vice.getScrew());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("feedback.tooltip.vice"));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ViceBlockEntity(pos, state);
    }
}
