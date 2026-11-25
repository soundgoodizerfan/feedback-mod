// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A pallet rack: open shelves two bays across, two up and two deep, reached from the front and the back. A fitting item
 * used on an empty bay installs it; a sneaking empty hand takes it out again; an empty hand reads the bay.
 */
public class RackBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public RackBlock(Properties properties) {
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

    /**
     * The bay a hit points at, or -1 for a hit on any face but the front or back. Bays count across (0 to 1, left to
     * right seen from the front), then up (+2), then back half (+4).
     */
    public static int bayAt(BlockState state, BlockHitResult hit) {
        Direction facing = state.getValue(FACING);
        if (hit.getDirection().getAxis() != facing.getAxis())
            return -1;
        Vec3 at = hit.getLocation().subtract(Vec3.atCenterOf(hit.getBlockPos()));
        Direction right = facing.getCounterClockWise();
        double across = at.x * right.getStepX() + at.z * right.getStepZ();
        return (across > 0 ? 1 : 0) + (at.y > 0 ? 2 : 0) + (hit.getDirection() == facing ? 0 : 4);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof FittingItem fitting))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int bay = bayAt(state, hit);
        if (bay < 0 || !(level.getBlockEntity(pos) instanceof RackBlockEntity rack))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        Bay b = rack.bay(bay);
        if (!b.empty() || b.fitting != Fitting.PALLET) {
            player.displayClientMessage(Component.translatable(b.empty() ? "message.feedback.rack.fitted" : "message.feedback.rack.not_empty"), true);
            return ItemInteractionResult.CONSUME;
        }
        b.fitting = fitting.fitting();
        stack.consume(1, player);
        rack.changed();
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1f, 0.8f);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int bay = bayAt(state, hit);
        if (bay < 0 || !(level.getBlockEntity(pos) instanceof RackBlockEntity rack))
            return InteractionResult.PASS;
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        Bay b = rack.bay(bay);
        if (player.isShiftKeyDown()) {
            if (b.fitting == Fitting.PALLET)
                return InteractionResult.CONSUME;
            if (!b.empty()) {
                player.displayClientMessage(Component.translatable("message.feedback.rack.not_empty"), true);
                return InteractionResult.CONSUME;
            }
            ItemStack out = new ItemStack(FittingItem.of(b.fitting));
            b.fitting = Fitting.PALLET;
            if (!player.getInventory().add(out))
                player.drop(out, false);
            rack.changed();
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1f, 0.8f);
            return InteractionResult.CONSUME;
        }
        io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, pos, describe(b));
        return InteractionResult.CONSUME;
    }

    private static Component describe(Bay b) {
        Component fitting = Component.translatable("fitting.feedback.rack." + b.fitting.id());
        if (b.empty())
            return Component.translatable("message.feedback.rack.empty", fitting);
        MutableComponent held = Component.empty();
        for (int i = 0; i < b.lots.size(); i++) {
            Lot l = b.lots.get(i);
            if (i > 0)
                held.append(", ");
            held.append(Component.translatable("message.feedback.rack.lot", l.count, l.item.getHoverName()));
        }
        return Component.translatable("message.feedback.rack.holds", fitting, held);
    }

    /** A creative break of a rack with anything on it still drops the item, so nothing stored is lost. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof RackBlockEntity rack) {
            ItemStack drop = new ItemStack(this);
            drop.applyComponents(rack.collectComponents());
            if (drop.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.RACK_STOCK.get()))
                popResource(level, pos, drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** A rack on a frame that burns catches and spreads as a wooden one does. */
    @Override
    public int getFlammability(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction face) {
        return level.getBlockEntity(pos) instanceof RackBlockEntity rack && rack.burns() ? 5 : 0;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction face) {
        return getFlammability(state, level, pos, face) > 0 ? 20 : 0;
    }

    /** Fire takes the rack: what was on its shelves comes down loose, to burn or not as it will. */
    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face,
            @Nullable net.minecraft.world.entity.LivingEntity igniter) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RackBlockEntity rack)
            rack.spill();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RackBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        AisleChanges.bump();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        super.onRemove(state, level, pos, newState, moved);
        AisleChanges.bump();
    }
}
