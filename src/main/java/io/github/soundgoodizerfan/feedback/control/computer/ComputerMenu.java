// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerMenu;
import io.github.soundgoodizerfan.feedback.registry.FMenus;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Computer's desktop: the four drive bays and the running slot as desktop icons down the left, the player's hotbar along the top.
 * Everything else the desktop does (windows, monitor, links) is drawn by the screen from the block entity; what changes the world goes
 * through {@link ComputerActionPayload}.
 */
public class ComputerMenu extends AbstractContainerMenu {

    /** Where the icons' slots sit, relative to the screen. */
    public static final int ICON_X = 12, ICON_Y = 28, ICON_STEP = 30;
    public static final int RUNNING = ComputerBays.SIZE;

    private final BlockPos pos;
    @Nullable
    private final ComputerBlockEntity computer;
    private final Container bays;
    private final Container running;

    public ComputerMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, inventory.player.level().getBlockEntity(pos) instanceof ComputerBlockEntity c ? c : null);
    }

    public ComputerMenu(int id, Inventory inventory, BlockPos pos, @Nullable ComputerBlockEntity computer) {
        super(FMenus.COMPUTER.get(), id);
        this.pos = pos;
        this.computer = computer;
        this.bays = computer != null ? computer.bays() : new SimpleContainer(ComputerBays.SIZE);
        this.running = computer != null ? computer.core() : new SimpleContainer(1);
        for (int i = 0; i < ComputerBays.SIZE; i++)
            addSlot(new Slot(bays, i, ICON_X, ICON_Y + i * ICON_STEP));
        addSlot(new Slot(running, 0, ICON_X, ICON_Y + ComputerBays.SIZE * ICON_STEP));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, ProgrammerMenu.HOTBAR_X + column * 18, ProgrammerMenu.STRIP_SLOT_Y));
        if (computer != null && !inventory.player.level().isClientSide)
            computer.watch(1);
    }

    public BlockPos getPos() {
        return pos;
    }

    @Nullable
    public ComputerBlockEntity getComputer() {
        return computer;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (computer != null && !player.level().isClientSide)
            computer.watch(-1);
    }

    @Override
    public boolean stillValid(Player player) {
        return computer == null ? player.level().getBlockEntity(pos) instanceof ComputerBlockEntity : computer.core().stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack moved = held.copy();
        int hotbar = ComputerBays.SIZE + 1;
        if (index < hotbar) {
            if (!moveItemStackTo(held, hotbar, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (held.is(FTags.PUNCH_CARDS)) {
            if (!moveItemStackTo(held, 0, ComputerBays.SIZE, false))
                return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return moved;
    }
}
