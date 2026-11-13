// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.bench;

import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.blueprint.BlueprintChecklist;
import io.github.soundgoodizerfan.feedback.modular.blueprint.BlueprintCredit;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.Opening;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A blueprint as a recipe card (a cooking recipe, not a crafting one): its title and credit, then the parts to make with their materials,
 * shapes and axis values. Opened by right-clicking a blueprint; read from the design the stack carries, so nothing goes to the server.
 */
public class BlueprintCardScreen extends Screen {

    private static final int COL_W = 150, ROW_H = 11;
    private static final GuiSkin SKIN = GuiSkin.BLUEPRINT;

    private final Opening opening = new Opening(SKIN);
    private final Component heading;
    private final Component credit;
    private final List<BlueprintChecklist.Row> rows;
    private int w, h, cols, visibleRows, scroll;

    private BlueprintCardScreen(ItemStack sheet, ModularDesign design) {
        super(Component.translatable("screen.feedback.blueprint_card"));
        BlueprintCredit c = BlueprintCredit.of(sheet);
        Component typeName = Component.translatable("item." + design.type().getNamespace() + "." + design.type().getPath());
        heading = c != null && c.titleLine() != null ? c.titleLine() : typeName;
        credit = c == null ? null : c.line();
        rows = BlueprintChecklist.rows(design, List.of(), ClientMaterials.get(), ModularTypes.known());
    }

    /** Opens the card of {@code sheet} if it holds a design. */
    public static void open(ItemStack sheet) {
        ModularDesign design = ModularStacks.design(sheet);
        if (design != null)
            Minecraft.getInstance().setScreen(new BlueprintCardScreen(sheet, design));
    }

    /** Sized to the window: wide enough for as many columns of entries as fit, up to the whole list, and as tall as the list needs. */
    private void size() {
        int innerMax = Math.max(COL_W, Math.min(width - 40, 560)) - 16;
        cols = Math.max(1, Math.min(innerMax / COL_W, Math.max(1, (rows.size() + 5) / 6)));
        int needRows = (rows.size() + cols - 1) / cols;
        visibleRows = Math.max(3, Math.min(needRows, (height - 90) / ROW_H));
        w = cols * COL_W + 16 + 6;
        h = 62 + visibleRows * ROW_H;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, needRows - visibleRows)));
    }

    private int left() {
        return (width - w) / 2;
    }

    private int top() {
        return (height - h) / 2;
    }

    @Override
    protected void init() {
        size();
        opening.start();
        addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.done"), b -> onClose(), left() + w / 2 - 30, top() + h - 22, 60, 14));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (opening.shut()) {
            super.onClose();
            return;
        }
        int x = left(), y = top();
        opening.render(g, width, height, x, y, w, h, () -> {
            SKIN.panel(g, x, y, w, h);
            g.drawString(font, heading, x + (w - font.width(heading)) / 2, y + 10, SKIN.ink(), false);
            if (credit != null)
                g.drawString(font, credit, x + (w - font.width(credit)) / 2, y + 21, SKIN.dim(), false);
            SKIN.inset(g, x + 8, y + 34, w - 16, h - 60);
            int total = (rows.size() + cols - 1) / cols;
            for (int r = 0; r < visibleRows; r++)
                for (int c = 0; c < cols; c++) {
                    int i = (scroll + r) * cols + c;
                    if (i >= rows.size())
                        break;
                    g.drawString(font, font.plainSubstrByWidth(Component.literal("- ").append(rows.get(i).text()).getString(), COL_W - 6),
                            x + 13 + c * COL_W, y + 39 + r * ROW_H, SKIN.ink(), false);
                }
            if (total > visibleRows)
                SKIN.scrollbar(g, x + w - 12, y + 36, visibleRows * ROW_H + 4, total, visibleRows, scroll);
            super.render(g, mouseX, mouseY, partialTick);
        });
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        int total = (rows.size() + cols - 1) / cols;
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(scrollY), Math.max(0, total - visibleRows)));
        return true;
    }

    /** The dimming is the opening's. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void onClose() {
        opening.close();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return !opening.settled() || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return opening.closing() || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
