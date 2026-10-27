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
package io.github.soundgoodizerfan.feedback.machine.basin;

import io.github.soundgoodizerfan.feedback.item.MoldItem;
import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The basin's four mold slots, a column from the first to be filled at the top, and the player's inventory. */
public class CastingBasinMenu extends AbstractContainerMenu {

    public static final int SLOT_X = 80, SLOT_Y = 18, SLOT_STEP = 22;
    public static final int INVENTORY_X = 8, INVENTORY_Y = 108;

    private final Container container;
    private final ContainerData data;

    public CastingBasinMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(CastingBasinBlockEntity.SLOTS), new SimpleContainerData(2));
    }

    public CastingBasinMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(FMenus.CASTING_BASIN.get(), id);
        this.container = container;
        this.data = data;
        for (int i = 0; i < CastingBasinBlockEntity.SLOTS; i++)
            addSlot(new Slot(container, i, SLOT_X, SLOT_Y + i * SLOT_STEP) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof MoldItem;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, INVENTORY_Y + 58));
        addDataSlots(data);
    }

    /** What the open pool holds, in mB. */
    public int poolMb() {
        return data.get(0);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack moved = held.copy();
        int own = CastingBasinBlockEntity.SLOTS;
        if (index < own ? !moveItemStackTo(held, own, slots.size(), true)
                : !(held.getItem() instanceof MoldItem) || !moveItemStackTo(held, 0, own, false))
            return ItemStack.EMPTY;
        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return moved;
    }
}
