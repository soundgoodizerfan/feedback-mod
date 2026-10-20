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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.weapon.AssemblyBenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The bench's grid, result and inventory: an iron tray of sixteen sockets, a brass pointer to the result. */
public class AssemblyBenchScreen extends SkinnedScreen<AssemblyBenchMenu> {

    public AssemblyBenchScreen(AssemblyBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.FORGE);
        imageWidth = 176;
        imageHeight = AssemblyBenchMenu.INV_Y + 84;
        titleLabelX = 100;
        titleLabelY = 18;
        inventoryLabelY = AssemblyBenchMenu.INV_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        int gx = leftPos + AssemblyBenchMenu.GRID_X, gy = topPos + AssemblyBenchMenu.GRID_Y, side = AssemblyBenchMenu.GRID * 18;
        g.fill(gx - 4, gy - 4, gx + side + 2, gy + side + 2, 0xFF161311);
        g.renderOutline(gx - 4, gy - 4, side + 6, side + 6, 0xFF8C6A32);
        for (int row = 0; row < AssemblyBenchMenu.GRID; row++)
            for (int column = 0; column < AssemblyBenchMenu.GRID; column++)
                skin.slot(g, gx + column * 18, gy + row * 18);
        // The result, in a brass ring, and the pointer to it.
        int rx = leftPos + AssemblyBenchMenu.RESULT_X, ry = topPos + AssemblyBenchMenu.RESULT_Y;
        g.fill(rx - 5, ry - 5, rx + 21, ry + 21, 0xFF5E4520);
        g.renderOutline(rx - 5, ry - 5, 26, 26, 0xFFC8A054);
        skin.slot(g, rx, ry);
        int ay = ry + 7, ax0 = gx + side + 8, ax1 = rx - 9;
        g.fill(ax0, ay, ax1, ay + 2, 0xFFC8A054);
        for (int i = 0; i < 5; i++)
            g.fill(ax1 + i - 1, ay - 4 + i, ax1 + i, ay + 6 - i, 0xFFC8A054);
        inventory(g, AssemblyBenchMenu.INV_X, AssemblyBenchMenu.INV_Y);
    }
}
