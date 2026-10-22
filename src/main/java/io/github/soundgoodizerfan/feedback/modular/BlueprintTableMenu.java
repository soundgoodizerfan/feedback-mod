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
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FMenus;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Blueprint Table's paper slot, one sample slot per role row and the hotbar. A sample is an
 * item that names its role's material, as at the Gun Stand; it is a pattern to read, not a cost.
 * Printing bakes the drawn design on the server's
 * own tables and turns the paper into a {@link ModularBlueprintItem}; a blueprint put in is opened
 * by the screen to draw on again.
 */
public class BlueprintTableMenu extends AbstractContainerMenu {

    public static final int PAPER_X = 230, PAPER_Y = 290, HOTBAR_X = 8, HOTBAR_Y = 290, H = 312;
    /** Sample slots: one, for the part selected on the sheet. */
    public static final int ROLE_SLOTS = 1;
    /** Button: send the samples back to the player, as the screen's page of rows has changed. */
    public static final int BTN_CLEAR = 1;
    public static final int SAMPLE_X = 62, SAMPLE_Y = 132, SAMPLE_STEP = 0;

    private final Container paper = new SimpleContainer(1);
    private final Container samples = new SimpleContainer(ROLE_SLOTS);
    private final Player player;
    /** Which role rows the screen draws a material for; the other sample slots are inactive. Client side only. */
    private List<Boolean> shown = List.of();
    private final ContainerLevelAccess access;

    /** Client-side open. */
    public BlueprintTableMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public BlueprintTableMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(FMenus.BLUEPRINT_TABLE.get(), id);
        this.access = access;
        this.player = inventory.player;
        addSlot(new Slot(paper, 0, PAPER_X, PAPER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(FTags.BLUEPRINT_PAPER);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < ROLE_SLOTS; row++) {
            int role = row; // not `index`: inside a Slot that names the slot's place in the menu
            addSlot(new Slot(samples, row, SAMPLE_X, SAMPLE_Y + row * SAMPLE_STEP) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isSample(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean isActive() {
                    return role < shown.size() && shown.get(role);
                }
            });
        }
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, HOTBAR_X + column * 18, HOTBAR_Y));
    }

    /** Whether {@code stack} is some material in some form, as this side knows the materials. */
    public boolean isSample(ItemStack stack) {
        return !stack.isEmpty() && Material.find(ClientMaterials.on(player.level()), stack).isPresent();
    }

    /** The screen's role rows, each whether it is drawn: the drawn ones have an active sample slot. */
    public void shown(List<Boolean> shown) {
        this.shown = shown;
    }

    /** The sample in role row {@code row}. */
    public ItemStack sample(int row) {
        return samples.getItem(row);
    }

    private static void send(Player player, Component text) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp)
            io.github.soundgoodizerfan.feedback.client.HudReadouts.send(sp, text, 50);
    }

    /** The screen's Print: bake {@code design} here, where the tables are trusted, and write it onto the paper. */
    public void print(ModularDesign design, Player player) {
        if (paper.getItem(0).isEmpty()) {
            send(player, Component.translatable("message.feedback.blueprint_table.no_paper"));
            return;
        }
        var figures = ToolDesigns.predict(design, ModularTypes.get().types(), MaterialTable.get().materials());
        if (figures.isEmpty()) {
            send(player, Component.translatable("message.feedback.blueprint_table.cannot_print"));
            return;
        }
        ItemStack blueprint = new ItemStack(FModular.BLUEPRINT.get());
        blueprint.set(FModular.DESIGN.get(), design);
        blueprint.set(FModular.FIGURES.get(), figures.get());
        paper.setItem(0, blueprint);
        broadcastChanges();
        send(player, Component.translatable("message.feedback.blueprint_table.printed"));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BTN_CLEAR)
            return false;
        access.execute((level, pos) -> clearContainer(player, samples));
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> {
            clearContainer(player, paper);
            clearContainer(player, samples);
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, FModular.BLUEPRINT_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack moved = held.copy();
        int hotbar = 1 + ROLE_SLOTS;
        if (index < hotbar) {
            if (!moveItemStackTo(held, hotbar, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (held.is(FTags.BLUEPRINT_PAPER)) {
            if (!moveItemStackTo(held, 0, 1, false))
                return ItemStack.EMPTY;
        } else if (isSample(held)) {
            if (!moveItemStackTo(held, 1, 1 + Math.max(shown.size(), 1), false))
                return ItemStack.EMPTY;
        } else
            return ItemStack.EMPTY;
        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return held.getCount() == moved.getCount() ? ItemStack.EMPTY : moved;
    }
}
