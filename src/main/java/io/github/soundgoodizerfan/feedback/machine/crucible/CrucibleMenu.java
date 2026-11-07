// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.crucible;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.MenuType;
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

    private final int size;
    private final int tankCapacity;

    public CrucibleMenu(int id, Inventory inventory) {
        this(FMenus.CRUCIBLE.get(), id, inventory, new SimpleContainer(FTuning.CRUCIBLE_SLOTS), new SimpleContainerData(3),
                FTuning.VESSEL_TANK_CAPACITY_MB);
    }

    /** The large crucible's menu: the same screen with {@link FTuning#CRUCIBLE_LARGE_SLOTS} slots. */
    public static CrucibleMenu largeCrucible(int id, Inventory inventory) {
        return new CrucibleMenu(FMenus.LARGE_CRUCIBLE.get(), id, inventory, new SimpleContainer(FTuning.CRUCIBLE_LARGE_SLOTS),
                new SimpleContainerData(3), FTuning.VESSEL_TANK_CAPACITY_MB);
    }

    /** The arc hearth's menu: the same screen with more slots and a deeper tank. */
    public static CrucibleMenu arcHearth(int id, Inventory inventory) {
        return new CrucibleMenu(FMenus.ARC_HEARTH.get(), id, inventory, new SimpleContainer(FTuning.ARC_CHARGE_SLOTS),
                new SimpleContainerData(3), FTuning.ARC_TANK_MB);
    }

    public CrucibleMenu(MenuType<?> type, int id, Inventory inventory, CrucibleBlockEntity crucible) {
        this(type, id, inventory, new View(crucible), new ContainerData() {
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
        }, crucible.getTank().getCapacity());
    }

    private CrucibleMenu(MenuType<?> type, int id, Inventory inventory, Container container, ContainerData data, int tankCapacity) {
        super(type, id);
        this.container = container;
        this.data = data;
        this.size = container.getContainerSize();
        this.tankCapacity = tankCapacity;
        for (int slot = 0; slot < size; slot++)
            addSlot(new Slot(container, slot, slotX(slot, size), slotY(slot, size)));
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, INV_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, 8 + column * 18, INV_Y + 58));
        addDataSlots(data);
    }

    /** Slots sit in one row when there are few, in rows of three when there are many. */
    public static int slotX(int slot, int size) {
        return SLOTS_X + (size > 3 ? slot % 3 : slot) * SLOT_PX;
    }

    public static int slotY(int slot, int size) {
        return size > 3 ? SLOTS_Y - SLOT_PX / 2 + (slot / 3) * SLOT_PX : SLOTS_Y;
    }

    public int size() {
        return size;
    }

    public int tankCapacity() {
        return tankCapacity;
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
        int own = size;
        // Shift-click moves the crucible's own stack without going through takeFrom, so stamp the vessel's heat here.
        if (index < own && container instanceof View view && view.crucible().getLevel() != null)
            ItemHeat.set(held, view.crucible().getTemperature(), view.crucible().getLevel());
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
            return crucible.getContents().size();
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
            for (int slot = 0; slot < crucible.getContents().size(); slot++)
                crucible.takeFrom(slot, Integer.MAX_VALUE);
        }
    }
}
