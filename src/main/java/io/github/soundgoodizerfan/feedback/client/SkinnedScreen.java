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

import net.minecraft.Util;
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

    private static final float OPEN_MS = 320, CLOSE_MS = 200;

    protected final GuiSkin skin;
    private long openedAt = -1, closingAt = -1;
    /** How far open it was when closing began, so a screen shut mid-unroll rolls up from there. */
    private float closingFrom;

    protected SkinnedScreen(M menu, Inventory inventory, Component title, GuiSkin skin) {
        super(menu, inventory, title);
        this.skin = skin;
    }

    @Override
    protected void init() {
        super.init();
        if (openedAt < 0) { // init runs again on every resize and rebuild
            openedAt = Util.getMillis();
            GuiSkin.play(skin.open(), 1f);
        }
    }

    /** 0 shut, 1 open, linear in time. */
    private float progress() {
        long now = Util.getMillis();
        if (closingAt >= 0)
            return Math.max(0, closingFrom - (now - closingAt) / CLOSE_MS);
        return Math.min(1, (now - openedAt) / OPEN_MS);
    }

    protected boolean settled() {
        return closingAt < 0 && progress() >= 1;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float t = progress();
        if (closingAt >= 0 && t <= 0) {
            super.onClose();
            return;
        }
        float e = GuiStyle.easeOut(t);
        g.fillGradient(0, 0, width, height, (int) (0xC0 * e) << 24 | 0x101010, (int) (0xD0 * e) << 24 | 0x101010);
        skin.beginReveal(g, leftPos, topPos, imageWidth, imageHeight, e);
        super.render(g, mouseX, mouseY, partialTick);
        skin.endReveal(g, leftPos, topPos, imageWidth, imageHeight, e);
        if (settled())
            renderTooltip(g, mouseX, mouseY);
    }

    /** The dimming is drawn in {@link #render}, faded in with the reveal; this draws only the panel. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBg(g, partialTick, mouseX, mouseY);
    }

    @Override
    public void onClose() {
        if (closingAt >= 0)
            return;
        closingFrom = progress();
        closingAt = Util.getMillis();
        GuiSkin.play(skin.close(), 1f);
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
        return closingAt >= 0 || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return closingAt >= 0 || super.charTyped(c, modifiers);
    }
}
