// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.modular.ToolTableBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/** The Gun Table's contents: the Tool Table's, with more part slots and a round ({@link ToolTableBlockEntity#GUN_SIZE}). */
public class GunTableBlockEntity extends ToolTableBlockEntity {

    public GunTableBlockEntity(BlockPos pos, BlockState state) {
        super(FWeapons.GUN_TABLE_ENTITY.get(), pos, state, GUN_SIZE);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.feedback.gun_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GunTableMenu(id, inventory, this);
    }
}
