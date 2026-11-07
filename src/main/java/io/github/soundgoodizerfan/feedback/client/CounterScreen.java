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
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.village.CounterBlockEntity;
import io.github.soundgoodizerfan.feedback.village.CounterMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Both sides of a Counter on one wooden panel. The customer side shows the shelf with a price under each slot; a click buys one item,
 * Shift-click a stack. The keeper side stocks the shelf, steps a price under each slot (Shift for eight), and holds the till and the returns.
 */
public class CounterScreen extends SkinnedScreen<CounterMenu> {

    public CounterScreen(CounterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.WOOD);
        imageWidth = 176;
        imageHeight = menu.imageHeight();
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelY = menu.invY() - 11;
    }

    @Override
    protected void init() {
        super.init();
        if (menu.mode() != CounterMenu.KEEPER)
            return;
        for (int i = 0; i < CounterBlockEntity.SHELF; i++) {
            final int slot = i;
            int x = leftPos + CounterMenu.SHELF_X + i * 18;
            int y = topPos + CounterMenu.PRICE_BTN_Y;
            addRenderableWidget(GuiStyle.button(skin, Component.literal("-"), b -> step(slot, 0), x, y, 8, 10));
            addRenderableWidget(GuiStyle.button(skin, Component.literal("+"), b -> step(slot, 1), x + 8, y, 8, 10));
        }
    }

    private void step(int slot, int plus) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, slot * 4 + plus + (Screen.hasShiftDown() ? 2 : 0));
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (menu.mode() == CounterMenu.BUY && slot != null && slot.index < menu.shelfSlotCount()) {
            if (slot.hasItem())
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                        (Screen.hasShiftDown() ? CounterMenu.BUY_STACK : CounterMenu.BUY_ONE) + slot.index);
            return;
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (int i = 0; i < CounterBlockEntity.SHELF; i++)
            skin.slot(g, leftPos + CounterMenu.SHELF_X + i * 18, topPos + CounterMenu.SHELF_Y);
        if (menu.mode() == CounterMenu.KEEPER) {
            for (int i = 0; i < 9; i++) {
                skin.slot(g, leftPos + CounterMenu.SHELF_X + i * 18, topPos + CounterMenu.TILL_Y);
                skin.slot(g, leftPos + CounterMenu.SHELF_X + i * 18, topPos + CounterMenu.RETURN_Y);
            }
        }
        inventory(g, 8, menu.invY());
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        super.renderLabels(g, mx, my);
        boolean keeper = menu.mode() == CounterMenu.KEEPER;
        for (int i = 0; i < CounterBlockEntity.SHELF; i++) {
            if (!menu.getSlot(i).hasItem())
                continue;
            int price = keeper ? menu.price(i) : menu.shown(i);
            String text = price > 0 ? Integer.toString(price) : "~";
            int y = keeper ? CounterMenu.PRICE_TEXT_Y : CounterMenu.PRICE_BTN_Y;
            int colour = keeper && menu.market(i) > 0 && price > menu.market(i) ? skin.warn() : skin.ink();
            g.drawString(font, text, CounterMenu.SHELF_X + i * 18 + 8 - font.width(text) / 2, y, colour, false);
        }
        if (keeper) {
            g.drawString(font, Component.translatable("gui.feedback.counter.till"), 8, CounterMenu.TILL_LABEL_Y, skin.dim(), false);
            g.drawString(font, Component.translatable("gui.feedback.counter.returns"), 8, CounterMenu.RETURN_LABEL_Y, skin.dim(), false);
        } else {
            g.drawString(font, Component.translatable("gui.feedback.counter.hint"), 8, CounterMenu.PRICE_BTN_Y + 12, skin.dim(), false);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mx, int my) {
        Slot slot = hoveredSlot;
        if (slot != null && slot.hasItem() && slot.index < menu.shelfSlotCount()) {
            ItemStack stack = slot.getItem();
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, stack));
            if (menu.mode() == CounterMenu.KEEPER)
                lines.add(menu.market(slot.index) > 0
                        ? Component.translatable("gui.feedback.counter.market", menu.market(slot.index))
                        : Component.translatable("gui.feedback.counter.no_market"));
            else
                lines.add(Component.translatable("gui.feedback.counter.price", menu.shown(slot.index)));
            g.renderComponentTooltip(font, lines, mx, my);
            return;
        }
        super.renderTooltip(g, mx, my);
    }
}
