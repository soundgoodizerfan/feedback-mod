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

/** The Prep Table's contents: the Tool Table's, with a knife where the hammer is. */
public class PrepTableBlockEntity extends PartBenchBlockEntity {

    public PrepTableBlockEntity(BlockPos pos, BlockState state) {
        super(FModular.PREP_TABLE_ENTITY.get(), pos, state, SIZE);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.feedback.prep_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PrepTableMenu(id, inventory, this);
    }
}
