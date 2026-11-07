// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The press's slots and the hotbar, plus its position and die set at open. */
public class ReloadingPressMenu extends AbstractContainerMenu {

    /** Case, charge, second charge, bullet, core, payload, then the round that comes out. */
    public static final int[][] SLOT_XY = {{22, 38}, {66, 38}, {100, 38}, {150, 38}, {184, 38}, {236, 38}, {284, 82}};
    public static final int HOTBAR_X = 80, HOTBAR_Y = 212;
    public static final int WIDTH = 320, HEIGHT = 234;

    private final Container container;
    private final BlockPos pos;
    private final int openedBore;
    private final Player player;

    public ReloadingPressMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, new SimpleContainer(ReloadingPressBlockEntity.SLOTS), buf.readBlockPos(), buf.readVarInt());
    }

    public ReloadingPressMenu(int id, Inventory inventory, ReloadingPressBlockEntity press) {
        this(id, inventory, press, press.getBlockPos(), press.boreMm());
    }

    private ReloadingPressMenu(int id, Inventory inventory, Container container, BlockPos pos, int bore) {
        super(FMenus.RELOADING_PRESS.get(), id);
        this.player = inventory.player;
        this.container = container;
        this.pos = pos;
        this.openedBore = bore;
        for (int i = 0; i < ReloadingPressBlockEntity.SLOTS; i++) {
            final int slot = i;
            addSlot(new Slot(container, i, SLOT_XY[i][0], SLOT_XY[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return ReloadingPressBlockEntity.fits(slot, stack, ClientMaterials.on(player.level()));
                }
            });
        }
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, HOTBAR_X + column * 18, HOTBAR_Y));
    }

    public BlockPos getPos() {
        return pos;
    }

    public int openedBore() {
        return openedBore;
    }


    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack moved = held.copy();
        if (index < ReloadingPressBlockEntity.SLOTS) {
            if (!moveItemStackTo(held, ReloadingPressBlockEntity.SLOTS, slots.size(), true))
                return ItemStack.EMPTY;
        } else {
            boolean placed = false;
            for (int s = 0; s < ReloadingPressBlockEntity.OUTPUT && !placed; s++)
                if (ReloadingPressBlockEntity.fits(s, held, ClientMaterials.on(player.level())))
                    placed = moveItemStackTo(held, s, s + 1, false);
            if (!placed)
                return ItemStack.EMPTY;
        }
        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return held.getCount() == moved.getCount() ? ItemStack.EMPTY : moved;
    }
}
