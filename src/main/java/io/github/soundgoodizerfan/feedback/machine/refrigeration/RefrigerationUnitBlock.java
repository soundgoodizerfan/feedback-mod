// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.refrigeration;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A heat pump on a wire that holds an adjacent Ice Box cold. See {@link RefrigerationUnitBlockEntity}. */
public class RefrigerationUnitBlock extends Block implements EntityBlock {

    public RefrigerationUnitBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RefrigerationUnitBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != FBlockEntities.REFRIGERATION_UNIT.get() ? null
                : (l, p, s, be) -> ((RefrigerationUnitBlockEntity) be).tickServer();
    }

    /** An empty hand shows where the room leaks: a puff at each opening, and the room's size in the action bar. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RefrigerationUnitBlockEntity unit && level instanceof ServerLevel server) {
            for (BlockPos at : unit.roomOpeningAt())
                server.sendParticles(ParticleTypes.CLOUD, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.01);
            player.displayClientMessage(Component.translatable("message.feedback.cold_room", unit.roomVolume(), unit.roomOpenings()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
