/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
