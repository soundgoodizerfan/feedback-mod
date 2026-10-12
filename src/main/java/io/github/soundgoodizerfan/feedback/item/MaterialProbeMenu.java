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
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One slot to put an item in, and the player's inventory. The slot hands its item back on close. */
public class MaterialProbeMenu extends AbstractContainerMenu {

    public static final int SLOT_X = 8;
    public static final int SLOT_Y = 8;
    public static final int INV_X = 48;
    public static final int INV_Y = 140;

    private final Container container = new SimpleContainer(1);

    public MaterialProbeMenu(int id, Inventory playerInventory) {
        super(FMenus.MATERIAL_PROBE.get(), id);
        addSlot(new Slot(container, 0, SLOT_X, SLOT_Y));
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(playerInventory, column + row * 9 + 9, INV_X + column * 18, INV_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(playerInventory, column, INV_X + column * 18, INV_Y + 58));
    }

    /** The item being probed, or empty. */
    public ItemStack probed() {
        return container.getItem(0);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, container);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack moved = held.copy();
        boolean ok = index == 0 ? moveItemStackTo(held, 1, slots.size(), true) : moveItemStackTo(held, 0, 1, false);
        if (!ok)
            return ItemStack.EMPTY;
        slot.setChanged();
        return moved;
    }
}
