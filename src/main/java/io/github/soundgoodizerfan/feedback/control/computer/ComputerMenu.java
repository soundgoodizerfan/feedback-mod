// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

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
 * The Computer's desktop: the four drive bays and the running slot as desktop icons down the left, in the manner of a Plasma desktop, the player's hotbar as a dock along the bottom.
 * Everything else the desktop does (windows, monitor, links) is drawn by the screen from the block entity; what changes the world goes
 * through {@link ComputerActionPayload}.
 */
public class ComputerMenu extends AbstractContainerMenu implements io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerAccess {

    /** The desktop panel's size; the slots below are placed against it. */
    public static final int PANEL_W = 440, PANEL_H = 260;
    /** Where the drive icons' slots sit, relative to the panel: a column down the left of the desktop, a label under each. */
    public static final int ICON_X = 14, ICON_Y = 10, ICON_STEP = 29;
    /** The drive icons' column when the settings put it on the right. */
    public static final int ICON_X_RIGHT = PANEL_W - ICON_X - 16;
    /** The taskbar's height and the gap under the desktop. */
    public static final int BAR_H = 20, BAR_GAP = 4;
    /** The dock: the hotbar, a floating strip centred along the bottom of the desktop, above the taskbar. */
    public static final int DOCK_X = (PANEL_W - 9 * 18) / 2 + 1, DOCK_Y = PANEL_H - BAR_H - BAR_GAP - 30;
    public static final int RUNNING = ComputerBays.SIZE;

    private final BlockPos pos;
    @Nullable
    private final ComputerBlockEntity computer;
    private final Container bays;
    private final Container running;
    private final boolean iconsRight;

    public ComputerMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, inventory.player.level().getBlockEntity(pos) instanceof ComputerBlockEntity c ? c : null);
    }

    public ComputerMenu(int id, Inventory inventory, BlockPos pos, @Nullable ComputerBlockEntity computer) {
        super(FMenus.COMPUTER.get(), id);
        this.pos = pos;
        this.computer = computer;
        this.iconsRight = computer != null && computer.settings().iconsRight();
        this.bays = computer != null ? computer.bays() : new SimpleContainer(ComputerBays.SIZE);
        this.running = computer != null ? computer.core() : new SimpleContainer(1);
        for (int i = 0; i < ComputerBays.SIZE; i++)
            addSlot(new Slot(bays, i, iconX(), ICON_Y + i * ICON_STEP));
        addSlot(new Slot(running, 0, iconX(), ICON_Y + ComputerBays.SIZE * ICON_STEP));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, DOCK_X + column * 18, DOCK_Y));
        if (computer != null && !inventory.player.level().isClientSide) {
            // a medium left in the editor (a restart, a crash) goes home when the next desktop opens on an idle Computer
            if (computer.watching() == 0 && inventory.player instanceof net.minecraft.server.level.ServerPlayer server)
                computer.closeEditor(server);
            computer.watch(1);
        }
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    /** Where the icon column's slots are, in this menu: a setting read when the desktop opened. */
    public int iconX() {
        return iconsRight ? ICON_X_RIGHT : ICON_X;
    }

    @Nullable
    public ComputerBlockEntity getComputer() {
        return computer;
    }

    @Override
    @Nullable
    public io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerBlockEntity getProgrammer() {
        return computer == null ? null : computer.programmer();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (computer != null && !player.level().isClientSide) {
            computer.watch(-1);
            if (player instanceof net.minecraft.server.level.ServerPlayer server)
                computer.closeEditor(server);
        }
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
