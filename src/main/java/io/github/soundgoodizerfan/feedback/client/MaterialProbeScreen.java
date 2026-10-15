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


import io.github.soundgoodizerfan.feedback.item.MaterialProbeMenu;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Prints the probed item's whole material file, as plain sentences. Scrolls. */
public class MaterialProbeScreen extends AbstractContainerScreen<MaterialProbeMenu> {

    private static final int SOCKET = 0xFF8B8B8B;
    private static final int TEXT_BG = 0xFF202020;
    private static final int TEXT_X = 32;
    private static final int TEXT_Y = 8;
    private static final int LINE = 10;

    private int scroll;

    public MaterialProbeScreen(MaterialProbeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = MaterialProbeMenu.INV_Y + 58 + 18 + 6;
        titleLabelY = 10_000; // the text panel is the whole top half
        inventoryLabelX = MaterialProbeMenu.INV_X;
        inventoryLabelY = MaterialProbeMenu.INV_Y - 11;
    }

    private int textWidth() {
        return imageWidth - TEXT_X - 8;
    }

    private int visibleLines() {
        return (MaterialProbeMenu.INV_Y - 14 - TEXT_Y) / LINE;
    }

    private List<FormattedCharSequence> lines() {
        ItemStack stack = menu.probed();
        List<String> raw = new ArrayList<>();
        if (stack.isEmpty()) {
            raw.add("Put an item in the slot.");
        } else {
            raw.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            Material.find(ClientMaterials.on(minecraft.level), stack.getItem()).ifPresentOrElse(found -> {
                raw.add("");
                raw.addAll(MaterialDescription.of(found.id(), found.material(), found.form()));
            }, () -> raw.add("No material file lists this item."));
        }
        List<FormattedCharSequence> out = new ArrayList<>();
        for (String line : raw)
            out.addAll(font.split(Component.literal(line), textWidth()));
        return out;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        GuiStyle.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        GuiStyle.slot(graphics, leftPos + MaterialProbeMenu.SLOT_X, topPos + MaterialProbeMenu.SLOT_Y);
        for (int row = 0; row < 4; row++)
            for (int column = 0; column < 9; column++) {
                int x = leftPos + MaterialProbeMenu.INV_X + column * 18;
                int y = topPos + MaterialProbeMenu.INV_Y + row * 18 + (row == 3 ? 4 : 0);
                GuiStyle.slot(graphics, x, y);
            }
        int top = topPos + TEXT_Y;
        GuiStyle.well(graphics, leftPos + TEXT_X - 3, top - 3, textWidth() + 6, visibleLines() * LINE + 6, TEXT_BG);
        List<FormattedCharSequence> lines = lines();
        scroll = Math.max(0, Math.min(scroll, lines.size() - visibleLines()));
        for (int i = 0; i < visibleLines() && scroll + i < lines.size(); i++)
            graphics.drawString(font, lines.get(scroll + i), leftPos + TEXT_X, top + i * LINE, 0xFFE0E0E0, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= (int) Math.signum(scrollY) * 3;
        return true;
    }
}
