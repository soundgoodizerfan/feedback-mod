// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.logistics.belt.BeltModuleItem;
import io.github.soundgoodizerfan.feedback.registry.FLogistics;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** The dock at a warehouse aisle's end. {@link #FACING} is its front, away from the rail; using it opens the stock list. */
public class DockBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public DockBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** A reader module used on a free side or the top fits it there as a head. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof BeltModuleItem item) || item.module().dockProperty() == null
                || !(level.getBlockEntity(pos) instanceof DockBlockEntity dock) || !dock.mounts(hit.getDirection())
                || dock.heads().containsKey(hit.getDirection()))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            dock.fit(hit.getDirection(), item.module());
            stack.consume(1, player);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1f, 0.8f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        // On a head's face an empty hand wipes it if fouled, or sneaking takes it off.
        if (level.getBlockEntity(pos) instanceof DockBlockEntity d) {
            Head head = d.tend(hit.getDirection(), player.isShiftKeyDown());
            if (head != null) {
                if (player.isShiftKeyDown()) {
                    ItemStack out = new ItemStack(FLogistics.MODULE_ITEMS.get(head.module).get());
                    if (!player.getInventory().add(out))
                        player.drop(out, false);
                }
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1f, 1f);
                return InteractionResult.CONSUME;
            }
        }
        // Sneaking with an empty hand takes the waiting intake back.
        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof DockBlockEntity dock && dock.returnIntake(player))
            return InteractionResult.CONSUME;
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof DockBlockEntity dock)
            sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new DockMenu(id, inv, pos),
                    Component.translatable(getDescriptionId())), pos);
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DockBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((DockBlockEntity) be).tickServer();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        AisleChanges.bump();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!newState.is(this) && level.getBlockEntity(pos) instanceof DockBlockEntity dock) {
            dock.spill();
            for (Head head : dock.heads().values())
                popResource(level, pos, new ItemStack(FLogistics.MODULE_ITEMS.get(head.module).get()));
        }
        super.onRemove(state, level, pos, newState, moved);
        AisleChanges.bump();
    }
}
