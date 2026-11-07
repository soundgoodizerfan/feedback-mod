// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** A Depot's container: the outgoing shelf (goods for the next convoy), the incoming shelf (what arrived, take only), and the inventory. */
public class DepotMenu extends AbstractContainerMenu {

    public static final int OUT_Y = 20, IN_Y = 56, INV_Y = 90;

    private final BlockPos pos;
    private final DepotBlockEntity depot;
    private final ContainerData data;

    public DepotMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    public DepotMenu(int id, Inventory inv, BlockPos pos) {
        super(FMenus.DEPOT.get(), id);
        this.pos = pos;
        this.depot = !inv.player.level().isClientSide && inv.player.level().getBlockEntity(pos) instanceof DepotBlockEntity d ? d : null;
        ItemStackHandler out = depot != null ? depot.outgoing() : new ItemStackHandler(DepotBlockEntity.SLOTS);
        ItemStackHandler in = depot != null ? depot.incoming() : new ItemStackHandler(DepotBlockEntity.SLOTS);
        for (int i = 0; i < DepotBlockEntity.SLOTS; i++)
            addSlot(new SlotItemHandler(out, i, 8 + i * 18, OUT_Y));
        for (int i = 0; i < DepotBlockEntity.SLOTS; i++)
            addSlot(new SlotItemHandler(in, i, 8 + i * 18, IN_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, 9 + row * 9 + col, 8 + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, INV_Y + 58));
        // Data: 0 linked (0 or 1), 1 parcels on the road to this depot.
        this.data = depot != null ? new ContainerData() {
            @Override
            public int get(int i) {
                return i == 0 ? (depot.link() != null ? 1 : 0) : depot.onTheRoad();
            }

            @Override
            public void set(int i, int v) {
            }

            @Override
            public int getCount() {
                return 2;
            }
        } : new SimpleContainerData(2);
        addDataSlots(data);
    }

    public boolean linked() {
        return data.get(0) == 1;
    }

    public int onTheRoad() {
        return data.get(1);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int invStart = slots.size() - 36;
        if (index < invStart) {
            if (!moveItemStackTo(stack, invStart, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, DepotBlockEntity.SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player p) {
        return p.level().isClientSide || (depot != null && !depot.isRemoved() && p.distanceToSqr(pos.getCenter()) < 64.0);
    }
}
