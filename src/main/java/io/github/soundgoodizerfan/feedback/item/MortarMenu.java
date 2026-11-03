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

import io.github.soundgoodizerfan.feedback.potion.BrewingStandBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Mortar's bench: one slot for plants and a crush button. Crushing sets {@link FDataComponents#CRUSHED} on every plant in the slot
 * (anything with a {@code potion} block) and wears the Mortar in the player's inventory by one per plant; the brewing stand reads the
 * component as surface. Opened from the tab on the inventory screen ({@code net/OpenMortarPayload}); crushing needs a Mortar carried.
 */
public class MortarMenu extends AbstractContainerMenu {

    public static final int SLOT_X = 8, SLOT_Y = 8, INV_X = 8, INV_Y = 100, CRUSH = 0;

    private final Container container = new SimpleContainer(1);
    private final Player player;

    public MortarMenu(int id, Inventory inventory) {
        super(FMenus.MORTAR.get(), id);
        this.player = inventory.player;
        addSlot(new Slot(container, 0, SLOT_X, SLOT_Y));
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, INV_X + column * 18, INV_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, INV_X + column * 18, INV_Y + 58));
    }

    /** The plant in the slot, or empty. */
    public ItemStack plant() {
        return container.getItem(0);
    }

    /** The first Mortar the player carries, or empty. */
    public static ItemStack mortarOf(Player player) {
        for (ItemStack s : player.getInventory().items)
            if (s.is(FItems.MORTAR.get()))
                return s;
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player who, int id) {
        ItemStack plant = plant(), mortar = mortarOf(who);
        if (id != CRUSH || plant.isEmpty() || plant.has(FDataComponents.CRUSHED.get()) || !(who.level() instanceof ServerLevel server))
            return false;
        if (mortar.isEmpty()) {
            who.displayClientMessage(Component.translatable("message.feedback.mortar.none"), true);
            return false;
        }
        if (BrewingStandBlockEntity.potionOf(server, plant).isEmpty()) {
            who.displayClientMessage(Component.translatable("message.feedback.mortar.nothing"), true);
            return false;
        }
        plant.set(FDataComponents.CRUSHED.get(), Unit.INSTANCE);
        mortar.hurtAndBreak(plant.getCount(), server, who, i -> {
        });
        container.setChanged();
        return true;
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
