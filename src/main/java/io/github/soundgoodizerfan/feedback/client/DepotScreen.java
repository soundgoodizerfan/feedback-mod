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

import io.github.soundgoodizerfan.feedback.village.DepotBlockEntity;
import io.github.soundgoodizerfan.feedback.village.DepotMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** A Depot: the outgoing shelf above the incoming one, and whether the depot is linked and how many parcels are on the road to it. */
public class DepotScreen extends SkinnedScreen<DepotMenu> {

    public DepotScreen(DepotMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.WOOD);
        imageWidth = 176;
        imageHeight = DepotMenu.INV_Y + 58 + 18 + 8;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelY = DepotMenu.INV_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (int i = 0; i < DepotBlockEntity.SLOTS; i++) {
            skin.slot(g, leftPos + 8 + i * 18, topPos + DepotMenu.OUT_Y);
            skin.slot(g, leftPos + 8 + i * 18, topPos + DepotMenu.IN_Y);
        }
        inventory(g, 8, DepotMenu.INV_Y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        super.renderLabels(g, mx, my);
        g.drawString(font, Component.translatable("gui.feedback.depot.outgoing"), 8, DepotMenu.OUT_Y - 10, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.depot.incoming"), 8, DepotMenu.IN_Y - 10, skin.dim(), false);
        Component status = menu.linked()
                ? Component.translatable("gui.feedback.depot.linked", menu.onTheRoad())
                : Component.translatable("gui.feedback.depot.unlinked");
        g.drawString(font, status, 8, DepotMenu.IN_Y + 20, menu.linked() ? skin.ink() : skin.warn(), false);
    }
}
