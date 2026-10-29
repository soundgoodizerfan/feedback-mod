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

import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The cut grid of the stonecutter and the woodworking table: the results for the input in the first cell, one icon each; the
 * one picked is what the output slot holds.
 */
final class CutPanel implements WorkPanel {

    private static final int COLS = 6, CELL = 18;

    private final ToolTableScreen host;

    CutPanel(ToolTableScreen host) {
        this.host = host;
    }

    @Override
    public boolean owns(int mode) {
        return mode == ToolTableMenu.MODE_CUT;
    }

    /** The result under the pointer, or -1. */
    private int at(double mouseX, double mouseY) {
        int cx = (int) Math.floor((mouseX - host.left() - ToolTableScreen.LIST_X) / CELL),
                cy = (int) Math.floor((mouseY - host.top() - ToolTableScreen.PART_Y) / CELL);
        int i = cy * COLS + cx;
        return cx >= 0 && cx < COLS && cy >= 0 && cy * CELL < ToolTableScreen.PART_ROWS * ToolTableScreen.ROW_H
                && i < host.menu().cuts(host.level()).size() ? i : -1;
    }

    @Override
    public void body(GuiGraphics g, int mouseX, int mouseY) {
        int x = host.left() + ToolTableScreen.LIST_X, y = host.top() + ToolTableScreen.PART_Y;
        host.skin.inset(g, x - 2, y - 2, ToolTableScreen.LIST_W + 4, ToolTableScreen.PART_ROWS * ToolTableScreen.ROW_H + 4);
        var all = host.menu().cuts(host.level());
        int hovered = at(mouseX, mouseY);
        for (int i = 0; i < all.size() && i / COLS * CELL < ToolTableScreen.PART_ROWS * ToolTableScreen.ROW_H; i++) {
            int cx = x + i % COLS * CELL, cy = y + i / COLS * CELL;
            host.skin.row(g, cx, cy, CELL, CELL, i == host.menu().picked(), i == hovered);
            g.renderItem(all.get(i), cx + 1, cy + 1);
            if (all.get(i).getCount() > 1)
                g.renderItemDecorations(host.font(), all.get(i), cx + 1, cy + 1);
        }
    }

    @Override
    public void labels(GuiGraphics g) {
        g.drawString(host.font(), Component.translatable("gui.feedback.tool_table.stock"), host.left() + ToolTableMenu.GRID_X,
                host.top() + ToolTableMenu.GRID_Y - 11, host.skin.dim(), false);
    }

    @Override
    public boolean click(double mouseX, double mouseY, int button) {
        if (!host.settled() || host.menu().mode() != ToolTableMenu.MODE_CUT || button != 0 || at(mouseX, mouseY) < 0)
            return false;
        GuiSkin.play(host.skin.click(), 1f);
        host.press(ToolTableMenu.BTN_PICK + at(mouseX, mouseY));
        return true;
    }

    @Override
    public boolean tooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (host.menu().mode() != ToolTableMenu.MODE_CUT || at(mouseX, mouseY) < 0)
            return false;
        g.renderTooltip(host.font(), host.menu().cuts(host.level()).get(at(mouseX, mouseY)), mouseX, mouseY);
        return true;
    }
}
