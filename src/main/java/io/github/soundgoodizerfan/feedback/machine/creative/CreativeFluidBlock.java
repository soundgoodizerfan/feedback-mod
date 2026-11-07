// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.creative;

import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/** The creative fluid source/sink: a bucket of anything sets what it pours. */
public class CreativeFluidBlock extends CreativeSourceBlock {

    public CreativeFluidBlock(Properties properties,
                              BiFunction<BlockPos, BlockState, BlockEntity> entity) {
        super(properties, entity);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.canUseGameMasterBlocks() && level.getBlockEntity(pos) instanceof CreativeFluidBlockEntity source) {
            var held = FluidUtil.getFluidContained(stack);
            if (held.isPresent()) {
                if (!level.isClientSide) {
                    source.setFluid(held.get());
                    player.displayClientMessage(Component.translatable("message.feedback.creative_fluid.set",
                            held.get().getHoverName()), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((CreativeFluidBlockEntity) be).tickServer();
    }
}
