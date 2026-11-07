// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.column;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A column tray. One class, three registrations: the type is the block, because the player picks it by crafting it. */
public class TrayBlock extends Block implements EntityBlock {

    private final TrayType type;

    public TrayBlock(Properties properties, TrayType type) {
        super(properties);
        this.type = type;
    }

    public TrayType type() {
        return type;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TrayBlockEntity tray)
            player.displayClientMessage(Component.translatable("message.feedback.tray", Math.round(tray.condition() * 100f),
                    tray.getDraw().getFluid().isEmpty() ? Component.translatable("message.feedback.tray.empty")
                            : tray.getDraw().getFluid().getHoverName()), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrayBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((TrayBlockEntity) be).tickServer();
    }
}
