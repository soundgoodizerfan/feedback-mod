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

import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;
import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Gun Table's menu: {@link ToolTableMenu} for guns ({@code docs/feedback_modular_items_spec.md} 8.3).
 * Parts are shaped from stock with a hand hammer, put together in sixteen slots under a blueprint, and
 * reworked, mended and taken apart, by the Tool Table's code; the type filter is the only difference in what it
 * makes. The round slot reads: with a round in it and a gun on offer, the screen bakes that gun against that
 * round ({@link GunSpec#bake(java.util.Map, Cartridge)}) and prints the spec sheet. Nothing on the table
 * changes a gun's or a round's figure (guns spec 13.1).
 */
public class GunTableMenu extends ToolTableMenu {

    /** Client-side open: the position comes in the open packet and the slots start empty. */
    public GunTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        super(FMenus.GUN_TABLE.get(), true, id, inventory,
                new SimpleContainer(io.github.soundgoodizerfan.feedback.modular.ToolTableBlockEntity.GUN_SIZE), buf.readBlockPos());
    }

    /** Server-side open. */
    public GunTableMenu(int id, Inventory inventory, GunTableBlockEntity table) {
        super(FMenus.GUN_TABLE.get(), true, id, inventory, table, table.getBlockPos());
    }
}
