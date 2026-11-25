// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.gearcutter;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A toothed cutting wheel over a plate, sitting directly on the network like the Drill Press. Right click with a
 * plate to put it on the table, or, on an occupied table, to take the piece back out. Empty-handed: a plain click
 * feeds one cut; a sneaking click opens the tooth picker (the screen lists the tooth counts and one click sets
 * the next cut).
 */
public class GearCutterBlock extends Block implements EntityBlock, Rotatable {

    /** Mirrors models/block/gear_cutter.json: base, upright, arm, arbor, wheel, four shaft stubs. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 4, 14), Block.box(6, 4, 12, 10, 15, 14), Block.box(6, 10, 6, 10, 13, 12),
            Block.box(7, 6, 7, 9, 10, 9), Block.box(4, 6, 4, 12, 7, 12),
            Block.box(6, 1, 0, 10, 3, 2), Block.box(6, 1, 14, 10, 3, 16), Block.box(0, 1, 6, 2, 3, 10), Block.box(14, 1, 6, 16, 3, 10));

    public GearCutterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || player.isShiftKeyDown())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof GearCutterBlockEntity cutter))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            if (cutter.getWorkpiece().isEmpty())
                cutter.insert(stack);
            else
                give(player, cutter.removeWorkpiece());
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof GearCutterBlockEntity cutter))
            return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            if (level.isClientSide)
                io.github.soundgoodizerfan.feedback.client.screen.GearCutterScreen.open(pos, cutter.teeth());
        } else if (!level.isClientSide) {
            if (cutter.getWorkpiece().isEmpty())
                return InteractionResult.PASS;
            cutter.push();
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
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GearCutterBlockEntity cutter) {
            ItemStack held = cutter.getWorkpiece();
            if (!held.isEmpty())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), held);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("feedback.tooltip.gear_cutter"));
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
        return new GearCutterBlockEntity(pos, state);
    }
}
