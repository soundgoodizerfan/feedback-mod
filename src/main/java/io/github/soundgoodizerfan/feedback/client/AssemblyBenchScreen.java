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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The bench's grid, result and inventory, drawn in flat fills like the other placeholder screens. */
public class AssemblyBenchScreen extends AbstractContainerScreen<AssemblyBenchMenu> {

    public AssemblyBenchScreen(AssemblyBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = AssemblyBenchMenu.INV_Y + 82;
        inventoryLabelY = AssemblyBenchMenu.INV_Y - 11;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        GuiStyle.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (int row = 0; row < AssemblyBenchMenu.GRID; row++)
            for (int column = 0; column < AssemblyBenchMenu.GRID; column++)
                slot(g, AssemblyBenchMenu.GRID_X + column * 18, AssemblyBenchMenu.GRID_Y + row * 18);
        GuiStyle.well(g, leftPos + AssemblyBenchMenu.RESULT_X - 5, topPos + AssemblyBenchMenu.RESULT_Y - 5, 26, 26,
                GuiStyle.SOCKET);
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                slot(g, AssemblyBenchMenu.INV_X + column * 18, AssemblyBenchMenu.INV_Y + row * 18);
        for (int column = 0; column < 9; column++)
            slot(g, AssemblyBenchMenu.INV_X + column * 18, AssemblyBenchMenu.INV_Y + 58);
        g.drawString(font, "→", leftPos + 100, topPos + AssemblyBenchMenu.RESULT_Y + 4, 0xFF404040, false);
    }

    private void slot(GuiGraphics g, int x, int y) {
        GuiStyle.slot(g, leftPos + x, topPos + y);
    }
}
