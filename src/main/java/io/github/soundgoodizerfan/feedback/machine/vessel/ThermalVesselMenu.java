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
package io.github.soundgoodizerfan.feedback.machine.vessel;

import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.process.FuelTable;
import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Nine identical workpiece slots and one fuel slot set apart from them, for any {@link
 * VesselKind}. There is deliberately no per-kind subclass -- the layout is the same shape
 * regardless of which vessel is open; only {@link ThermalVesselBlockEntity#getKind()} and the
 * numbers behind it differ.
 *
 * <h2>Why the fuel slot is not underneath the grid</h2>
 * Vanilla's furnace puts fuel directly below the one input slot, which reads as "this feeds
 * that." With nine equivalent slots there is no one thing for it to feed -- fuel is consumed by
 * the vessel as a whole, not by any particular slot -- so it sits to the side, visibly its own
 * kind of slot rather than underneath the workpieces it has no special relationship to any one of.
 */
public class ThermalVesselMenu extends AbstractContainerMenu {

    public static final int GRID_X = 26;
    public static final int GRID_Y = 18;
    public static final int GRID_COLUMNS = 3;
    public static final int SLOT_PX = 18;

    public static final int FUEL_X = GRID_X + GRID_COLUMNS * SLOT_PX + 12;
    public static final int FUEL_Y = GRID_Y + SLOT_PX;

    public static final int GAUGE_X = FUEL_X + 30;
    public static final int GAUGE_Y = GRID_Y;
    public static final int GAUGE_HEIGHT = GRID_COLUMNS * SLOT_PX;

    public static final int INV_Y = GRID_Y + GRID_COLUMNS * SLOT_PX + 20;

    private final Container container;
    private final ContainerData data;

    public ThermalVesselMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(ThermalVesselBlockEntity.SLOTS + 1), new SimpleContainerData(3));
    }

    public ThermalVesselMenu(int id, Inventory playerInventory, ThermalVesselBlockEntity vessel) {
        this(id, playerInventory, vessel, vessel.getContainerData());
    }

    private ThermalVesselMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(FMenus.THERMAL_VESSEL.get(), id);
        checkContainerSize(container, ThermalVesselBlockEntity.SLOTS + 1);
        checkContainerDataCount(data, 3);
        this.container = container;
        this.data = data;

        for (int slot = 0; slot < ThermalVesselBlockEntity.SLOTS; slot++) {
            int column = slot % GRID_COLUMNS;
            int row = slot / GRID_COLUMNS;
            addSlot(new HeatStampingSlot(container, slot,
                    GRID_X + column * SLOT_PX, GRID_Y + row * SLOT_PX));
        }
        addSlot(new FuelSlot(container, ThermalVesselBlockEntity.SLOT_FUEL, FUEL_X, FUEL_Y));

        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, INV_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(playerInventory, column, 8 + column * 18, INV_Y + 58));

        addDataSlots(data);
    }

    public boolean isLit() {
        return data.get(0) > 0;
    }

    public float getLitProgress() {
        int duration = data.get(1);
        return duration <= 0 ? 0 : Mth.clamp(data.get(0) / (float) duration, 0f, 1f);
    }

    public int getGaugeTu() {
        return data.get(2);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    private static final int VESSEL_SLOTS = ThermalVesselBlockEntity.SLOTS + 1;

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem())
            return moved;

        ItemStack held = slot.getItem();
        moved = held.copy();

        if (index < VESSEL_SLOTS) {
            // Shift-click moves the slot's own stack, not through removeItem, so stamp it here.
            if (index < ThermalVesselBlockEntity.SLOTS && container instanceof ThermalBody body)
                ItemHeat.set(held, body.getTemperature(), player.level());
            if (!moveItemStackTo(held, VESSEL_SLOTS, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (FuelTable.get().find(held).isPresent()) {
            if (!moveItemStackTo(held, ThermalVesselBlockEntity.SLOT_FUEL, ThermalVesselBlockEntity.SLOT_FUEL + 1, false)
                    && !moveItemStackTo(held, 0, ThermalVesselBlockEntity.SLOTS, false))
                return ItemStack.EMPTY;
        } else if (!moveItemStackTo(held, 0, ThermalVesselBlockEntity.SLOTS, false)) {
            return ItemStack.EMPTY;
        }

        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        if (held.getCount() == moved.getCount())
            return ItemStack.EMPTY;

        slot.onTake(player, held);
        return moved;
    }

    /** Only what {@link FuelTable} actually recognises -- not vanilla's own, unrelated fuel map. */
    private static final class FuelSlot extends Slot {
        FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return FuelTable.get().find(stack).isPresent();
        }
    }

    /**
     * A workpiece slot that stamps whatever leaves it with the vessel's own temperature (§9: a
     * workpiece is stamped at the moment it leaves, the same as the crucible's own {@code
     * removeItem}).
     */
    private static final class HeatStampingSlot extends Slot {
        HeatStampingSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            if (container instanceof ThermalBody body)
                ItemHeat.set(stack, body.getTemperature(), player.level());
            super.onTake(player, stack);
        }
    }
}
