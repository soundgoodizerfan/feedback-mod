// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.hydride;

import com.mojang.serialization.MapCodec;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The hydride bed: a box of hydrogen-absorbing metal. Use an ingot of such a metal on it to load one; sneak with an empty hand to take
 * one out (refused while the gas it holds would not fit what is left). What a pipe and a flame do with it is the block entity's.
 */
public class HydrideBedBlock extends BaseEntityBlock {

    public static final MapCodec<HydrideBedBlock> CODEC = simpleCodec(HydrideBedBlock::new);

    public HydrideBedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof HydrideBedBlockEntity bed))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var found = Material.find(MaterialIngredient.materialsNow(), stack).orElse(null);
        if (found == null || !found.form().equals("ingot") || !HydrideBedBlockEntity.absorbs(found.material()))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && bed.addIngot(found.id()) && !player.getAbilities().instabuild)
            stack.shrink(1);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof HydrideBedBlockEntity bed))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            var id = bed.takeIngot();
            if (id == null)
                player.displayClientMessage(Component.translatable("feedback.hydride.holds_gas"), true);
            else {
                Material m = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials().get(id);
                ItemStack out = m == null ? ItemStack.EMPTY : m.stack(id, "ingot").orElse(ItemStack.EMPTY);
                if (!player.getInventory().add(out))
                    player.drop(out, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof HydrideBedBlockEntity bed)
            for (var id : bed.ingots()) {
                Material m = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials().get(id);
                if (m != null)
                    m.stack(id, "ingot").ifPresent(s -> popResource(level, pos, s));
            }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HydrideBedBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FBlockEntities.HYDRIDE_BED.get(), (l, p, s, be) -> be.tickServer());
    }
}
