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

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * The smithing table's background: a procedural fire behind the bench, driven by the heat of the stock. Layered columns whose heights come
 * from a few summed sines (a cheap stand-in for noise), three layers deep -- a red-orange body, an orange middle and a pale core that
 * only appears once the stock is properly hot -- so the fire is dull and low for cold stock and tall, bright and whiter as it heats.
 * Code-drawn fills and gradients, no texture and no shader; nothing is allocated. A real shader is the open question in
 * {@code docs/gui_bench.md}: this is the cheap first version.
 */
final class BenchFire {

    private static final int COLUMN = 6;

    private BenchFire() { }

    /** A fire colour for heat 0..1: dull red, orange, yellow, then white-hot. */
    static int colour(float heat) {
        int r = 0x70 + (int) (0x8F * Mth.clamp(heat * 2f, 0f, 1f));
        int gr = (int) (0xB0 * Mth.clamp(heat * 1.5f - 0.15f, 0f, 1f));
        int b = (int) (0xC0 * Mth.clamp(heat * 2.4f - 1.3f, 0f, 1f));
        return r << 16 | gr << 8 | b;
    }

    /**
     * Draw the fire in the rectangle (x, y, w, h), burning up from its bottom edge. {@code heat} is 0..1 of the stock's working range; a cold
     * bench still keeps a low bed of embers.
     */
    static void draw(GuiGraphics g, int x, int y, int w, int h, float heat) {
        boolean still = ClientSettings.reduceMotion();
        float t = still ? 0f : GuiStyle.time();
        float h0 = Mth.clamp(heat, 0f, 1f);
        // The glow cast on the whole bench.
        int glow = (int) (0x10 + 0x50 * h0);
        g.fillGradient(x, y + h / 3, x + w, y + h, 0, glow << 24 | colour(Math.max(0.15f, h0)));
        int cols = w / COLUMN + 1;
        for (int layer = 0; layer < 3; layer++) {
            // Each layer is a little shorter and hotter than the one behind it; the pale core waits for real heat.
            float reach = (0.16f + 0.5f * h0) * (1f - layer * 0.28f);
            float warmth = Mth.clamp(h0 * 1.15f + layer * 0.22f - 0.1f, 0.05f, 1f);
            if (layer == 2 && h0 < 0.45f)
                continue;
            int alpha = layer == 0 ? 0x70 : layer == 1 ? 0x80 : 0x90;
            for (int i = 0; i < cols; i++) {
                float n = 0.5f + 0.26f * Mth.sin(i * 0.9f + t * 0.31f + layer) + 0.18f * Mth.sin(i * 0.37f - t * 0.22f + 1.3f * layer)
                        + 0.08f * Mth.sin(i * 2.3f + t * 0.8f);
                // Tongues lean toward the middle of the bench, where the stock lies.
                float mid = 1f - Math.abs(i - cols / 2f) / (cols / 2f) * 0.45f;
                int fh = Math.max(2, (int) (h * reach * n * mid));
                int x0 = x + i * COLUMN, x1 = Math.min(x + w, x0 + COLUMN - 1);
                g.fillGradient(x0, y + h - fh, x1, y + h, 0, alpha << 24 | colour(warmth));
            }
        }
        // Sparks lifting off a hot bench.
        if (!still && h0 > 0.2f) {
            int n = 4 + (int) (14 * h0);
            for (int i = 0; i < n; i++) {
                int s = (i + 1) * 0x9E3779B1;
                s ^= s >>> 15;
                int period = 40 + (s >>> 4 & 31);
                float f = ((t + (s & 0xFF)) % period) / period;
                int a = (int) (0xD0 * Mth.sin(f * Mth.PI));
                int px = x + (s >>> 8 & 0xFF) * w / 256 + Math.round(Mth.sin(f * 6.28f + i) * 4);
                int py = y + h - Math.round(f * h * (0.4f + 0.5f * h0));
                if (a > 8)
                    g.fill(px, py, px + 1 + (i & 1), py + 1 + (i & 1), a << 24 | 0xFFC060);
            }
        }
    }
}
