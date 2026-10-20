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

import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * What a screen is made of. Each skin is a material, and it looks and sounds like one: the era of
 * the block picks it (a forge-era vessel is iron, a drafting stand is paper). Everything is drawn
 * with fills, so there is nothing to license and every screen on a skin moves when this file does.
 * {@link #STEEL} is the old house look, kept for screens not yet given a material.
 */
public enum GuiSkin {

    STEEL(GuiStyle.TEXT, 0xFF606060, 0xFF1C5FA8, 0xFFAA0000) {
        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            GuiStyle.panel(g, x, y, w, h);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            GuiStyle.slot(g, x, y);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int top = lerp(0xFFD8D8D8, 0xFFFFFFFF, glow), mid = lerp(0xFFB4B4B4, 0xFFD6E4FF, glow);
            if (!active) {
                top = 0xFF9A9A9A;
                mid = 0xFF8B8B8B;
            }
            g.fill(x, y, x + w, y + h, GuiStyle.EDGE);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, mid);
            if (down) {
                g.fill(x + 1, y + 1, x + w - 1, y + 2, GuiStyle.SHADE);
                g.fill(x + 1, y + 1, x + 2, y + h - 1, GuiStyle.SHADE);
            } else {
                g.fill(x + 1, y + 1, x + w - 1, y + 2, top);
                g.fill(x + 1, y + 1, x + 2, y + h - 1, top);
                g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, GuiStyle.SHADE);
                g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, GuiStyle.SHADE);
            }
            int color = active ? (glow > 0.5f ? 0xFF000000 : 0xFF202020) : 0xFF606060;
            g.drawCenteredString(Minecraft.getInstance().font, label, x + w / 2, y + (h - 8) / 2 + (down ? 1 : 0), color);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.UI_BUTTON_CLICK.value();
        }
    },

    /**
     * Cyanotype on a drafting stand: white ink on blue paper, a grid, a ruled double border. Buttons
     * are drawn boxes that firm up under the pointer; the primary one is a red rubber stamp. The
     * sheet unrolls from the left when the screen opens and rolls back up when it closes.
     */
    BLUEPRINT(0xFFE8F1FF, 0xFF8FB3DA, 0xFFFFE070, 0xFFFF7A6E) {
        private static final int PAPER = 0xFF1B4C86, STAMP = 0xFFE85A4F;
        /** Fibres in the paper, fixed so the sheet does not crawl. Fractions of the sheet, x then y. */
        private static final float[] SPECKLE = speckle();

        private static float[] speckle() {
            Random r = new Random(0x5EED);
            float[] out = new float[2 * 180];
            for (int i = 0; i < out.length; i++)
                out[i] = r.nextFloat();
            return out;
        }

        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x50000000); // the sheet's shadow on the stand
            g.fill(x, y, x + w, y + h, PAPER);
            g.fillGradient(x, y, x + w, y + 48, 0x16FFFFFF, 0x00FFFFFF);
            g.fillGradient(x, y + h - 48, x + w, y + h, 0x00000000, 0x2A000000);
            for (int i = 0; i < 4; i++) { // the edges darken where the paper curls
                int a = (0x1C - i * 6) << 24;
                g.fill(x + i * 2, y, x + i * 2 + 2, y + h, a);
                g.fill(x + w - i * 2 - 2, y, x + w - i * 2, y + h, a);
            }
            for (int gx = 6; gx < w; gx += 6)
                g.fill(x + gx, y, x + gx + 1, y + h, gx % 24 == 0 ? 0x1CFFFFFF : 0x0CFFFFFF);
            for (int gy = 6; gy < h; gy += 6)
                g.fill(x, y + gy, x + w, y + gy + 1, gy % 24 == 0 ? 0x1CFFFFFF : 0x0CFFFFFF);
            for (int i = 0; i < SPECKLE.length; i += 2) {
                int sx = x + (int) (SPECKLE[i] * w), sy = y + (int) (SPECKLE[i + 1] * h);
                g.fill(sx, sy, sx + 1, sy + 1, (i & 2) == 0 ? 0x14FFFFFF : 0x18000000);
            }
            g.renderOutline(x + 3, y + 3, w - 6, h - 6, 0xB0E8F1FF);
            g.renderOutline(x + 5, y + 5, w - 10, h - 10, 0x40E8F1FF);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0x30000000);
            g.renderOutline(x - 1, y - 1, 18, 18, 0x90E8F1FF);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int ink = !active ? 0x60E8F1FF : primary ? lerp(0xC0000000 | (STAMP & 0xFFFFFF), 0xFFFF8A80, glow)
                    : lerp(dim(), ink(), glow);
            int o = down ? 1 : 0;
            if (down)
                g.fill(x, y, x + w, y + h, primary ? 0x30E85A4F : 0x30FFFFFF);
            else if (glow > 0)
                g.fill(x, y, x + w, y + h, ((int) (glow * 0x1C) << 24) | (primary ? STAMP & 0xFFFFFF : 0xFFFFFF));
            g.renderOutline(x, y, w, h, ink);
            if (primary)
                g.renderOutline(x + 2, y + 2, w - 4, h - 4, ink);
            // Drafting ticks run out past the corners as the pointer arrives.
            int t = Math.round(glow * 2);
            if (t > 0) {
                g.fill(x - 1 - t, y, x, y + 1, ink);
                g.fill(x + w, y + h - 1, x + w + 1 + t, y + h, ink);
                g.fill(x, y - 1 - t, x + 1, y, ink);
                g.fill(x + w - 1, y + h, x + w, y + h + 1 + t, ink);
            }
            var font = Minecraft.getInstance().font;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, ink, false);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT;
        }

        @Override
        public SoundEvent open() {
            return FSounds.UI_UNROLL.get();
        }

        @Override
        public SoundEvent close() {
            return FSounds.UI_ROLL.get();
        }

        @Override
        public void beginReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            if (t < 1)
                g.enableScissor(x - 4, y - 4, x + Math.round(w * t), y + h + 8);
        }

        @Override
        public void endReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            if (t >= 1)
                return;
            g.flush();
            g.disableScissor();
            // The rest of the sheet, still rolled, riding the edge.
            int rx = x + Math.round(w * t);
            g.fill(rx - 1, y - 3, rx + 7, y + h + 3, 0xFF15406F);
            g.fill(rx, y - 2, rx + 6, y + h + 2, 0xFF2A64A8);
            g.fill(rx + 1, y - 2, rx + 3, y + h + 2, 0xFF6A9CD8);
            g.fill(rx + 5, y - 2, rx + 6, y + h + 2, 0xFF143A66);
        }
    };

    private final int ink, dim, accent, warn;

    GuiSkin(int ink, int dim, int accent, int warn) {
        this.ink = ink;
        this.dim = dim;
        this.accent = accent;
        this.warn = warn;
    }

    /** Body text and lines. */
    public int ink() {
        return ink;
    }

    /** Labels and secondary lines. */
    public int dim() {
        return dim;
    }

    /** The thing selected, a value just changed. */
    public int accent() {
        return accent;
    }

    /** A figure past its limit. */
    public int warn() {
        return warn;
    }

    public abstract void panel(GuiGraphics g, int x, int y, int w, int h);

    /** A socket around the 16x16 item at (x, y). */
    public abstract void slot(GuiGraphics g, int x, int y);

    abstract void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down,
                         boolean active, boolean primary);

    /** What pressing one of this skin's buttons sounds like, unless the button says otherwise. */
    public abstract SoundEvent click();

    /** Played as a screen opens; null for none. */
    public SoundEvent open() {
        return null;
    }

    public SoundEvent close() {
        return null;
    }

    /** Clip the screen to how far it has opened, {@code t} 0 shut to 1 open; {@link #endReveal} after drawing it. */
    public void beginReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
    }

    public void endReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
    }

    /** A screen sound, a touch off-pitch each time so repeats do not sound stamped out. */
    public static void play(SoundEvent sound, float pitch) {
        if (sound != null)
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound,
                    pitch * (0.96f + 0.08f * ThreadLocalRandom.current().nextFloat())));
    }

    static int lerp(int a, int b, float t) {
        return FastColor.ARGB32.lerp(t, a, b);
    }
}
