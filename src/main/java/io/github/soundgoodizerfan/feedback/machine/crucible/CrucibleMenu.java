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
package io.github.soundgoodizerfan.feedback.machine.crucible;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The crucible's two slots and what it is holding in its tank, beside a heat gauge. The slots take inputs and give back products alike:
 * the crucible puts a product in a free slot, so there is no separate output side to draw. What leaves a slot is stamped at the
 * vessel's heat ({@link CrucibleBlockEntity#takeFrom}); the melt is read from the tank, never taken from here (a mold does that).
 */
public class CrucibleMenu extends AbstractContainerMenu {

    public static final int SLOTS_X = 26, SLOTS_Y = 36, SLOT_PX = 18;
    public static final int HEAT_X = 80, TANK_X = 102, GAUGE_Y = 18, GAUGE_W = 10, TANK_W = 18, GAUGE_H = 54;
    public static final int INV_Y = GAUGE_Y + GAUGE_H + 18;

    private final Container container;
    private final ContainerData data;

    public CrucibleMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(FTuning.CRUCIBLE_SLOTS), new SimpleContainerData(3));
    }

    public CrucibleMenu(int id, Inventory inventory, CrucibleBlockEntity crucible) {
        this(id, inventory, new View(crucible), new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> Math.round(crucible.getTemperature().value());
                    case 1 -> crucible.getTank().getFluidAmount();
                    default -> BuiltInRegistries.FLUID.getId(crucible.getTank().getFluid().getFluid());
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 3;
            }
        });
    }

    private CrucibleMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(FMenus.CRUCIBLE.get(), id);
        this.container = container;
        this.data = data;
        for (int slot = 0; slot < FTuning.CRUCIBLE_SLOTS; slot++)
            addSlot(new Slot(container, slot, SLOTS_X + slot * SLOT_PX, SLOTS_Y));
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, INV_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, 8 + column * 18, INV_Y + 58));
        addDataSlots(data);
    }

    public int temperatureTu() {
        return data.get(0);
    }

    public int tankMb() {
        return data.get(1);
    }

    public net.minecraft.world.level.material.Fluid tankFluid() {
        return BuiltInRegistries.FLUID.byId(data.get(2));
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
        int own = FTuning.CRUCIBLE_SLOTS;
        if (index < own ? !moveItemStackTo(held, own, slots.size(), true) : !moveItemStackTo(held, 0, own, false))
            return ItemStack.EMPTY;
        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        if (held.getCount() == moved.getCount())
            return ItemStack.EMPTY;
        slot.onTake(player, held);
        return moved;
    }

    /** The crucible's contents as a container; every change goes through the block entity so it syncs and stamps heat. */
    private record View(CrucibleBlockEntity crucible) implements Container {
        @Override
        public int getContainerSize() {
            return FTuning.CRUCIBLE_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            return crucible.getContents().stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return crucible.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            return crucible.takeFrom(slot, count);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return crucible.takeFrom(slot, Integer.MAX_VALUE);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            crucible.putItem(slot, stack);
        }

        @Override
        public void setChanged() {
            crucible.sync();
        }

        @Override
        public boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(crucible, player);
        }

        @Override
        public void clearContent() {
            for (int slot = 0; slot < FTuning.CRUCIBLE_SLOTS; slot++)
                crucible.takeFrom(slot, Integer.MAX_VALUE);
        }
    }
}
