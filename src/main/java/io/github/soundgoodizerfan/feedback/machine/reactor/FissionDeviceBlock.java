// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.reactor;

import io.github.soundgoodizerfan.feedback.core.NuclearConfig;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A fission device: a plutonium core inside a conventional explosive shell, set off by a redstone signal. Config-gated (default off, {@link
 * NuclearConfig#ARMS}): when off it sits inert and says so; nothing else about the mod changes. When it goes off the blast follows the world's
 * game rules like every blast, and the ground around it is left as radioactive slag, the same stamped slag a breached core leaves.
 */
public class FissionDeviceBlock extends Block {

    public FissionDeviceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        if (level instanceof ServerLevel server && server.hasNeighborSignal(pos))
            detonate(server, pos, null);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                                   net.minecraft.world.phys.BlockHitResult hit) {
        if (!NuclearConfig.ARMS.get()) {
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("feedback.fission_device.inert"), true);
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    private static void detonate(ServerLevel server, BlockPos pos, Player by) {
        if (!NuclearConfig.ARMS.get())
            return;
        server.removeBlock(pos, false);
        server.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ReactorTuning.FISSION_DEVICE_BLAST,
                Level.ExplosionInteraction.BLOCK);
        int reach = ReactorTuning.REACTOR_SLAG_REACH * 2;
        for (int i = 0; i < ReactorTuning.REACTOR_SLAG_BLOCKS * 4; i++) {
            BlockPos at = pos.offset(server.random.nextIntBetweenInclusive(-reach, reach),
                    server.random.nextIntBetweenInclusive(-reach, 0), server.random.nextIntBetweenInclusive(-reach, reach));
            BlockState here = server.getBlockState(at);
            if (here.isAir() || here.canBeReplaced()) {
                server.setBlock(at, FBlocks.CORE_SLAG.get().defaultBlockState(), 3);
                if (server.getBlockEntity(at) instanceof CoreSlagBlockEntity slag)
                    slag.stamp(ReactorTuning.REACTOR_SLAG_RU, server.getGameTime());
            }
        }
    }
}
