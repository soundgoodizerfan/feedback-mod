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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.FastColor;

/**
 * The look every Feedback screen shares: a beveled steel panel, inset sockets, recessed wells and
 * one button. Drawn with fills, not textures, so there is nothing to license and every screen
 * moves together when this file changes.
 */
public final class GuiStyle {

    public static final int PANEL = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, SHADE = 0xFF555555, EDGE = 0xFF1A1A1A,
            SOCKET = 0xFF8B8B8B, WELL = 0xFF202020, TEXT = 0xFF404040;

    private GuiStyle() {}

    /** Raised panel: dark outline, light top-left bevel, shaded bottom-right. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 1, y + 1, x + w - 1, y + 3, LIGHT);
        g.fill(x + 1, y + 1, x + 3, y + h - 1, LIGHT);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, SHADE);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, SHADE);
        g.fill(x + 1, y + h - 3, x + 3, y + h - 1, PANEL); // corners: bevels meet cleanly
        g.fill(x + w - 3, y + 1, x + w - 1, y + 3, PANEL);
    }

    /** Sunken 18x18 slot socket around the 16x16 item at (x, y). */
    public static void slot(GuiGraphics g, int x, int y) {
        well(g, x - 1, y - 1, 18, 18, SOCKET);
    }

    /** A recessed rectangle: dark top-left, light bottom-right, as if cut into the panel. */
    public static void well(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w - 1, y + 1, SHADE);
        g.fill(x, y, x + 1, y + h - 1, SHADE);
        g.fill(x + 1, y + h - 1, x + w, y + h, LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, LIGHT);
    }

    public static void hotbar(GuiGraphics g, int x, int y) {
        for (int c = 0; c < 9; c++) slot(g, x + c * 18, y);
    }

    /** A flat button that brightens under the pointer and sinks when pressed. */
    public static Button button(Component label, Button.OnPress press, int x, int y, int w, int h) {
        return new FButton(x, y, w, h, label, press);
    }

    private static final class FButton extends Button {
        private float glow;
        private long last;

        FButton(int x, int y, int w, int h, Component label, OnPress press) {
            super(x, y, w, h, label, press, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            long now = net.minecraft.Util.getMillis();
            float dt = Math.min(now - last, 100L) / 1000f;
            last = now;
            float target = !active ? 0f : isHoveredOrFocused() ? 1f : 0f;
            glow += Mth.clamp(target - glow, -dt * 8f, dt * 8f);

            boolean down = active && isHovered() && Minecraft.getInstance().mouseHandler.isLeftPressed();
            int top = lerp(0xFFD8D8D8, 0xFFFFFFFF, glow), mid = lerp(0xFFB4B4B4, 0xFFD6E4FF, glow);
            if (!active) { top = 0xFF9A9A9A; mid = 0xFF8B8B8B; }
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            g.fill(x, y, x + w, y + h, EDGE);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, mid);
            if (down) {
                g.fill(x + 1, y + 1, x + w - 1, y + 2, SHADE);
                g.fill(x + 1, y + 1, x + 2, y + h - 1, SHADE);
            } else {
                g.fill(x + 1, y + 1, x + w - 1, y + 2, top);
                g.fill(x + 1, y + 1, x + 2, y + h - 1, top);
                g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, SHADE);
                g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, SHADE);
            }
            int color = active ? (glow > 0.5f ? 0xFF000000 : 0xFF202020) : 0xFF606060;
            g.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + w / 2,
                    y + (h - 8) / 2 + (down ? 1 : 0), color);
        }

        private static int lerp(int a, int b, float t) {
            return FastColor.ARGB32.lerp(t, a, b);
        }
    }
}
