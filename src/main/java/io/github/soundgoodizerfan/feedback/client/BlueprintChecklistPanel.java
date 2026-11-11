// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.BlueprintChecklist;
import io.github.soundgoodizerfan.feedback.modular.BlueprintCredit;
import io.github.soundgoodizerfan.feedback.modular.ModularBlueprints;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The bench's collapsible checklist: the parts of the blueprint in the plan slot, each ticked while the bench holds it. Drawn beside the
 * panel (right, or left when the window is too narrow), a header that folds it away. It lists a missing material and never blocks one.
 * The rows are recomputed only when the stacks on the bench change.
 */
final class BlueprintChecklistPanel {

    private static final int W = 150, ROW = 10;

    private final GuiSkin skin;
    private final ToolTableMenu menu;
    private boolean collapsed;
    private int key = Integer.MIN_VALUE;
    private List<BlueprintChecklist.Row> rows = List.of();
    private Component heading = Component.empty();
    private int x, y;

    BlueprintChecklistPanel(GuiSkin skin, ToolTableMenu menu) {
        this.skin = skin;
        this.menu = menu;
    }

    private void refresh() {
        List<ItemStack> held = menu.parts();
        ItemStack plan = menu.plan();
        int now = ItemStack.hashStackList(held) * 31 + ItemStack.hashItemAndComponents(plan);
        if (now == key)
            return;
        key = now;
        rows = List.of();
        heading = Component.empty();
        var sheets = ModularBlueprints.sheets(plan);
        if (sheets.isEmpty())
            return;
        // in a book, the sheet of the type the centre already is; else the first
        ModularDesign centre = ModularStacks.design(menu.centreStack());
        ItemStack sheet = sheets.stream().filter(s -> centre != null && ModularStacks.design(s).type().equals(centre.type())).findFirst().orElse(sheets.get(0));
        ModularDesign design = ModularStacks.design(sheet);
        BlueprintCredit credit = BlueprintCredit.of(sheet);
        heading = credit != null && credit.titleLine() != null ? credit.titleLine()
                : Component.translatable("item." + design.type().getNamespace() + "." + design.type().getPath());
        rows = new ArrayList<>(BlueprintChecklist.rows(design, held, ClientMaterials.get(), ModularTypes.known()));
    }

    /** Draws it at the panel's side; {@code left}, {@code top}, {@code panelW} are the bench panel's place. */
    void draw(GuiGraphics g, Font font, int left, int top, int panelW, int screenW) {
        refresh();
        if (rows.isEmpty())
            return;
        x = left + panelW + 6 + W <= screenW ? left + panelW + 6 : left - 6 - W;
        y = top + 4;
        skin.plaque(g, x, y, W, 14);
        g.drawString(font, Component.translatable(collapsed ? "gui.feedback.checklist.open" : "gui.feedback.checklist.close"), x + 4, y + 3, skin.ink(), false);
        if (collapsed)
            return;
        int h = 8 + rows.size() * ROW + 12;
        skin.inset(g, x, y + 16, W, h);
        g.drawString(font, font.plainSubstrByWidth(heading.getString(), W - 8), x + 4, y + 20, skin.accent(), false);
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            String line = (row.held() ? "[x] " : "[ ] ") + row.text().getString();
            g.drawString(font, font.plainSubstrByWidth(line, W - 8), x + 4, y + 32 + i * ROW, row.held() ? skin.ink() : skin.dim(), false);
        }
    }

    /** A click on the header folds or unfolds it. */
    boolean click(double mx, double my, int button) {
        if (rows.isEmpty() || button != 0 || mx < x || mx >= x + W || my < y || my >= y + 14)
            return false;
        collapsed = !collapsed;
        skin.click();
        return true;
    }
}
