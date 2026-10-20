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

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * A container screen made of a {@link GuiSkin}: it opens and closes the way its material does
 * (a blueprint unrolls), with the skin's sounds, the world dimming behind it as it arrives. Input
 * waits until it is open and stops once it starts closing. The panel itself is the subclass's
 * {@code renderBg}, drawn with {@code skin.panel}.
 */
public abstract class SkinnedScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {

    protected final GuiSkin skin;
    private final Opening opening;

    protected SkinnedScreen(M menu, Inventory inventory, Component title, GuiSkin skin) {
        super(menu, inventory, title);
        this.skin = skin;
        this.opening = new Opening(skin);
    }

    @Override
    protected void init() {
        super.init();
        opening.start();
    }

    protected boolean settled() {
        return opening.settled();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (opening.shut()) {
            super.onClose();
            return;
        }
        opening.render(g, width, height, leftPos, topPos, imageWidth, imageHeight,
                () -> super.render(g, mouseX, mouseY, partialTick));
        if (settled())
            renderTooltip(g, mouseX, mouseY);
    }

    /** The dimming is drawn in {@link #render}, faded in with the reveal; this draws only the panel. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBg(g, partialTick, mouseX, mouseY);
    }

    /** The player's inventory at (x, y) relative to the screen: three rows, then the hotbar 4 below. */
    protected void inventory(GuiGraphics g, int x, int y) {
        for (int row = 0; row < 4; row++)
            for (int column = 0; column < 9; column++)
                skin.slot(g, leftPos + x + column * 18, topPos + y + row * 18 + (row == 3 ? 4 : 0));
    }

    /** The title and the inventory's label, in the skin's ink. */
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, skin.ink(), false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, skin.dim(), false);
    }

    @Override
    public void onClose() {
        opening.close();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return !settled() || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return !settled() || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return !settled() || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return opening.closing() || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return opening.closing() || super.charTyped(c, modifiers);
    }
}
