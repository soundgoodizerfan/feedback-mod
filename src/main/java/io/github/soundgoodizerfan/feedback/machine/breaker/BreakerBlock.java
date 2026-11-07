// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.breaker;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/** A breaker in a line. A hand opens and closes it; so can a controller. See {@link BreakerBlockEntity}. */
public class BreakerBlock extends Block implements EntityBlock {

    public BreakerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BreakerBlockEntity breaker))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            breaker.setEngaged(!breaker.isEngaged());
            player.displayClientMessage(Component.translatable(breaker.isEngaged()
                    ? "feedback.breaker.closed" : "feedback.breaker.open"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BreakerBlockEntity(pos, state);
    }
}
