// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.lathe;

import io.github.soundgoodizerfan.feedback.registry.FTags;
import java.util.List;

import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import io.github.soundgoodizerfan.feedback.process.Operation;

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
 * Stock chucked and spun by the network, cut by a seated lathe bit. Sits directly on the network
 * like a Gearbox. Two slots, both item-typed rather than sharing one interaction: a lathe bit
 * is seated or swapped, anything else inserts or swaps the workpiece -- the same "which
 * physical component does this go into" question {@code CrankLinkageBlock#useItemOn} already asks
 * of a shaft in hand, not a sorting rule. Sneak with an empty hand swaps light/heavy pass.
 */
public class LatheBlock extends Block implements EntityBlock, Rotatable {

    public static final BooleanProperty HEAVY = BooleanProperty.create("heavy");

    /** Mirrors models/block/lathe.json (scripts/shop_models.py): base, lower bearing, chuck, column, rail, tail bearing, tail post, four shaft stubs. */
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(2, 0, 2, 14, 4, 14), Block.box(5, 4, 5, 11, 6, 11), Block.box(3, 6, 3, 13, 8, 13),
            Block.box(11, 8, 11, 14, 16, 14), Block.box(5, 12, 11, 14, 16, 14), Block.box(6, 10, 6, 10, 12, 10),
            Block.box(7, 10, 10, 9, 12, 12),
            Block.box(6, 1, 0, 10, 3, 2), Block.box(6, 1, 14, 10, 3, 16), Block.box(0, 1, 6, 2, 3, 10), Block.box(14, 1, 6, 16, 3, 10));

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return SHAPE;
    }

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

        if (lathe.tryHead(player, stack))
            return ItemInteractionResult.sidedSuccess(level.isClientSide);

        if (!level.isClientSide) {
            if (lathe.getWorkpiece().isEmpty())
                lathe.insert(stack);
            else
                give(player, lathe.removeWorkpiece());
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide) {
            if (player.isShiftKeyDown())
                level.setBlockAndUpdate(pos, state.setValue(HEAVY, !state.getValue(HEAVY)));
            else if (level.getBlockEntity(pos) instanceof LatheBlockEntity lathe) {
                if (!lathe.getWorkpiece().isEmpty())
                    give(player, lathe.removeWorkpiece());
                else
                    lathe.giveHead(player);
            }
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
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LatheBlockEntity lathe) {
            if (!lathe.getWorkpiece().isEmpty())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), lathe.getWorkpiece());
            if (!lathe.getHead().isEmpty())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), lathe.getHead().get());
            if (!lathe.getScrew().isEmpty())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), lathe.getScrew());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("feedback.tooltip.performs", Component.translatable(Operation.TURN.translationKey())));
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
