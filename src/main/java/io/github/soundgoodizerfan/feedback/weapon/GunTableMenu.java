// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;
import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Gun Table's menu: {@link ToolTableMenu} for guns.
 * Parts are shaped from stock with a hand hammer, put together in sixteen slots under a blueprint, and
 * reworked, mended and taken apart, by the Tool Table's code; the type filter is the only difference in what it
 * makes. The round slot reads: with a round in it and a gun on offer, the screen bakes that gun against that
 * round ({@link GunSpec#bake(java.util.Map, Cartridge)}) and prints the spec sheet. Nothing on the table
 * changes a gun's or a round's figure (guns spec 13.1).
 */
public class GunTableMenu extends ToolTableMenu {

    /** Client-side open: the position comes in the open packet and the slots start empty. */
    public GunTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        super(FMenus.GUN_TABLE.get(), io.github.soundgoodizerfan.feedback.modular.ToolTableAssembly.Kind.GUN, id, inventory,
                new SimpleContainer(io.github.soundgoodizerfan.feedback.modular.ToolTableBlockEntity.GUN_SIZE), buf.readBlockPos());
    }

    /** Server-side open. */
    public GunTableMenu(int id, Inventory inventory, GunTableBlockEntity table) {
        super(FMenus.GUN_TABLE.get(), io.github.soundgoodizerfan.feedback.modular.ToolTableAssembly.Kind.GUN, id, inventory, table, table.getBlockPos());
    }
}
