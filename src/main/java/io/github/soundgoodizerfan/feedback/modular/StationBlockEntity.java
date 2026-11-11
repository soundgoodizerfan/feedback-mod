// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/** What a part-making bench holds: the Tool Table's contents, under the name of the block it sits in. */
public class StationBlockEntity extends PartBenchBlockEntity {

    public StationBlockEntity(BlockPos pos, BlockState state) {
        super(FModular.STATION_ENTITY.get(), pos, state, SIZE);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new StationMenu(id, inventory, this,
                getBlockState().getBlock() instanceof StationBlock b ? b.station : Station.TOOL);
    }
}
