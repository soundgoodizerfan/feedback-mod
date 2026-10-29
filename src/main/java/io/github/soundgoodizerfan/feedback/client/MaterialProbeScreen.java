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
import java.util.Arrays;
import java.util.List;

import io.github.soundgoodizerfan.feedback.item.MaterialProbeMenu;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** The probed item's material file as label and figure rows, one tab per group; hover a row for what it means. */
public class MaterialProbeScreen extends SkinnedScreen<MaterialProbeMenu> {

    private static final int TEXT_X = 32;
    private static final int TEXT_Y = 26;
    private static final int LINE = 10;

    private int scroll, tab;
    private ItemStack shown = ItemStack.EMPTY;
    private List<MaterialDescription.Row> rows = List.of();

    public MaterialProbeScreen(MaterialProbeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.FORGE);
        imageWidth = 256;
        imageHeight = MaterialProbeMenu.INV_Y + 58 + 18 + 8;
        titleLabelY = 10_000; // the text panel is the whole top half
        inventoryLabelX = MaterialProbeMenu.INV_X;
        inventoryLabelY = MaterialProbeMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.tabs(skin, leftPos + TEXT_X, topPos + 6, textWidth(), 13,
                Arrays.stream(MaterialDescription.TABS)
                        .map(t -> (Component) Component.translatable("screen.feedback.probe.tab." + t)).toList(),
                () -> tab, i -> {
                    tab = i;
                    scroll = 0;
                }));
    }

    private int textWidth() {
        return imageWidth - TEXT_X - 8;
    }

    private int visibleLines() {
        return (MaterialProbeMenu.INV_Y - 14 - TEXT_Y) / LINE;
    }

    /** The rows of the probed item, rebuilt only when the stack changes. */
    private void refresh() {
        ItemStack stack = menu.probed();
        if (ItemStack.isSameItemSameComponents(stack, shown))
            return;
        shown = stack.copy();
        scroll = 0;
        if (stack.isEmpty()) {
            rows = List.of();
            return;
        }
        List<MaterialDescription.Row> out = new ArrayList<>();
        out.add(new MaterialDescription.Row(0, Component.translatable("screen.feedback.probe.row.item"),
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), Component.translatable("screen.feedback.probe.row.item.tip")));
        Material.find(ClientMaterials.on(minecraft.level), stack.getItem())
                .ifPresent(found -> out.addAll(MaterialDescription.of(found.id(), found.material(), found.form())));
        rows = out;
    }

    private List<MaterialDescription.Row> tabRows() {
        return rows.stream().filter(r -> r.tab() == tab).toList();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        refresh();
        skin.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        skin.slot(graphics, leftPos + MaterialProbeMenu.SLOT_X, topPos + MaterialProbeMenu.SLOT_Y);
        inventory(graphics, MaterialProbeMenu.INV_X, MaterialProbeMenu.INV_Y);
        int top = topPos + TEXT_Y, x = leftPos + TEXT_X - 3, w = textWidth() + 6, h = visibleLines() * LINE + 6;
        skin.inset(graphics, x - 1, top - 4, w + 2, h + 2);
        List<MaterialDescription.Row> shownRows = tabRows();
        if (shownRows.isEmpty()) {
            graphics.drawString(font, Component.translatable(shown.isEmpty() ? "screen.feedback.probe.put" : "screen.feedback.probe.none"),
                    leftPos + TEXT_X, top, skin.dim(), false);
            return;
        }
        scroll = Math.max(0, Math.min(scroll, shownRows.size() - visibleLines()));
        int right = leftPos + TEXT_X + textWidth() - 6;
        for (int i = 0; i < visibleLines() && scroll + i < shownRows.size(); i++) {
            var r = shownRows.get(scroll + i);
            int y = top + i * LINE;
            graphics.drawString(font, r.label(), leftPos + TEXT_X, y, skin.ink(), false);
            String fig = font.plainSubstrByWidth(r.figure(), textWidth() - font.width(r.label()) - 14);
            graphics.drawString(font, fig, right - font.width(fig), y, skin.accent(), false);
        }
        skin.scrollbar(graphics, x + w - 3, top - 1, h - 4, shownRows.size(), visibleLines(), scroll);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        super.renderTooltip(g, mouseX, mouseY);
        int row = (mouseY - (topPos + TEXT_Y)) / LINE, i = scroll + row;
        List<MaterialDescription.Row> shownRows = tabRows();
        if (mouseY >= topPos + TEXT_Y && row < visibleLines() && i < shownRows.size()
                && mouseX >= leftPos + TEXT_X && mouseX < leftPos + imageWidth - 8)
            g.renderTooltip(font, font.split(shownRows.get(i).tip(), 180), mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= (int) Math.signum(scrollY) * 3;
        return true;
    }
}
