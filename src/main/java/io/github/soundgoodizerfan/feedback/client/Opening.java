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

/**
 * A skinned screen's arrival and departure in time: the world dims behind it, the skin reveals
 * it the way its material does, its sounds play, and input waits until it is open. Shared by
 * {@link SkinnedScreen} and the plain screens drawn on a skin.
 */
final class Opening {

    private static final float OPEN_MS = 320, CLOSE_MS = 200;

    private final GuiSkin skin;
    private long openedAt = -1, closingAt = -1;
    /** How far open it was when closing began, so a screen shut mid-reveal closes from there. */
    private float closingFrom;

    Opening(GuiSkin skin) {
        this.skin = skin;
    }

    /** Begin opening, once: a screen's init runs again on every resize and rebuild. */
    void start() {
        if (openedAt < 0) {
            openedAt = Util.getMillis();
            GuiSkin.play(skin.open(), 1f);
        }
    }

    /** 0 shut, 1 open, linear in time. */
    float progress() {
        long now = Util.getMillis();
        if (closingAt >= 0)
            return Math.max(0, closingFrom - (now - closingAt) / CLOSE_MS);
        return Math.min(1, (now - openedAt) / OPEN_MS);
    }

    boolean settled() {
        return closingAt < 0 && progress() >= 1;
    }

    boolean closing() {
        return closingAt >= 0;
    }

    /** Closed all the way: the screen can go. */
    boolean shut() {
        return closingAt >= 0 && progress() <= 0;
    }

    void close() {
        if (closingAt >= 0)
            return;
        closingFrom = progress();
        closingAt = Util.getMillis();
        GuiSkin.play(skin.close(), 1f);
    }

    /** Dim the world and draw {@code body}, the panel at (x, y, w, h), revealed as far as it has opened. */
    void render(GuiGraphics g, int width, int height, int x, int y, int w, int h, Runnable body) {
        render(g, width, height, x, y, w, h, 1f, body);
    }

    /** As {@link #render(GuiGraphics, int, int, int, int, int, int, Runnable)}, the dimming scaled by {@code dim} (a popup over a screen dims less). */
    void render(GuiGraphics g, int width, int height, int x, int y, int w, int h, float dim, Runnable body) {
        float e = GuiStyle.easeOut(progress());
        g.fillGradient(0, 0, width, height, (int) (0xC0 * e * dim) << 24 | 0x101010, (int) (0xD0 * e * dim) << 24 | 0x101010);
        skin.beginReveal(g, x, y, w, h, e);
        body.run();
        skin.endReveal(g, x, y, w, h, e);
    }
}
