// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.xp;

import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The four world-facing taps on the XP wire; one class, the kind says which source it drinks. */
public class XpTapBlock extends Block implements EntityBlock {

    public enum Kind {
        /** Loose orbs in reach. */
        ORB,
        /** XP of ore broken in reach. */
        ORE,
        /** XP of a villager's trade in reach. */
        TRADE,
        /** XP banked by a vessel beside it. */
        SMELTER
    }

    private final Kind kind;

    public XpTapBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new XpTapBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != FBlockEntities.XP_TAP.get() || (kind != Kind.ORB && kind != Kind.SMELTER))
            return null;
        return (lvl, pos, st, be) -> XpTapBlockEntity.serverTick(lvl, pos, st, (XpTapBlockEntity) be);
    }
}
