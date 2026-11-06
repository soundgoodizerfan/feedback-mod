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

import io.github.soundgoodizerfan.feedback.modular.PackMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The worn pack's chest screen with a tinted, rimmed pad behind each store slot so the typed row reads apart from the fixed slots. */
public class PackScreen extends ContainerScreen {

    private static final int[] TINT = {0x00000000, 0x80A0682C, 0x802C7AA0, 0x802C8A4A, 0x80A03C3C, 0x80B8A030};

    private final PackMenu pack;

    public PackScreen(net.minecraft.world.inventory.ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.pack = (PackMenu) menu;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        super.renderBg(g, partialTick, mouseX, mouseY);
        for (var slot : pack.slots) {
            int kind = slot.index < pack.getContainer().getContainerSize() ? pack.kindOf(slot.index) : 0;
            if (kind <= 0)
                continue;
            int x = leftPos + slot.x, y = topPos + slot.y;
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF3B3B3B);
            g.fill(x, y, x + 16, y + 16, TINT[Math.min(kind, TINT.length - 1)]);
        }
    }
}
