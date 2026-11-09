// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;

/**
 * How the phosphor terminal skins ({@link GuiSkin#TERMINAL} and its three tints) are drawn. Every colour is derived from the skin's own
 * ink, so a skin is only a palette; the CRT effects (scanlines, glow, the rolling band, the boot line) are switched by the Computer's
 * settings through {@link #effects}, set by the screen before it draws.
 */
final class TerminalLook {

    private static boolean scanlines = true, glow = true, band = true, boot = true;

    private TerminalLook() {
    }

    /** The Computer's CRT settings, in force for every terminal skin until set again. Client render thread only. */
    static void effects(boolean scanlines, boolean glow, boolean band, boolean boot) {
        TerminalLook.scanlines = scanlines;
        TerminalLook.glow = glow;
        TerminalLook.band = band;
        TerminalLook.boot = boot;
    }

    /** The colour scaled toward black by {@code f}, opaque. */
    static int shade(int rgb, float f) {
        int r = (int) (((rgb >> 16) & 0xFF) * f), g = (int) (((rgb >> 8) & 0xFF) * f), b = (int) ((rgb & 0xFF) * f);
        return 0xFF000000 | Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, b);
    }

    private static int tint(GuiSkin skin, int alpha) {
        return alpha << 24 | (shade(skin.ink(), 0.5f) & 0xFFFFFF);
    }

    static void panel(GuiSkin skin, GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x70000000);
        g.fill(x, y, x + w, y + h, shade(skin.dim(), 0.1f));
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF0B0F0C);
        // the glass, a little brighter at the middle than the rim
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, shade(skin.ink(), 0.03f));
        if (glow) {
            g.fillGradient(x + 3, y + 3, x + w - 3, y + 3 + (h - 6) / 2, tint(skin, 0x00), tint(skin, 0x12));
            g.fillGradient(x + 3, y + 3 + (h - 6) / 2, x + w - 3, y + h - 3, tint(skin, 0x12), tint(skin, 0x00));
        }
        if (scanlines)
            for (int yy = y + 4; yy < y + h - 3; yy += 3)
                g.fill(x + 3, yy, x + w - 3, yy + 1, 0x30000000);
        // one slow bright band rolling down the glass
        if (band && !ClientSettings.reduceMotion()) {
            float phase = (GuiStyle.time() * 0.45f) % (h + 40);
            int by = y + (int) phase - 20;
            for (int i = 0; i < 14; i++) {
                int yy = by + i;
                if (yy > y + 3 && yy < y + h - 3)
                    g.fill(x + 3, yy, x + w - 3, yy + 1, tint(skin, 0x0A - i / 2));
            }
        }
        g.renderOutline(x + 2, y + 2, w - 4, h - 4, 0x70000000 | shade(skin.dim(), 0.7f) & 0xFFFFFF);
        // corner brackets
        int bracket = 0xB0000000 | shade(skin.dim(), 0.85f) & 0xFFFFFF;
        for (int[] c : new int[][] {{x + 6, y + 6, 1, 1}, {x + w - 6, y + 6, -1, 1}, {x + 6, y + h - 6, 1, -1}, {x + w - 6, y + h - 6, -1, -1}}) {
            g.fill(Math.min(c[0], c[0] + 6 * c[2]), c[1], Math.max(c[0], c[0] + 6 * c[2]), c[1] + 1, bracket);
            g.fill(c[0], Math.min(c[1], c[1] + 6 * c[3]), c[0] + 1, Math.max(c[1], c[1] + 6 * c[3]), bracket);
        }
    }

    static void slot(GuiSkin skin, GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF010402);
        g.renderOutline(x - 1, y - 1, 18, 18, shade(skin.dim(), 0.7f));
        int lit = shade(skin.ink(), 0.8f);
        for (int[] c : new int[][] {{x - 1, y - 1, 1, 1}, {x + 16, y - 1, -1, 1}, {x - 1, y + 16, 1, -1}, {x + 16, y + 16, -1, -1}}) {
            g.fill(c[0], c[1], c[0] + c[2] * 4 + (c[2] < 0 ? 1 : 0), c[1] + 1, lit);
            g.fill(c[0], c[1], c[0] + 1, c[1] + c[3] * 4 + (c[3] < 0 ? 1 : 0), lit);
        }
    }

    static void button(GuiSkin skin, GuiGraphics g, int x, int y, int w, int h, Component label, float glowAmount, boolean down,
                       boolean active, boolean primary) {
        int line, text, fill;
        if (!active) {
            line = shade(skin.dim(), 0.45f);
            text = shade(skin.dim(), 0.6f);
            fill = 0xFF020804;
        } else {
            // inverted when the pointer is on it: a slab of the phosphor, black text
            fill = GuiSkin.lerp(primary ? shade(skin.ink(), 0.1f) : 0xFF020804, skin.ink(), glowAmount);
            line = primary ? shade(skin.ink(), 0.95f) : skin.dim();
            text = GuiSkin.lerp(primary ? skin.accent() : skin.ink(), 0xFF020804, glowAmount);
        }
        if (down)
            fill = GuiSkin.lerp(fill, 0xFF020804, 0.3f);
        g.fill(x, y, x + w, y + h, fill);
        g.renderOutline(x, y, w, h, line);
        if (primary && active)
            g.renderOutline(x + 2, y + 2, w - 4, h - 4, 0x60000000 | skin.dim() & 0xFFFFFF);
        var font = Minecraft.getInstance().font;
        int o = down ? 1 : 0;
        g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, false);
    }

    private static int gap(int h, float t) {
        return Math.round(Math.max(1f, (h / 2f + 4) * t));
    }

    static void beginReveal(GuiSkin skin, GuiGraphics g, int x, int y, int w, int h, float t) {
        if (boot && t < 1)
            g.enableScissor(x - 4, y + h / 2 - gap(h, t), x + w + 8, y + h / 2 + gap(h, t));
    }

    static void endReveal(GuiSkin skin, GuiGraphics g, int x, int y, int w, int h, float t) {
        if (!boot || t >= 1)
            return;
        g.flush();
        g.disableScissor();
        // the bright line the picture collapses to at the centre, fading out as it opens
        int mid = y + h / 2, gap = gap(h, t);
        int colour = (int) (0xFF * (1f - t)) << 24 | shade(skin.ink(), 0.95f) & 0xFFFFFF;
        g.fill(x, mid - gap, x + w, mid - gap + 1, colour);
        g.fill(x, mid + gap - 1, x + w, mid + gap, colour);
    }
}
