// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.BlueprintChecklist;
import io.github.soundgoodizerfan.feedback.modular.BlueprintCredit;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;

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

    private static final int W = 220, MAX_ROWS = 14;
    private static final GuiSkin SKIN = GuiSkin.BLUEPRINT;

    private final Opening opening = new Opening(SKIN);
    private final Component heading;
    private final Component credit;
    private final List<BlueprintChecklist.Row> rows;
    private final int h;

    private BlueprintCardScreen(ItemStack sheet, ModularDesign design) {
        super(Component.translatable("screen.feedback.blueprint_card"));
        BlueprintCredit c = BlueprintCredit.of(sheet);
        Component typeName = Component.translatable("item." + design.type().getNamespace() + "." + design.type().getPath());
        heading = c != null && c.titleLine() != null ? c.titleLine() : typeName;
        credit = c == null ? null : c.line();
        rows = BlueprintChecklist.rows(design, List.of(), ClientMaterials.get(), ModularTypes.known());
        h = 62 + Math.min(rows.size(), MAX_ROWS) * 11;
    }

    /** Opens the card of {@code sheet} if it holds a design. */
    public static void open(ItemStack sheet) {
        ModularDesign design = ModularStacks.design(sheet);
        if (design != null)
            Minecraft.getInstance().setScreen(new BlueprintCardScreen(sheet, design));
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - h) / 2;
    }

    @Override
    protected void init() {
        opening.start();
        addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.done"), b -> onClose(), left() + W / 2 - 30, top() + h - 22, 60, 14));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (opening.shut()) {
            super.onClose();
            return;
        }
        int x = left(), y = top();
        opening.render(g, width, height, x, y, W, h, () -> {
            SKIN.panel(g, x, y, W, h);
            g.drawString(font, heading, x + (W - font.width(heading)) / 2, y + 10, SKIN.ink(), false);
            if (credit != null)
                g.drawString(font, credit, x + (W - font.width(credit)) / 2, y + 21, SKIN.dim(), false);
            SKIN.inset(g, x + 8, y + 34, W - 16, h - 60);
            int shown = Math.min(rows.size(), MAX_ROWS);
            for (int i = 0; i < shown; i++)
                g.drawString(font, font.plainSubstrByWidth(Component.literal("- ").append(rows.get(i).text()).getString(), W - 28), x + 13, y + 39 + i * 11, SKIN.ink(), false);
            if (rows.size() > shown)
                g.drawString(font, "...", x + 13, y + 39 + shown * 11, SKIN.dim(), false);
            super.render(g, mouseX, mouseY, partialTick);
        });
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
