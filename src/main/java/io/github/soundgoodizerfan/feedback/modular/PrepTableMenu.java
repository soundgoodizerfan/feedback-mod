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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Prep Table's menu: {@link ToolTableMenu} for dishes. Parts shapes the containers (bowl, plate, skewer) with the
 * knife; Assemble builds the dish type the picker names; the third tab is Prepare, the Cut minigame's place (not
 * built). A knife takes the hammer's slot and loses a point per container or dish; there is no rework (a dish is
 * eaten, not taken apart) and no plan slot. Each assembly slot holds one food, since a role's count here is one.
 */
public class PrepTableMenu extends ToolTableMenu {

    /** Client-side open: the position comes in the open packet and the slots start empty. */
    public PrepTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        super(FMenus.PREP_TABLE.get(), ToolTableAssembly.Kind.DISH, id, inventory, new SimpleContainer(ToolTableBlockEntity.SIZE), buf.readBlockPos());
    }

    /** Server-side open. */
    public PrepTableMenu(int id, Inventory inventory, PrepTableBlockEntity table) {
        super(FMenus.PREP_TABLE.get(), ToolTableAssembly.Kind.DISH, id, inventory, table, table.getBlockPos());
    }
}
