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
package io.github.soundgoodizerfan.feedback.control.programmer;

import io.github.soundgoodizerfan.feedback.registry.FTags;
import io.github.soundgoodizerfan.feedback.registry.FMenus;

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
 * One Punch Card slot plus the player's hotbar, in a strip along the top of a full-window
 * screen -- the main inventory is left out so the canvas gets the room. The node canvas itself is not slot-based
 * -- it lives entirely in {@code ProgrammerScreen}, reading {@link ProgrammerBlockEntity#getDisplayedGraph()}
 * straight off the block entity the way Jade already reads a debug controller's links, and
 * sending edits back over {@link ProgrammerCommitPayload} rather than through a slot.
 */
public class ProgrammerMenu extends AbstractContainerMenu {

    private static final int CARD_SLOT = 0;
    /** Slot row in the screen's top strip; {@code ProgrammerScreen.STRIP_H} must clear it. Slot positions are final, so a strip along the bottom of a resizable window isn't possible. */
    public static final int STRIP_SLOT_Y = 6;
    public static final int CARD_SLOT_X = 8;
    public static final int HOTBAR_X = 34;

    private final Container container;
    private final BlockPos pos;
    @Nullable
    private final ProgrammerBlockEntity programmer;

    /** Client-side open. */
    public ProgrammerMenu(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, pos, resolve(playerInventory, pos));
    }

    /** Server-side open. */
    public ProgrammerMenu(int id, Inventory playerInventory, ProgrammerBlockEntity programmer) {
        this(id, playerInventory, programmer.getBlockPos(), programmer);
    }

    private ProgrammerMenu(int id, Inventory playerInventory, BlockPos pos, @Nullable ProgrammerBlockEntity programmer) {
        super(FMenus.PROGRAMMER.get(), id);
        this.pos = pos;
        this.programmer = programmer;
        this.container = programmer != null ? programmer : new SimpleContainer(1);

        addSlot(new Slot(container, CARD_SLOT, CARD_SLOT_X, STRIP_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(FTags.PUNCH_CARDS);
            }
        });

        for (int column = 0; column < 9; column++)
            addSlot(new Slot(playerInventory, column, HOTBAR_X + column * 18, STRIP_SLOT_Y));
    }

    @Nullable
    private static ProgrammerBlockEntity resolve(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof ProgrammerBlockEntity programmer
                ? programmer : null;
    }

    public BlockPos getPos() {
        return pos;
    }

    @Nullable
    public ProgrammerBlockEntity getProgrammer() {
        return programmer;
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

        if (index == CARD_SLOT) {
            if (!moveItemStackTo(held, 1, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (held.is(FTags.PUNCH_CARDS)) {
            if (!moveItemStackTo(held, CARD_SLOT, CARD_SLOT + 1, false))
                return ItemStack.EMPTY;
        } else {
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
}
