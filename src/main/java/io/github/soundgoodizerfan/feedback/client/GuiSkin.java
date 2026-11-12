// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * What a screen is made of. Each skin is a material, and it looks and sounds like one: the era of
 * the block picks it (a forge-era vessel is iron, a drafting stand is paper). Everything is drawn
 * with fills, so there is nothing to license and every screen on a skin moves when this file does.
 */
public enum GuiSkin {

    /**
     * Cyanotype on a drafting stand: white ink on blue paper, a grid, a ruled double border. Buttons
     * are drawn boxes that firm up under the pointer; the primary one is a red rubber stamp. The
     * sheet unrolls from the left when the screen opens and rolls back up when it closes.
     */
    BLUEPRINT(0xFFE8F1FF, 0xFF8FB3DA, 0xFFFFE070, 0xFFFF7A6E, 0x38000000, 0x90E8F1FF, 0x28FFFFFF) {
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
    },

    /**
     * The forge's: blackened iron lit only by its fire. Soot-dark at the top, a coal glow rising from the bottom edge,
     * dull scale on the plate, edges that catch the firelight. Buttons are cold iron that warm under the pointer; the
     * primary one is metal heated to glowing. The screen opens like a hatch, two iron shutters parting from the middle.
     */
    FORGE(0xFFE6D2B4, 0xFF9A8470, 0xFFFF9A3C, 0xFFFF5A3C, 0xFF0A0807, 0xFF5A2E18, 0xFF2A201C) {
        private static final int IRON = 0xFF1C1816, EDGE = 0xFF070504, GLOW = 0xFFFF6A1E;
        /** Scale pitting on the plate, fixed: x, y, size, as fractions of the plate. */
        private static final float[] SCALE = scale();

        private static float[] scale() {
            Random r = new Random(0xF0E6E);
            float[] out = new float[3 * 140];
            for (int i = 0; i < out.length; i++)
                out[i] = r.nextFloat();
            return out;
        }

        /** Blackened plate: iron, firelight rising from the bottom, a warm lip on the lower edges. */
        private static void plate(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x, y, x + w, y + h, EDGE);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, IRON);
            g.fillGradient(x + 1, y + 1, x + w - 1, y + Math.min(h - 1, 30), 0x30000000, 0x00000000); // soot
            g.fillGradient(x + 1, y + Math.max(1, h / 2), x + w - 1, y + h - 1, 0x00FF5A14, 0x40FF5A14);
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x80FF7A2A);
            g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x14FFFFFF);
        }

        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x70000000);
            plate(g, x, y, w, h);
            for (int i = 0; i < SCALE.length; i += 3) {
                int sx = x + 3 + (int) (SCALE[i] * (w - 6)), sy = y + 3 + (int) (SCALE[i + 1] * (h - 6));
                float heat = (sy - y) / (float) h; // pits catch more light low down
                int sz = SCALE[i + 2] > 0.85f ? 2 : 1;
                g.fill(sx, sy, sx + sz, sy + sz, (i & 1) == 0 ? 0x30000000 : (int) (heat * 0x1C) << 24 | 0xFF8A3C);
            }
            g.renderOutline(x + 4, y + 4, w - 8, h - 8, 0x50000000);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0E0B0A);
            g.fill(x - 1, y - 1, x + 17, y, 0xFF000000);
            g.fill(x - 1, y - 1, x, y + 17, 0xFF000000);
            g.fill(x - 1, y + 16, x + 17, y + 17, 0x40FF7A2A);
            g.fill(x + 16, y - 1, x + 17, y + 17, 0x30FF7A2A);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int top, bottom, text;
            if (!active) {
                top = 0xFF2A2523;
                bottom = 0xFF1E1A18;
                text = 0xFF6A625C;
            } else if (primary) { // heated through
                top = lerp(0xFFC04A1A, 0xFFF07A2A, glow);
                bottom = lerp(0xFF7A2810, 0xFFA83E16, glow);
                text = 0xFFFFEBCC;
            } else { // cold iron, warming
                top = lerp(0xFF3A322E, 0xFF5A3A28, glow);
                bottom = lerp(0xFF221C1A, 0xFF3A2218, glow);
                text = lerp(0xFFB09A84, 0xFFFFD8A8, glow);
            }
            if (down) {
                int t = top;
                top = bottom;
                bottom = t;
            }
            g.fill(x, y, x + w, y + h, EDGE);
            g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, top, bottom);
            if (!down && active) {
                g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x28FFFFFF);
                g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, (int) ((0x30 + glow * 0x50)) << 24 | 0xFF7A2A);
            }
            if (primary && active && glow > 0)
                g.renderOutline(x - 1, y - 1, w + 2, h + 2, (int) (glow * 0x80) << 24 | 0xFF9A3C);
            var font = Minecraft.getInstance().font;
            int o = down ? 1 : 0;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, false);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.STONE_BUTTON_CLICK_ON;
        }

        @Override
        public SoundEvent open() {
            return FSounds.UI_HATCH_OPEN.get();
        }

        @Override
        public SoundEvent close() {
            return FSounds.UI_HATCH_CLOSE.get();
        }

        private static int gap(int h, float t) {
            return Math.round((h / 2f + 6) * t);
        }

        @Override
        public void beginReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            if (t < 1)
                g.enableScissor(x - 4, y + h / 2 - gap(h, t), x + w + 8, y + h / 2 + gap(h, t));
        }

        @Override
        public void endReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            if (t >= 1)
                return;
            g.flush();
            g.disableScissor();
            int mid = y + h / 2, gap = gap(h, t);
            // The shutters, sliding back into the frame as the hatch opens.
            int top = mid - gap - y, bottom = y + h - (mid + gap);
            if (top > 0) {
                plate(g, x, y, w, top);
                g.fill(x + 1, y + top - 2, x + w - 1, y + top, 0xFF5A2E18);
            }
            if (bottom > 0) {
                plate(g, x, mid + gap, w, bottom);
                g.fill(x + 1, mid + gap, x + w - 1, mid + gap + 2, 0xFF5A2E18);
            }
        }
    },

    /**
     * Atomic-age laboratory enamel: a dusty sage hammered-enamel cabinet face, never white, stencilled in dark ink. Cut corners, a cream
     * and rust double stripe across the top between a starburst and a cream pilot lamp that breathes, chrome rivets at the foot, a
     * printed scale ruler along the bottom edge. Buttons are putty push-plates; the primary one is rust enamel. The calm light sibling
     * of {@link #CONSOLE}. Fills and a fixed-seed dimple table, so nothing crawls but the lamp.
     */
    ENAMEL(0xFF18211B, 0xFF3F4C43, 0xFF9A3A12, 0xFFA81C1C, 0xFF7F8B7C, 0xFF3B463E, 0xFFA7B1A1) {
        private static final int FACE = 0xFF97A392, EDGE = 0xFF27302A, CREAM = 0xFFCFC9A8, RUST = 0xFFA8481A, CHROME = 0xFFC2C8BE;
        private static final float[] DIMPLE = dimples();

        private static float[] dimples() {
            Random r = new Random(0xE4A3);
            float[] out = new float[2 * 260];
            for (int i = 0; i < out.length; i++)
                out[i] = r.nextFloat();
            return out;
        }

        private static void star(GuiGraphics g, int cx, int cy, int c) {
            g.fill(cx - 3, cy, cx + 4, cy + 1, c);
            g.fill(cx, cy - 3, cx + 1, cy + 4, c);
            for (int k = 1; k <= 2; k++) {
                g.fill(cx - k, cy - k, cx - k + 1, cy - k + 1, c);
                g.fill(cx + k, cy - k, cx + k + 1, cy - k + 1, c);
                g.fill(cx - k, cy + k, cx - k + 1, cy + k + 1, c);
                g.fill(cx + k, cy + k, cx + k + 1, cy + k + 1, c);
            }
        }

        private static void rivet(GuiGraphics g, int x, int y) {
            g.fill(x + 1, y, x + 4, y + 5, CHROME);
            g.fill(x, y + 1, x + 5, y + 4, CHROME);
            g.fill(x + 1, y + 1, x + 2, y + 2, 0xFFF2F4EE);
            g.fill(x + 3, y + 3, x + 4, y + 4, 0xFF69706A);
        }

        /** A rectangle with its four corner pixels cut. */
        private static void cut(GuiGraphics g, int x, int y, int w, int h, int c) {
            g.fill(x + 2, y, x + w - 2, y + h, c);
            g.fill(x, y + 2, x + w, y + h - 2, c);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, c);
        }

        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x50000000);
            cut(g, x, y, w, h, EDGE);
            cut(g, x + 1, y + 1, w - 2, h - 2, FACE);
            g.fillGradient(x + 2, y + 2, x + w - 2, y + Math.min(h - 2, 40), 0x18FFFFFF, 0x00FFFFFF);
            for (int i = 0; i < DIMPLE.length; i += 2) {
                int sx = x + 3 + (int) (DIMPLE[i] * (w - 7)), sy = y + 3 + (int) (DIMPLE[i + 1] * (h - 7));
                g.fill(sx, sy, sx + 2, sy + 1, (i & 2) == 0 ? 0x16FFFFFF : 0x14000000);
            }
            if (!inTab)
                ornaments(g, x, y, w, h);
            g.fill(x + 2, y + 1, x + w - 2, y + 2, 0x30FFFFFF);
        }

        private void ornaments(GuiGraphics g, int x, int y, int w, int h) {
            // the double stripe, between its ornaments
            g.fill(x + 14, y + 3, x + w - 14, y + 4, CREAM);
            g.fill(x + 14, y + 5, x + w - 14, y + 7, RUST);
            star(g, x + 8, y + 5, RUST);
            // pilot lamp
            float breath = ClientSettings.reduceMotion() ? 0.8f : 0.5f + 0.5f * net.minecraft.util.Mth.sin(GuiStyle.time() * 0.08f);
            int lx = x + w - 9, ly = y + 5;
            g.fill(lx - 3, ly - 2, lx + 4, ly + 3, 0xFF39423B);
            g.fill(lx - 2, ly - 3, lx + 3, ly + 4, 0xFF39423B);
            g.fill(lx - 1, ly - 1, lx + 2, ly + 2, lerp(0xFF7A6A3A, 0xFFF4E2A6, breath));
            g.renderOutline(lx - 4, ly - 4, 9, 9, (int) (breath * 0x28) << 24 | 0xF4E2A6);
            // the scale ruler printed along the foot
            for (int i = 12; i < w - 12; i += 4) {
                int len = ((i - 12) / 4) % 5 == 0 ? 3 : 2;
                g.fill(x + i, y + h - 3 - len, x + i + 1, y + h - 3, 0x50141C17);
            }
            rivet(g, x + 4, y + h - 9);
            rivet(g, x + w - 9, y + h - 9);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF5E6A5C);
            g.fill(x - 1, y - 1, x + 17, y, 0xFF3C463E);
            g.fill(x - 1, y - 1, x, y + 17, 0xFF3C463E);
            g.fill(x - 1, y + 16, x + 17, y + 17, 0x60FFFFFF);
            g.fill(x + 16, y - 1, x + 17, y + 17, 0x60FFFFFF);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int top, bottom, text;
            if (!active) {
                top = 0xFF8A9486;
                bottom = 0xFF7E887B;
                text = 0xFF5E695F;
            } else if (primary) {
                top = lerp(0xFFB4501C, 0xFFCC6A2E, glow);
                bottom = lerp(0xFF8A3A10, 0xFFA04C1C, glow);
                text = 0xFFF6E6C4;
            } else {
                top = lerp(0xFFBEC6B2, 0xFFD2D8C6, glow);
                bottom = lerp(0xFFA2AC97, 0xFFB4BEA8, glow);
                text = ink();
            }
            if (down) {
                top = lerp(top, 0xFF000000, 0.2f);
                bottom = lerp(bottom, 0xFF000000, 0.2f);
            }
            cut(g, x, y, w, h, EDGE);
            g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, top, bottom);
            if (!down)
                g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x40FFFFFF);
            var font = Minecraft.getInstance().font;
            int o = down ? 1 : 0;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, false);
        }

        @Override
        public SoundEvent click() {
            return FSounds.UI_DETENT.get();
        }

        @Override
        public SoundEvent open() {
            return FSounds.UI_TOGGLE.get();
        }

        @Override
        public SoundEvent close() {
            return FSounds.UI_TOGGLE.get();
        }
    },

    /**
     * The same atomic-age laboratory after dark: a charcoal-teal enamel console with brass corner brackets and rails, a row of
     * indicator lamps along the foot (one breathing, the rest steady), and a slow glint of light drifting across the glass. Buttons are
     * brass-bezelled; the primary one is a lit amber lamp. Cream ink. The machine-room sibling of {@link #ENAMEL}.
     */
    CONSOLE(0xFFE8DEC0, 0xFF8EA098, 0xFFFFB84A, 0xFFFF5A3C, 0xFF0C1719, 0xFF7A6330, 0xFF2A3C3F) {
        private static final int FACE = 0xFF1B2A2D, EDGE = 0xFF080F11, BRASS = 0xFFB08A3C, BRASS_LO = 0xFF6E5624;

        private static void bracket(GuiGraphics g, int x, int y, int sx, int sy) {
            // an L of brass, 8 long, 2 thick, opening away from the corner (sx, sy are +1 or -1)
            int x0 = sx > 0 ? x : x - 8, y0 = sy > 0 ? y : y - 8;
            g.fill(x0, sy > 0 ? y : y - 2, x0 + 8, sy > 0 ? y + 2 : y, BRASS);
            g.fill(sx > 0 ? x : x - 2, y0, sx > 0 ? x + 2 : x, y0 + 8, BRASS);
        }

        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x60000000);
            g.fill(x, y, x + w, y + h, EDGE);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, FACE);
            g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, 0x14FFFFFF, 0x20000000);
            if (!inTab)
                ornaments(g, x, y, w, h);
            g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x20FFFFFF);
        }

        private void ornaments(GuiGraphics g, int x, int y, int w, int h) {
            // brass rails top and bottom, brackets in the corners
            g.fill(x + 12, y + 3, x + w - 12, y + 4, BRASS);
            g.fill(x + 12, y + 4, x + w - 12, y + 5, BRASS_LO);
            bracket(g, x + 3, y + 3, 1, 1);
            bracket(g, x + w - 3, y + 3, -1, 1);
            bracket(g, x + 3, y + h - 3, 1, -1);
            bracket(g, x + w - 3, y + h - 3, -1, -1);
            // the glint, a soft diagonal band: a quick pass every few seconds, nothing in between
            float t = ClientSettings.reduceMotion() ? 2f : (GuiStyle.time() % 100f) / 24f;
            int centre = (int) (-40 + t * (w + 80));
            for (int j = 2; t <= 1f && j < h - 2; j += 2) {
                int a = x + centre + (int) (j * 0.7f), b = a + 14;
                int l = Math.max(x + 1, a), r = Math.min(x + w - 1, b);
                if (r > l)
                    g.fill(l, y + j, r, y + Math.min(j + 2, h - 1), 0x149EE8E0);
            }
            // indicator lamps along the foot
            float breath = ClientSettings.reduceMotion() ? 0.8f : 0.5f + 0.5f * net.minecraft.util.Mth.sin(GuiStyle.time() * 0.1f);
            for (int i = 0; i < 5; i++) {
                int lx = x + 14 + i * 7, ly = y + h - 7;
                int c = i == 2 ? lerp(0xFF6A4A18, 0xFFFFB84A, breath) : i == 0 ? 0xFFC8923A : 0xFF3A4A44;
                g.fill(lx - 1, ly - 1, lx + 4, ly + 4, EDGE);
                g.fill(lx, ly, lx + 3, ly + 3, c);
            }
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, BRASS_LO);
            g.fill(x, y, x + 16, y + 16, 0xFF0A1416);
            g.fill(x - 1, y - 1, x + 17, y, BRASS);
            g.fill(x - 1, y - 1, x, y + 17, BRASS);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int top, bottom, text;
            if (!active) {
                top = 0xFF1A2628;
                bottom = 0xFF141E20;
                text = 0xFF526058;
            } else if (primary) {
                top = lerp(0xFF8E5E16, 0xFFD08C26, glow);
                bottom = lerp(0xFF62400E, 0xFF9A6618, glow);
                text = 0xFFFFEFC8;
            } else {
                top = lerp(0xFF2E4448, 0xFF3C565A, glow);
                bottom = lerp(0xFF1E2E31, 0xFF2A3E42, glow);
                text = lerp(dim(), ink(), glow);
            }
            if (down) {
                top = lerp(top, 0xFF000000, 0.3f);
                bottom = lerp(bottom, 0xFF000000, 0.3f);
            }
            g.fill(x, y, x + w, y + h, active ? BRASS : BRASS_LO);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, EDGE);
            g.fillGradient(x + 2, y + 2, x + w - 2, y + h - 2, top, bottom);
            if (primary && active && glow > 0)
                g.renderOutline(x - 1, y - 1, w + 2, h + 2, (int) (glow * 0x70) << 24 | 0xFFB84A);
            var font = Minecraft.getInstance().font;
            int o = down ? 1 : 0;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, false);
        }

        @Override
        public SoundEvent click() {
            return FSounds.UI_DETENT.get();
        }

        @Override
        public SoundEvent open() {
            return FSounds.UI_TOGGLE.get();
        }

        @Override
        public SoundEvent close() {
            return FSounds.UI_TOGGLE.get();
        }
    },

    /**
     * The first hearth: cobblestone, dimmed and flattened under a smoke-grey wash so it reads as
     * old masonry and not as a block face. No trim, no hardware; buttons are cut slabs. It opens
     * and closes with no ceremony. Vanilla's cobblestone is drawn in place, not copied.
     */
    STONE(0xFFDCD6C8, 0xFF9C968A, 0xFFFFB05A, 0xFFFF6A50, 0xFF161411, 0x50FFFFFF, 0xFF3A3630) {
        private static final ResourceLocation COBBLE = ResourceLocation.withDefaultNamespace("textures/block/cobblestone.png");
        private static final int MORTAR = 0xFF14120F;

        /** Cobblestone tiled over a box, then washed toward grey so its contrast drops and it darkens. */
        private static void masonry(GuiGraphics g, int x, int y, int w, int h, int wash) {
            g.blit(COBBLE, x, y, 0, 0, w, h, 16, 16);
            g.fill(x, y, x + w, y + h, wash);
        }

        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x60000000);
            g.fill(x, y, x + w, y + h, MORTAR);
            masonry(g, x + 1, y + 1, w - 2, h - 2, 0xB0262320);
            g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x18FFFFFF);
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x50000000);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF161411);
            g.fill(x - 1, y - 1, x + 17, y, 0xFF000000);
            g.fill(x - 1, y - 1, x, y + 17, 0xFF000000);
            g.fill(x - 1, y + 16, x + 17, y + 17, 0x28FFFFFF);
            g.fill(x + 16, y - 1, x + 17, y + 17, 0x28FFFFFF);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int wash = !active ? 0xD0201E1B : lerp(primary ? 0x98402A1C : 0x98262320, 0x60302C28, glow);
            int text = !active ? 0xFF6E6A62 : primary ? lerp(0xFFE8C090, 0xFFFFD8A0, glow) : lerp(dim(), ink(), glow);
            g.fill(x, y, x + w, y + h, MORTAR);
            masonry(g, x + 1, y + 1, w - 2, h - 2, wash);
            if (!down && active) {
                g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x28FFFFFF);
                g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x40000000);
            }
            var font = Minecraft.getInstance().font;
            int o = down ? 1 : 0;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, false);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.STONE_BUTTON_CLICK_ON;
        }

        @Override
        public SoundEvent open() {
            return SoundEvents.STONE_PLACE;
        }

        @Override
        public SoundEvent close() {
            return SoundEvents.STONE_HIT;
        }
    },

    /**
     * The enchanting table's: deep indigo going to violet, a faint noise of glyphs, trimmed in gold leaf with
     * a diamond at each corner. Buttons are violet with a gold rim that glows under the pointer; the primary one
     * is gold leaf. Opens with the table's own sound and has no ceremony beyond it.
     */
    MYSTIC(0xFFE6DCFF, 0xFF9C8CC4, 0xFFF2CC5A, 0xFFFF6A8A, 0xFF0C0818, 0x90C9A44A, 0xFF2A1A52) {
        private static final int DEEP = 0xFF150C30, VIOLET = 0xFF2E1A66, GOLD = 0xFFC9A44A, GOLD_HI = 0xFFF6DD8A;
        /** Glyph fragments, fixed so nothing crawls: x, y as fractions of the panel, then a shape 0..2 (bar, stroke, tick). */
        private static final float[] GLYPHS = glyphs();

        private static float[] glyphs() {
            Random r = new Random(0x5E1D);
            float[] out = new float[3 * 150];
            for (int i = 0; i < out.length; i++)
                out[i] = r.nextFloat();
            return out;
        }

        /** Animation clock: game time plus partial tick, so it holds still when the game is paused; wall clock with no world. */
        private static float seconds() {
            var mc = Minecraft.getInstance();
            if (mc.level == null)
                return net.minecraft.Util.getMillis() / 1000f;
            return (mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false)) / 20f;
        }

        private static void diamond(GuiGraphics g, int cx, int cy) {
            g.fill(cx - 1, cy - 3, cx + 2, cy + 4, GOLD);
            g.fill(cx - 3, cy - 1, cx + 4, cy + 2, GOLD);
            g.fill(cx, cy - 1, cx + 1, cy + 2, GOLD_HI);
        }

        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x60000000);
            g.fillGradient(x, y, x + w, y + h, VIOLET, DEEP);
            float t = seconds();
            // Aurora: three soft violet bands drifting vertically, each two gradients (fade in, fade out), clipped to the panel.
            for (int k = 0; k < 3; k++) {
                float c = y + h * (0.5f + 0.4f * Mth.sin(t * (0.12f + 0.05f * k) + k * 2.1f));
                int half = 14 + 4 * k, top = Math.max(y + 1, (int) c - half), mid = Mth.clamp((int) c, y + 1, y + h - 1),
                        bot = Math.min(y + h - 1, (int) c + half);
                int col = k == 1 ? 0x6A3CC8 : 0x8A4CE0, a = (int) (0x14 + 0x0C * Mth.sin(t * 0.3f + k)) << 24;
                if (mid > top)
                    g.fillGradient(x + 1, top, x + w - 1, mid, col, a | col);
                if (bot > mid)
                    g.fillGradient(x + 1, mid, x + w - 1, bot, a | col, col);
            }
            // Glyphs drift upward slowly and twinkle; alpha fades to nothing at the wrap so none pop.
            int span = Math.max(1, h - 16);
            for (int i = 0; i < GLYPHS.length; i += 3) {
                float pos = (GLYPHS[i + 1] * span + t * 1.5f) % span;
                int gx = x + 8 + (int) (GLYPHS[i] * (w - 16)), gy = y + 8 + (int) pos;
                float env = Mth.sin(pos / span * Mth.PI) * (0.55f + 0.45f * Mth.sin(t * 0.9f + GLYPHS[i + 2] * 40f));
                int shape = (int) (GLYPHS[i + 2] * 3), c = (int) (0x1C * env) << 24 | 0xF2CC5A;
                if (shape == 0)
                    g.fill(gx, gy, gx + 4, gy + 1, c);
                else if (shape == 1) {
                    g.fill(gx, gy, gx + 1, gy + 4, c);
                    g.fill(gx, gy + 3, gx + 3, gy + 4, c);
                } else
                    g.fill(gx, gy, gx + 2, gy + 2, c);
            }
            // Rare sparkles: three slots, each a 7 s cycle with a short fade; spot is hashed from the cycle number.
            for (int k = 0; k < 3; k++) {
                float u = t / 7f + k / 3f;
                int cyc = (int) u;
                float ph = u - cyc;
                if (ph > 0.12f)
                    continue;
                float f = Mth.sin(ph / 0.12f * Mth.PI);
                int hsh = (cyc * 73856093) ^ (k * 19349663);
                int sx = x + 8 + Math.floorMod(hsh, Math.max(1, w - 16)), sy = y + 8 + Math.floorMod(hsh >> 9, span);
                int c = (int) (0x90 * f) << 24 | 0xFFF0B0, c2 = (int) (0x40 * f) << 24 | 0xFFF0B0;
                g.fill(sx, sy, sx + 1, sy + 1, c);
                g.fill(sx - 2, sy, sx + 3, sy + 1, c2);
                g.fill(sx, sy - 2, sx + 1, sy + 3, c2);
            }
            g.fillGradient(x, y, x + w, y + 30, 0x18FFFFFF, 0x00FFFFFF);
            g.renderOutline(x, y, w, h, 0xFF08041A);
            g.renderOutline(x + 3, y + 3, w - 6, h - 6, GOLD);
            g.renderOutline(x + 5, y + 5, w - 10, h - 10, 0x50C9A44A);
            diamond(g, x + 4, y + 4);
            diamond(g, x + w - 5, y + 4);
            diamond(g, x + 4, y + h - 5);
            diamond(g, x + w - 5, y + h - 5);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0C0818);
            g.renderOutline(x - 1, y - 1, 18, 18, 0xA0C9A44A);
            g.fill(x, y, x + 16, y + 1, 0x30000000);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int top, bottom, text, rim;
            if (!active) {
                top = 0xFF2A2240;
                bottom = 0xFF1C1630;
                text = 0xFF6E6490;
                rim = 0xFF4A4060;
            } else if (primary) {
                top = lerp(0xFFD8B050, 0xFFF6DD8A, glow);
                bottom = lerp(0xFF8E6E26, 0xFFB8903A, glow);
                text = 0xFF1E1230;
                rim = GOLD_HI;
            } else {
                top = lerp(0xFF4A2E94, 0xFF6440B8, glow);
                bottom = lerp(0xFF2A1A5C, 0xFF3A2482, glow);
                text = 0xFFEDE4FF;
                rim = lerp(GOLD, GOLD_HI, glow);
            }
            if (down) {
                int t = top;
                top = bottom;
                bottom = t;
            }
            if (glow > 0 && active) // the soft glow
                g.renderOutline(x - 1, y - 1, w + 2, h + 2, (int) (glow * 0x60) << 24 | 0xF2CC5A);
            g.fillGradient(x, y, x + w, y + h, top, bottom);
            g.renderOutline(x, y, w, h, rim);
            var font = Minecraft.getInstance().font;
            int o = down ? 1 : 0;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, false);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.AMETHYST_BLOCK_CHIME;
        }

        @Override
        public SoundEvent open() {
            return SoundEvents.ENCHANTMENT_TABLE_USE;
        }
    },

    /**
     * The look of vanilla's own inventory: light grey with a white bevel up-left and a dark one down-right, notched
     * black corners, sunken grey slots, plain grey buttons. For what sits over the world rather than in a block's
     * screen (the HUD), so it reads as the game's own and not a machine's. It opens with no ceremony.
     */
    VANILLA(0xFF404040, 0xFF707070, 0xFF2E7D32, 0xFFB02020, 0xFF8B8B8B, 0xFF373737, 0xFFC6C6C6) {
        private static final int BODY = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, SHADE = 0xFF555555, BLACK = 0xFF000000;

        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 2, y, x + w - 2, y + h, BLACK); // the notched silhouette
            g.fill(x, y + 2, x + w, y + h - 2, BLACK);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BLACK);
            g.fill(x + 3, y + 3, x + w - 3, y + h - 3, BODY);
            g.fill(x + 2, y + 2, x + w - 3, y + 4, LIGHT);
            g.fill(x + 2, y + 2, x + 4, y + h - 3, LIGHT);
            g.fill(x + 3, y + h - 4, x + w - 2, y + h - 2, SHADE);
            g.fill(x + w - 4, y + 3, x + w - 2, y + h - 2, SHADE);
            g.fill(x + w - 3, y + 2, x + w - 2, y + 3, BODY);
            g.fill(x + 2, y + h - 3, x + 3, y + h - 2, BODY);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
            g.fill(x, y, x + 18, y + 18, LIGHT);
            g.fill(x, y, x + 17, y + 17, 0xFF8B8B8B);
            g.fill(x - 1, y - 1, x + 16, y, 0xFF373737);
            g.fill(x - 1, y - 1, x, y + 16, 0xFF373737);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int face = !active ? 0xFF8B8B8B : lerp(0xFF6C6C6C, 0xFF7E7E9C, glow);
            g.fill(x, y, x + w, y + h, BLACK);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
            if (active && !down) {
                g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFFA8A8A8);
                g.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFFA8A8A8);
                g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF3C3C3C);
                g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFF3C3C3C);
            }
            var font = Minecraft.getInstance().font;
            int o = down ? 1 : 0;
            int text = !active ? 0xFFA0A0A0 : primary ? 0xFFFFFFA0 : 0xFFFFFFFF;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, true);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.UI_BUTTON_CLICK.value();
        }
    },

    /**
     * The kitchen's: vanilla's bricks drawn in place under a warm wash (as STONE does cobblestone), a calcite-cream
     * trim round the panel and its sunken areas, brick buttons with the primary one a cream calcite slab. Opens and closes with calcite's
     * own sounds and no ceremony. Sunken areas stay dark so the cream text on them reads; the sockets are calcite.
     */
    BRICK(0xFFF4EAD2, 0xFFD2B79A, 0xFFFFE08A, 0xFFFF4A30, 0xFF2B1511, 0xFFE8DCC4, 0xFF8A4632) {
        private static final ResourceLocation BRICKS = ResourceLocation.withDefaultNamespace("textures/block/bricks.png");
        private static final int CALCITE = 0xFFE8DCC4, SOCKET = 0xFFD6CBB4;

        /** Vanilla's bricks tiled over a box, then washed so the text on it reads. */
        private static void masonry(GuiGraphics g, int x, int y, int w, int h, int wash) {
            g.blit(BRICKS, x, y, 0, 0, w, h, 16, 16);
            g.fill(x, y, x + w, y + h, wash);
        }

        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x60000000);
            g.fill(x, y, x + w, y + h, 0xFF2B1511);
            masonry(g, x + 1, y + 1, w - 2, h - 2, 0x90200E0A);
            g.fillGradient(x + 1, y + 1, x + w - 1, y + Math.min(h - 1, 40), 0x20FFFFFF, 0x00FFFFFF);
            g.fillGradient(x + 1, y + h - 40, x + w - 1, y + h - 1, 0x00000000, 0x40000000);
            g.renderOutline(x + 3, y + 3, w - 6, h - 6, CALCITE);
            g.renderOutline(x + 4, y + 4, w - 8, h - 8, 0x50000000);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, SOCKET);
            g.fill(x - 1, y - 1, x + 17, y, 0xFF5A3A2C);
            g.fill(x - 1, y - 1, x, y + 17, 0xFF5A3A2C);
            g.fill(x - 1, y + 16, x + 17, y + 17, 0xFFFFF8E8);
            g.fill(x + 16, y - 1, x + 17, y + 17, 0xFFFFF8E8);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int text;
            g.fill(x, y, x + w, y + h, 0xFF2B1511);
            if (primary && active) {
                int top = lerp(0xFFE6DAC0, 0xFFFFF4DC, glow), bottom = lerp(0xFFB8AB92, 0xFFD4C7AC, glow);
                g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, down ? bottom : top, down ? top : bottom);
                text = 0xFF3A1D16;
            } else {
                masonry(g, x + 1, y + 1, w - 2, h - 2, !active ? 0xC0201210 : lerp(0x90301410, 0x50301410, glow));
                text = !active ? 0xFF8E7A6C : ink();
            }
            if (!down && active)
                g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x50FFE8D0);
            var font = Minecraft.getInstance().font;
            int o = down ? 1 : 0;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, false);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.CALCITE_HIT;
        }

        @Override
        public SoundEvent open() {
            return SoundEvents.CALCITE_PLACE;
        }

        @Override
        public SoundEvent close() {
            return SoundEvents.CALCITE_HIT;
        }
    },

    /**
     * The woodworking table's: vanilla's oak planks drawn in place under a dark wash, a pale-grain trim round the panel,
     * sockets cut into the board, buttons of darker board with the primary one a bright cut face. Wood's own sounds, no ceremony.
     */
    WOOD(0xFFF2E4C8, 0xFFC4A878, 0xFFFFD27A, 0xFFFF6A50, 0xFF1F160D, 0xFFB08850, 0xFF5A4126) {
        private static final ResourceLocation PLANKS = ResourceLocation.withDefaultNamespace("textures/block/oak_planks.png");
        private static final int EDGE = 0xFF1F160D;

        private static void board(GuiGraphics g, int x, int y, int w, int h, int wash) {
            g.blit(PLANKS, x, y, 0, 0, w, h, 16, 16);
            g.fill(x, y, x + w, y + h, wash);
        }

        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x60000000);
            g.fill(x, y, x + w, y + h, EDGE);
            board(g, x + 1, y + 1, w - 2, h - 2, 0xA0301E0E);
            g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x30FFFFFF);
            g.renderOutline(x + 3, y + 3, w - 6, h - 6, 0x80B08850);
            g.renderOutline(x + 4, y + 4, w - 8, h - 8, 0x50000000);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF3A2814);
            g.fill(x - 1, y - 1, x + 17, y, 0xFF140C05);
            g.fill(x - 1, y - 1, x, y + 17, 0xFF140C05);
            g.fill(x - 1, y + 16, x + 17, y + 17, 0x40FFE0A0);
            g.fill(x + 16, y - 1, x + 17, y + 17, 0x40FFE0A0);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            g.fill(x, y, x + w, y + h, EDGE);
            if (primary && active) {
                int top = lerp(0xFFD9A85C, 0xFFF0C878, glow), bottom = lerp(0xFFA87C3C, 0xFFC89850, glow);
                g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, down ? bottom : top, down ? top : bottom);
            } else
                board(g, x + 1, y + 1, w - 2, h - 2, !active ? 0xD0140C05 : lerp(0xA0301E0E, 0x60301E0E, glow));
            int text = !active ? 0xFF7A6A52 : primary ? 0xFF2A1A0A : ink();
            if (!down && active)
                g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x40FFE8C0);
            var font = Minecraft.getInstance().font;
            int o = down ? 1 : 0;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, false);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.WOODEN_BUTTON_CLICK_ON;
        }

        @Override
        public SoundEvent open() {
            return SoundEvents.WOOD_PLACE;
        }

        @Override
        public SoundEvent close() {
            return SoundEvents.WOOD_HIT;
        }
    },

    /**
     * The anvil's: vanilla's anvil texture drawn in place under a cold wash, a polished-steel trim round the panel (the anvil's
     * worked face), hammer-dimple sockets, buttons of dark cast iron with the primary one bright struck steel. Anvil sounds only.
     */
    ANVIL(0xFFE6E9EC, 0xFF9AA2AA, 0xFFFFC66A, 0xFFFF5A44, 0xFF0D0F11, 0xFFAEB8C2, 0xFF3A4046) {
        private static final ResourceLocation IRON = ResourceLocation.withDefaultNamespace("textures/block/anvil.png");
        private static final int EDGE = 0xFF0D0F11, FACE = 0xFFAEB8C2;

        private static void cast(GuiGraphics g, int x, int y, int w, int h, int wash) {
            g.blit(IRON, x, y, 0, 0, w, h, 16, 16);
            g.fill(x, y, x + w, y + h, wash);
        }

        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x60000000);
            g.fill(x, y, x + w, y + h, EDGE);
            cast(g, x + 1, y + 1, w - 2, h - 2, 0xB0101418);
            g.fillGradient(x + 1, y + 1, x + w - 1, y + Math.min(h - 1, 30), 0x28FFFFFF, 0x00FFFFFF);
            g.renderOutline(x + 3, y + 3, w - 6, h - 6, FACE);
            g.renderOutline(x + 4, y + 4, w - 8, h - 8, 0x50000000);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF1C2024);
            g.fill(x - 1, y - 1, x + 17, y, 0xFF050607);
            g.fill(x - 1, y - 1, x, y + 17, 0xFF050607);
            g.fill(x - 1, y + 16, x + 17, y + 17, 0x50D8E4F0);
            g.fill(x + 16, y - 1, x + 17, y + 17, 0x50D8E4F0);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            g.fill(x, y, x + w, y + h, EDGE);
            if (primary && active) {
                int top = lerp(0xFFA9B4BF, 0xFFD2DCE6, glow), bottom = lerp(0xFF6C7782, 0xFF909BA6, glow);
                g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, down ? bottom : top, down ? top : bottom);
            } else
                cast(g, x + 1, y + 1, w - 2, h - 2, !active ? 0xE0080A0C : lerp(0xB0101418, 0x70101418, glow));
            int text = !active ? 0xFF6A7178 : primary ? 0xFF101418 : ink();
            if (!down && active)
                g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x50FFFFFF);
            var font = Minecraft.getInstance().font;
            int o = down ? 1 : 0;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, false);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.ANVIL_PLACE;
        }

        @Override
        public SoundEvent open() {
            return SoundEvents.ANVIL_PLACE;
        }

        @Override
        public SoundEvent close() {
            return SoundEvents.ANVIL_HIT;
        }
    },

    /**
     * The smoker's: smoke-cured hickory, dark and warm. Broad boards with seams and grain, soot gathering at the top, a low
     * ember glow at the bottom edge. Buttons are dark wood that warms under the pointer; the primary one is a live coal.
     */
    SMOKE(0xFFEBD3A6, 0xFFA88A60, 0xFFFFA64A, 0xFFFF5A3C, 0xFF1B1008, 0xFF553A1F, 0xFF3B2713) {
        private static final int EDGE = 0xFF1B1008, BOARD = 0xFF3B2713; // spruce log bark: 3B2713 body, 4D3317 light, 2E1C0A dark, 553A1F highlight
        /** Grain, fixed: x, y, length as fractions of the panel. */
        private static final float[] GRAIN = grain();

        private static float[] grain() {
            Random r = new Random(0x5A0CE);
            float[] out = new float[3 * 160];
            for (int i = 0; i < out.length; i++)
                out[i] = r.nextFloat();
            return out;
        }

        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x70000000);
            g.fill(x, y, x + w, y + h, EDGE);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BOARD);
            for (int bx = x + 30; bx < x + w - 2; bx += 30) { // board seams
                g.fill(bx, y + 1, bx + 1, y + h - 1, 0xFF2E1608);
                g.fill(bx + 1, y + 1, bx + 2, y + h - 1, 0x40553A1F);
            }
            for (int i = 0; i < GRAIN.length; i += 3) {
                int gx = x + 2 + (int) (GRAIN[i] * (w - 4)), gy = y + 2 + (int) (GRAIN[i + 1] * (h - 4));
                int len = 3 + (int) (GRAIN[i + 2] * 14);
                g.fill(gx, gy, gx + 1, Math.min(y + h - 1, gy + len), (i & 1) == 0 ? 0xFF2E1C0A : 0xFF4D3317);
            }
            g.fillGradient(x + 1, y + 1, x + w - 1, y + 1 + Math.min(40, h / 3), 0x50000000, 0x00000000); // soot
            g.fillGradient(x + 1, y + h - 28, x + w - 1, y + h - 1, 0x00FF6A1C, 0x40FF6A1C); // embers
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x70FF8A3A);
            g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x10FFE0B0);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0F0A06);
            g.fill(x - 1, y - 1, x + 17, y, 0xFF000000);
            g.fill(x - 1, y - 1, x, y + 17, 0xFF000000);
            g.fill(x - 1, y + 16, x + 17, y + 17, 0x1CFFE0B0);
            g.fill(x + 16, y - 1, x + 17, y + 17, 0x1CFFE0B0);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int top, bottom, text;
            if (!active) {
                top = 0xFF2A1A0C;
                bottom = 0xFF20130A;
                text = 0xFF6E6054;
            } else if (primary) {
                top = lerp(0xFFC04A1A, 0xFFF07A2A, glow);
                bottom = lerp(0xFF6A220C, 0xFF9A3A16, glow);
                text = 0xFFFFEBC8;
            } else {
                top = lerp(0xFF4D3317, 0xFF6A4624, glow);
                bottom = lerp(0xFF2E1C0A, 0xFF3F2813, glow);
                text = lerp(dim(), ink(), glow);
            }
            if (down) {
                int t = top;
                top = bottom;
                bottom = t;
            }
            g.fill(x, y, x + w, y + h, EDGE);
            g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, top, bottom);
            if (!down && active)
                g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x24FFE0B0);
            var font = Minecraft.getInstance().font;
            int o = down ? 1 : 0;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, false);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.WOODEN_BUTTON_CLICK_ON;
        }

        @Override
        public SoundEvent open() {
            return SoundEvents.CAMPFIRE_CRACKLE;
        }

        @Override
        public SoundEvent close() {
            return SoundEvents.WOODEN_TRAPDOOR_CLOSE;
        }
    },

    /**
     * The crude blast furnace's: heavy gunmetal plate in a plain riveted casing, and the cracks of cooling slag glowing at
     * the bottom edge. Buttons are cast iron; the primary one is molten orange.
     */
    SLAG(0xFFDAD6D0, 0xFF8E8780, 0xFFFF8A2A, 0xFFFF4A2A, 0xFF0E0C0B, 0xFF3C3633, 0xFF2A2523) {
        private static final int EDGE = 0xFF0A0807, PLATE = 0xFF211D1B;
        /** Cracks in the slag, fixed: x fraction, height, wobble seed. */
        private static final float[] CRACKS = cracks();

        private static float[] cracks() {
            Random r = new Random(0x51A6);
            float[] out = new float[3 * 26];
            for (int i = 0; i < out.length; i++)
                out[i] = r.nextFloat();
            return out;
        }

        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x70000000);
            g.fill(x, y, x + w, y + h, EDGE);
            g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF2A2523, PLATE);
            g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x18FFFFFF);
            g.fill(x + 1, y + 1, x + 2, y + h - 1, 0x0CFFFFFF);
            // A casing: a plain raised frame round the plate, one rivet in each corner, nothing across the middle.
            g.renderOutline(x + 3, y + 3, w - 6, h - 6, 0xFF38322F);
            g.renderOutline(x + 4, y + 4, w - 8, h - 8, 0x50000000);
            for (int[] c : new int[][] {{x + 5, y + 5}, {x + w - 8, y + 5}, {x + 5, y + h - 8}, {x + w - 8, y + h - 8}}) {
                g.fill(c[0], c[1], c[0] + 3, c[1] + 3, 0xFF3A3532);
                g.fill(c[0], c[1], c[0] + 3, c[1] + 1, 0xFF5A5450);
            }
            g.fillGradient(x + 1, y + h - 26, x + w - 1, y + h - 1, 0x00FF5A10, 0x30FF5A10);
            for (int i = 0; i < CRACKS.length; i += 3) { // cooling slag, glowing in its cracks
                int cx = x + 2 + (int) (CRACKS[i] * (w - 4)), top = y + h - 3 - (int) (4 + CRACKS[i + 1] * 12);
                for (int cy = y + h - 2; cy > top; cy--) {
                    cx += CRACKS[i + 2] > 0.5f ? ((cy + i) % 3 == 0 ? 1 : 0) : ((cy + i) % 3 == 0 ? -1 : 0);
                    int a = 0x30 + (int) (0x90 * (cy - top) / (float) (y + h - 2 - top));
                    g.fill(cx, cy, cx + 1, cy + 1, a << 24 | 0xFF7A20);
                }
            }
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0F0D0C);
            g.fill(x - 1, y - 1, x + 17, y, 0xFF000000);
            g.fill(x - 1, y - 1, x, y + 17, 0xFF000000);
            g.fill(x - 1, y + 16, x + 17, y + 17, 0x1CFFFFFF);
            g.fill(x + 16, y - 1, x + 17, y + 17, 0x1CFFFFFF);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int top, bottom, text;
            if (!active) {
                top = 0xFF211D1B;
                bottom = 0xFF181514;
                text = 0xFF6A645E;
            } else if (primary) {
                top = lerp(0xFFE0782A, 0xFFFFA44A, glow);
                bottom = lerp(0xFF8E3A12, 0xFFB45A22, glow);
                text = 0xFF1A0C04;
            } else {
                top = lerp(0xFF3E3835, 0xFF5A4A40, glow);
                bottom = lerp(0xFF25201E, 0xFF372C26, glow);
                text = lerp(dim(), ink(), glow);
            }
            if (down) {
                int t = top;
                top = bottom;
                bottom = t;
            }
            g.fill(x, y, x + w, y + h, EDGE);
            g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, top, bottom);
            if (!down && active)
                g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x28FFFFFF);
            var font = Minecraft.getInstance().font;
            int o = down ? 1 : 0;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, false);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.IRON_TRAPDOOR_CLOSE;
        }

        @Override
        public SoundEvent open() {
            return SoundEvents.IRON_DOOR_OPEN;
        }

        @Override
        public SoundEvent close() {
            return SoundEvents.IRON_DOOR_CLOSE;
        }
    },

    /**
     * A phosphor terminal: black glass with a faint glow, scanlines, one slow bright band rolling down it, light-on-black text. Buttons
     * are outlined boxes that invert under the pointer; the primary one is double-ruled. It opens as a CRT does, a bright line at the
     * middle that grows to the full screen. The Computer's desktop, in one of four phosphors (the Computer's settings pick which);
     * the drawing is {@link TerminalLook}, the same for all four.
     */
    TERMINAL(0xFF5CFF7A, 0xFF2E9E48, 0xFFC4FFCE, 0xFFFFB02E, 0xFF020804, 0xFF1F6B33, 0xFF07140B) {
        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            TerminalLook.panel(this, g, x, y, w, h);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            TerminalLook.slot(this, g, x, y);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            TerminalLook.button(this, g, x, y, w, h, label, glow, down, active, primary);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.COMPARATOR_CLICK;
        }

        @Override
        public SoundEvent open() {
            return SoundEvents.BEACON_ACTIVATE;
        }

        @Override
        public SoundEvent close() {
            return SoundEvents.BEACON_DEACTIVATE;
        }

        @Override
        public void beginReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            TerminalLook.beginReveal(this, g, x, y, w, h, t);
        }

        @Override
        public void endReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            TerminalLook.endReveal(this, g, x, y, w, h, t);
        }
    },
    /** The terminal in amber phosphor. */
    TERMINAL_AMBER(0xFFFFB347, 0xFF9E6A1F, 0xFFFFE2A8, 0xFFFF5A3C, 0xFF080502, 0xFF6B4210, 0xFF140D05) {
        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            TerminalLook.panel(this, g, x, y, w, h);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            TerminalLook.slot(this, g, x, y);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            TerminalLook.button(this, g, x, y, w, h, label, glow, down, active, primary);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.COMPARATOR_CLICK;
        }

        @Override
        public SoundEvent open() {
            return SoundEvents.BEACON_ACTIVATE;
        }

        @Override
        public SoundEvent close() {
            return SoundEvents.BEACON_DEACTIVATE;
        }

        @Override
        public void beginReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            TerminalLook.beginReveal(this, g, x, y, w, h, t);
        }

        @Override
        public void endReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            TerminalLook.endReveal(this, g, x, y, w, h, t);
        }
    },
    /** The terminal in white phosphor. */
    TERMINAL_WHITE(0xFFE6EAE6, 0xFF8C928C, 0xFFFFFFFF, 0xFFFFB02E, 0xFF050605, 0xFF4F544F, 0xFF101210) {
        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            TerminalLook.panel(this, g, x, y, w, h);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            TerminalLook.slot(this, g, x, y);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            TerminalLook.button(this, g, x, y, w, h, label, glow, down, active, primary);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.COMPARATOR_CLICK;
        }

        @Override
        public SoundEvent open() {
            return SoundEvents.BEACON_ACTIVATE;
        }

        @Override
        public SoundEvent close() {
            return SoundEvents.BEACON_DEACTIVATE;
        }

        @Override
        public void beginReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            TerminalLook.beginReveal(this, g, x, y, w, h, t);
        }

        @Override
        public void endReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            TerminalLook.endReveal(this, g, x, y, w, h, t);
        }
    },
    /** The terminal in blue phosphor. */
    TERMINAL_BLUE(0xFF5CC8FF, 0xFF2E7A9E, 0xFFC4EBFF, 0xFFFFB02E, 0xFF020608, 0xFF1F4F6B, 0xFF07101A) {
        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            TerminalLook.panel(this, g, x, y, w, h);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            TerminalLook.slot(this, g, x, y);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            TerminalLook.button(this, g, x, y, w, h, label, glow, down, active, primary);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.COMPARATOR_CLICK;
        }

        @Override
        public SoundEvent open() {
            return SoundEvents.BEACON_ACTIVATE;
        }

        @Override
        public SoundEvent close() {
            return SoundEvents.BEACON_DEACTIVATE;
        }

        @Override
        public void beginReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            TerminalLook.beginReveal(this, g, x, y, w, h, t);
        }

        @Override
        public void endReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            TerminalLook.endReveal(this, g, x, y, w, h, t);
        }
    },

    /**
     * The powered suit's own material: matte carbon weave under armour plate, cyan hairlines where it is lit, amber where it warns. Corners
     * are chamfered and braced, the top edge carries a row of latches, and a slot is a dark socket with cyan corner brackets. It opens as two
     * armour plates draw apart from the middle with a servo's push, and closes the same way in reverse. Built from fills and a fixed-seed
     * speckle, so nothing crawls.
     */
    SUIT(0xFFE4F1F6, 0xFF7C99A6, 0xFF4FE3FF, 0xFFFFA52E, 0xFF06090B, 0x9035B6D0, 0xFF1B2328) {
        private static final int FACE = 0xFF14181B, WEAVE = 0xFF191F23, EDGE = 0xFF05080A, CYAN = 0xFF4FE3FF, AMBER = 0xFFFFA52E;
        private static final float[] SPECK = speck();

        private static float[] speck() {
            Random r = new Random(0x5017);
            float[] out = new float[2 * 300];
            for (int i = 0; i < out.length; i++)
                out[i] = r.nextFloat();
            return out;
        }

        /** The carbon weave: horizontal bands two pixels deep, every other one a shade lighter, crossed by faint verticals every four. */
        private static void weave(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x, y, x + w, y + h, FACE);
            for (int row = 0; row < h; row += 4)
                g.fill(x, y + row, x + w, Math.min(y + h, y + row + 2), WEAVE);
            for (int col = 0; col < w; col += 4)
                g.fill(x + col, y, x + col + 1, y + h, 0x14000000);
        }

        /** A chamfered corner brace: cyan L-bracket six pixels long. */
        private static void brace(GuiGraphics g, int x, int y, int dx, int dy) {
            g.fill(Math.min(x, x + dx * 6), y, Math.max(x, x + dx * 6) + 1, y + 1, 0xA035B6D0);
            g.fill(x, Math.min(y, y + dy * 6), x + 1, Math.max(y, y + dy * 6) + 1, 0xA035B6D0);
        }

        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x60000000);
            g.fill(x, y, x + w, y + h, EDGE);
            weave(g, x + 1, y + 1, w - 2, h - 2);
            g.fillGradient(x + 1, y + 1, x + w - 1, y + Math.min(h - 1, 30), 0x1CFFFFFF, 0x00FFFFFF);
            for (int i = 0; i < SPECK.length; i += 2) {
                int sx = x + 2 + (int) (SPECK[i] * (w - 4)), sy = y + 2 + (int) (SPECK[i + 1] * (h - 4));
                g.fill(sx, sy, sx + 1, sy + 1, (i & 2) == 0 ? 0x10FFFFFF : 0x28000000);
            }
            // chamfered corners
            for (int i = 0; i < 4; i++) {
                g.fill(x + i, y + 3 - i, x + i + 1, y + 4 - i, EDGE);
                g.fill(x + w - 1 - i, y + 3 - i, x + w - i, y + 4 - i, EDGE);
                g.fill(x + i, y + h - 4 + i, x + i + 1, y + h - 3 + i, EDGE);
                g.fill(x + w - 1 - i, y + h - 4 + i, x + w - i, y + h - 3 + i, EDGE);
            }
            g.renderOutline(x + 3, y + 3, w - 6, h - 6, 0x3835B6D0);
            brace(g, x + 5, y + 5, 1, 1);
            brace(g, x + w - 6, y + 5, -1, 1);
            brace(g, x + 5, y + h - 6, 1, -1);
            brace(g, x + w - 6, y + h - 6, -1, -1);
            // the row of latches along the top edge
            for (int lx = x + 14; lx < x + w - 14; lx += 10)
                g.fill(lx, y + 1, lx + 5, y + 3, (lx / 10 & 3) == 0 ? AMBER : 0xFF2A353B);
            g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x2CFFFFFF);
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x60000000);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF06090B);
            g.fill(x - 1, y + 16, x + 17, y + 17, 0x3035B6D0);
            g.fill(x + 16, y - 1, x + 17, y + 17, 0x3035B6D0);
            int c = 0xC04FE3FF;
            g.fill(x - 1, y - 1, x + 3, y, c);
            g.fill(x - 1, y - 1, x, y + 3, c);
            g.fill(x + 14, y + 16, x + 17, y + 17, c);
            g.fill(x + 16, y + 14, x + 17, y + 17, c);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int top, bottom, text, line;
            if (!active) {
                top = 0xFF1B2125;
                bottom = 0xFF14191C;
                text = 0xFF4A5A62;
                line = 0xFF26323A;
            } else if (primary) {
                top = lerp(0xFF4A3410, 0xFF9A6A1C, glow);
                bottom = lerp(0xFF2E200A, 0xFF6A4810, glow);
                text = 0xFFFFE6B8;
                line = AMBER;
            } else {
                top = lerp(0xFF222B31, 0xFF2E3C44, glow);
                bottom = lerp(0xFF161C20, 0xFF1E282E, glow);
                text = lerp(dim(), ink(), glow);
                line = lerp(0xFF2E7F94, CYAN, glow);
            }
            if (down) {
                top = lerp(top, 0xFF000000, 0.3f);
                bottom = lerp(bottom, 0xFF000000, 0.3f);
            }
            g.fill(x, y, x + w, y + h, EDGE);
            g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, top, bottom);
            g.renderOutline(x, y, w, h, line);
            g.fill(x + 1, y + 1, x + 4, y + 2, line); // a lit tab at the corner
            if (active && glow > 0)
                g.renderOutline(x - 1, y - 1, w + 2, h + 2, (int) (glow * 0x50) << 24 | (primary ? 0xFFA52E : 0x4FE3FF));
            var font = Minecraft.getInstance().font;
            int o = down ? 1 : 0;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, false);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.IRON_TRAPDOOR_OPEN;
        }

        @Override
        public SoundEvent open() {
            return SoundEvents.PISTON_EXTEND;
        }

        @Override
        public SoundEvent close() {
            return SoundEvents.PISTON_CONTRACT;
        }

        private int gap(int w, float t) {
            return Math.round((w / 2f + 6) * t);
        }

        @Override
        public void beginReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            if (t < 1)
                g.enableScissor(x + w / 2 - gap(w, t) - 1, y - 4, x + w / 2 + gap(w, t) + 1, y + h + 8);
        }

        @Override
        public void endReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            if (t >= 1)
                return;
            g.flush();
            g.disableScissor();
            int mid = x + w / 2, gap = gap(w, t);
            // The two armour plates, drawing back from the middle into their housings.
            int left = mid - gap - x, right = x + w - (mid + gap);
            if (left > 0) {
                weave(g, x, y, left, h);
                g.fill(x, y, x + left, y + 1, EDGE);
                g.fill(x + left - 2, y, x + left, y + h, CYAN);
                for (int cy = y + 6; cy < y + h - 6; cy += 12)
                    g.fill(x + left - 8, cy, x + left - 3, cy + 3, AMBER);
            }
            if (right > 0) {
                weave(g, mid + gap, y, right, h);
                g.fill(mid + gap, y, mid + gap + 2, y + h, CYAN);
                for (int cy = y + 6; cy < y + h - 6; cy += 12)
                    g.fill(mid + gap + 3, cy, mid + gap + 8, cy + 3, AMBER);
            }
        }
    };

    private final int ink, dim, accent, warn, well, trim, plate;

    /** {@code well}: the floor of a sunken area; {@code trim}: its rim; {@code plate}: a raised label plate. */
    GuiSkin(int ink, int dim, int accent, int warn, int well, int trim, int plate) {
        this.ink = ink;
        this.dim = dim;
        this.accent = accent;
        this.warn = warn;
        this.well = well;
        this.trim = trim;
        this.plate = plate;
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


    /** A sunken area: gauges, text wells, readouts. Drawn from (x, y), {@code w} by {@code h} including the rim. */
    public void inset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, well);
        g.renderOutline(x, y, w, h, trim);
    }

    /** A raised plate for a stamped readout between two controls. */
    public void plaque(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, plate);
        g.renderOutline(x, y, w, h, lerp(trim, 0xFFFFFFFF, 0.2f));
    }

    /**
     * A horizontal gauge in a well: {@code fill} of the way along, a tick at {@code mark} (both 0..1
     * of the width; {@code mark} negative for none), and the part past the tick in {@link #warn()}.
     */
    public void bar(GuiGraphics g, int x, int y, int w, int h, float fill, float mark) {
        bar(g, x, y, w, h, fill, mark, ink);
    }

    /** {@link #bar(GuiGraphics, int, int, int, int, float, float)} with the part before the tick in {@code lead}, to tell stacked gauges apart. */
    public void bar(GuiGraphics g, int x, int y, int w, int h, float fill, float mark, int lead) {
        gauge(g, x, y, w, h, false, fill, mark, lead, 0);
    }

    /**
     * The one gauge every screen and HUD draws through ({@link Gauge} links it to a figure): a well {@code w} by
     * {@code h}, filled {@code fill} of the way along the long axis ({@code vertical} from the bottom, else from the
     * left) in {@code lead}, a tick at {@code mark} with the part past it in {@link #warn()} ({@code mark} negative for
     * none), and {@code ticks} graduations outside the well when above 1.
     */
    public void gauge(GuiGraphics g, int x, int y, int w, int h, boolean vertical, float fill, float mark, int lead, int ticks) {
        inset(g, x - 1, y - 1, w + 2, h + 2);
        int len = vertical ? h : w;
        int f = Math.round(Mth.clamp(fill, 0, 1) * len), m = mark < 0 ? len : Math.round(Mth.clamp(mark, 0, 1) * len);
        if (vertical) {
            g.fill(x, y + h - Math.min(f, m), x + w, y + h, lead);
            if (f > m)
                g.fill(x, y + h - f, x + w, y + h - m, warn);
            if (mark >= 0)
                g.fill(x - 2, y + h - m, x + w + 2, y + h - m + 1, accent);
            for (int i = 1; i < ticks; i++)
                g.fill(x + w + 2, y + h - i * h / ticks, x + w + (i % 3 == 0 ? 6 : 4), y + h - i * h / ticks + 1, trim);
        } else {
            g.fill(x, y, x + Math.min(f, m), y + h, lead);
            if (f > m)
                g.fill(x + m, y, x + f, y + h, warn);
            if (mark >= 0)
                g.fill(x + m, y - 2, x + m + 1, y + h + 2, accent);
            for (int i = 1; i < ticks; i++)
                g.fill(x + i * w / ticks, y + h + 2, x + i * w / ticks + 1, y + h + (i % 3 == 0 ? 6 : 4), trim);
        }
    }

    /** A 10x10 check box at (x, y), {@code glow} 0..1 for the pointer being over it. */
    void checkbox(GuiGraphics g, int x, int y, boolean on, float glow) {
        inset(g, x, y, 10, 10);
        if (glow > 0)
            g.fill(x + 1, y + 1, x + 9, y + 9, (int) (glow * 0x24) << 24 | 0xFFFFFF);
        if (on) {
            GuiStyle.line(g, x + 2, y + 5, x + 4, y + 7, accent);
            GuiStyle.line(g, x + 4, y + 7, x + 8, y + 2, accent);
            GuiStyle.line(g, x + 2, y + 4, x + 4, y + 6, accent);
            GuiStyle.line(g, x + 4, y + 6, x + 8, y + 1, accent);
        }
    }

    /** A slider's groove and thumb in a 12-high box: {@code frac} 0..1 along it, {@code glow} 0..1 for pointer or drag. */
    void slider(GuiGraphics g, int x, int y, int w, float frac, float glow, boolean active) {
        inset(g, x, y + 4, w, 4);
        int tx = x + Math.round((w - 6) * frac);
        if (frac > 0)
            g.fill(x + 1, y + 5, tx + 3, y + 7, active ? lerp(dim, accent, glow) : dim);
        plaque(g, tx, y, 6, 12);
        g.fill(tx + 2, y + 2, tx + 4, y + 10, active ? lerp(dim, accent, glow) : dim);
    }

    /**
     * One folder tab, standing on the top edge of a panel whose top is at {@code y + h}. It is the panel's own face (drawn by {@link #panel}
     * and clipped to the tab), so it matches on every skin. The selected tab is flush: its face runs over the panel's top border with no seam.
     * An unselected one sits back, two pixels lower and darker, and the panel's border runs under it. Draw it after the panel.
     */
    public void folderTab(GuiGraphics g, int x, int y, int w, int h, boolean selected, float glow) {
        folderTab(g, x, y, w, h, selected, glow, null);
    }

    /**
     * As above; {@code host} is the real panel's rectangle (x, y, w, h) when the caller knows it. The selected tab's flush patch is then the
     * host panel's own face, drawn at its true size and lifted so its border falls above the patch, so textured faces (crinkle, courses,
     * grain) match instead of being rescaled into the tab's small rectangle.
     */
    public void folderTab(GuiGraphics g, int x, int y, int w, int h, boolean selected, float glow, int[] host) {
        inTab = true;
        try {
            folderTabFace(g, x, y, w, h, selected, glow, host);
        } finally {
            inTab = false;
        }
    }

    /** True while a folder tab borrows {@link #panel}: a skin leaves out ornaments that belong to a whole window (stripes, lamps, brackets). */
    static boolean inTab;

    private void folderTabFace(GuiGraphics g, int x, int y, int w, int h, boolean selected, float glow, int[] host) {
        if (selected) {
            // The tab itself, with its own frame and rails, standing above the panel.
            g.enableScissor(x, y, x + w, y + h);
            panel(g, x, y, w, h + 14);
            g.disableScissor();
            // Below it, only the plate's face (inset past the rails) runs over the panel's top border, so the tab and the panel are one piece.
            g.enableScissor(x + 6, y + h, x + w - 6, y + h + FLUSH);
            if (host != null)
                panel(g, host[0], host[1] - FLUSH, host[2], host[3]);
            else
                panel(g, x, y, w, h + 14);
            g.disableScissor();
            return;
        }
        g.enableScissor(x, y, x + w, y + h);
        panel(g, x, y + 2, w, h + 14);
        g.fill(x, y, x + w, y + h, 0x58000000);
        if (glow > 0)
            g.fill(x, y, x + w, y + h, (int) (0x30 * glow) << 24 | accent & 0xFFFFFF);
        g.disableScissor();
    }

    /** How many rows under a selected folder tab the panel's own top border is covered by the tab's face. */
    private static final int FLUSH = 9;

    /** One tab of a strip. The selected one is a plate with an underline; the rest sit in the well. */
    void tab(GuiGraphics g, int x, int y, int w, int h, Component label, boolean selected, boolean active, float glow) {
        if (selected) {
            plaque(g, x, y, w, h);
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, accent);
        } else {
            g.fill(x, y, x + w, y + h, well);
            g.renderOutline(x, y, w, h, trim);
            if (glow > 0)
                g.fill(x + 1, y + 1, x + w - 1, y + h - 1, (int) (glow * 0x1C) << 24 | 0xFFFFFF);
        }
        var font = Minecraft.getInstance().font;
        int text = !active ? lerp(dim, 0xFF000000, 0.35f) : selected ? ink : lerp(dim, ink, glow);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, text, false);
    }

    /** A list row's highlight: the picked one in the accent, the one under the pointer lightly. */
    public void row(GuiGraphics g, int x, int y, int w, int h, boolean picked, boolean hovered) {
        if (picked)
            g.fill(x, y, x + w, y + h, 0x60 << 24 | accent & 0xFFFFFF);
        else if (hovered)
            g.fill(x, y, x + w, y + h, 0x20FFFFFF);
    }

    /** A scrollbar thumb in a track {@code h} tall: {@code total} of something, {@code visible} shown from {@code at}. Nothing when it all fits. */
    public void scrollbar(GuiGraphics g, int x, int y, int h, int total, int visible, int at) {
        if (total <= visible)
            return;
        g.fill(x, y, x + 2, y + h, 0x30000000);
        int bar = Math.max(8, h * visible / total);
        int by = y + (h - bar) * at / Math.max(1, total - visible);
        g.fill(x, by, x + 2, by + bar, dim);
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

    /** Beats of a flow screen (docs/gui/gui_flow_feel.md) that every skin answers with a sound and a move. */
    public enum Cue { SEAT, REJECT, STAGE, START, FINISH, ABORT }

    /** Sound floor: the same cue inside this many ms of its last play is dropped, so rapid presses do not stack. */
    private static final long CUE_GAP_MS = 70;
    private static final long[] CUE_LAST = new long[Cue.values().length];

    /** The vanilla sound standing in for {@code cue} on this skin (null for none). */
    public SoundEvent sound(Cue cue) {
        return switch (this) {
            case BLUEPRINT -> switch (cue) {
                case SEAT -> SoundEvents.BOOK_PAGE_TURN;
                case REJECT -> SoundEvents.BOOK_PUT;
                case STAGE -> FSounds.UI_UNROLL.get();
                case START -> SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT;
                case FINISH -> FSounds.UI_STAMP.get();
                case ABORT -> FSounds.UI_ROLL.get();
            };
            case FORGE -> switch (cue) {
                case SEAT -> SoundEvents.CHAIN_PLACE;
                case REJECT -> SoundEvents.ANVIL_LAND;
                case STAGE -> FSounds.UI_HATCH_OPEN.get();
                case START -> SoundEvents.FIRECHARGE_USE;
                case FINISH -> SoundEvents.ANVIL_USE;
                case ABORT -> FSounds.UI_HATCH_CLOSE.get();
            };
            case ENAMEL, CONSOLE -> switch (cue) {
                case SEAT -> FSounds.UI_DETENT.get();
                case REJECT -> SoundEvents.DISPENSER_FAIL;
                case STAGE -> FSounds.UI_TOGGLE.get();
                case START -> SoundEvents.LEVER_CLICK;
                case FINISH -> SoundEvents.COMPARATOR_CLICK;
                case ABORT -> FSounds.UI_TOGGLE.get();
            };
            case STONE -> switch (cue) {
                case SEAT -> SoundEvents.STONE_PLACE;
                case REJECT -> SoundEvents.STONE_HIT;
                case STAGE -> SoundEvents.UI_STONECUTTER_SELECT_RECIPE;
                case START -> SoundEvents.GRINDSTONE_USE;
                case FINISH -> SoundEvents.UI_STONECUTTER_TAKE_RESULT;
                case ABORT -> SoundEvents.STONE_BREAK;
            };
            case MYSTIC -> switch (cue) {
                case SEAT -> SoundEvents.AMETHYST_BLOCK_CHIME;
                case REJECT -> SoundEvents.AMETHYST_BLOCK_HIT;
                case STAGE, START -> SoundEvents.ENCHANTMENT_TABLE_USE;
                case FINISH -> SoundEvents.AMETHYST_BLOCK_RESONATE;
                case ABORT -> SoundEvents.AMETHYST_BLOCK_BREAK;
            };
            case BRICK -> switch (cue) {
                case SEAT, START -> SoundEvents.CALCITE_PLACE;
                case REJECT -> SoundEvents.CALCITE_HIT;
                case STAGE -> SoundEvents.CALCITE_FALL;
                case FINISH -> SoundEvents.CALCITE_HIT;
                case ABORT -> SoundEvents.CALCITE_BREAK;
            };
            case WOOD -> switch (cue) { // vanilla wood events until real ones are edited from existing assets
                case SEAT -> SoundEvents.WOODEN_BUTTON_CLICK_ON;
                case REJECT -> SoundEvents.BAMBOO_WOOD_HIT;
                case STAGE -> SoundEvents.BAMBOO_WOOD_BUTTON_CLICK_ON;
                case START -> SoundEvents.CHEST_OPEN;
                case FINISH -> SoundEvents.BAMBOO_WOOD_PLACE;
                case ABORT -> SoundEvents.CHEST_CLOSE;
            };
            case ANVIL -> switch (cue) {
                case SEAT -> SoundEvents.ANVIL_PLACE;
                case REJECT -> SoundEvents.ANVIL_LAND;
                case STAGE -> SoundEvents.ANVIL_HIT;
                case START -> SoundEvents.ANVIL_PLACE;
                case FINISH -> SoundEvents.ANVIL_USE;
                case ABORT -> SoundEvents.ANVIL_BREAK;
            };
            case SMOKE -> switch (cue) {
                case SEAT -> SoundEvents.WOODEN_BUTTON_CLICK_ON;
                case REJECT -> SoundEvents.BAMBOO_WOOD_HIT;
                case STAGE, START -> SoundEvents.CAMPFIRE_CRACKLE;
                case FINISH -> SoundEvents.BAMBOO_WOOD_PLACE;
                case ABORT -> SoundEvents.WOODEN_TRAPDOOR_CLOSE;
            };
            case SLAG -> switch (cue) {
                case SEAT -> SoundEvents.IRON_TRAPDOOR_CLOSE;
                case REJECT -> SoundEvents.ANVIL_LAND;
                case STAGE, START -> SoundEvents.FIRECHARGE_USE;
                case FINISH -> SoundEvents.IRON_DOOR_OPEN;
                case ABORT -> SoundEvents.IRON_DOOR_CLOSE;
            };
            case TERMINAL, TERMINAL_AMBER, TERMINAL_WHITE, TERMINAL_BLUE -> switch (cue) {
                case SEAT -> SoundEvents.COMPARATOR_CLICK;
                case REJECT -> SoundEvents.NOTE_BLOCK_BASS.value();
                case STAGE, START -> SoundEvents.UI_BUTTON_CLICK.value();
                case FINISH -> SoundEvents.BEACON_POWER_SELECT;
                case ABORT -> SoundEvents.BEACON_DEACTIVATE;
            };
            case SUIT -> switch (cue) {
                case SEAT -> SoundEvents.IRON_TRAPDOOR_CLOSE;
                case REJECT -> SoundEvents.DISPENSER_FAIL;
                case STAGE -> SoundEvents.PISTON_EXTEND;
                case START -> SoundEvents.BEACON_POWER_SELECT;
                case FINISH -> SoundEvents.BEACON_ACTIVATE;
                case ABORT -> SoundEvents.PISTON_CONTRACT;
            };
            case VANILLA -> cue == Cue.REJECT ? SoundEvents.DISPENSER_FAIL : click();
        };
    }

    /**
     * Pitch carried by what the stock is made of: metal rings high, stone and grain sit low. Reads
     * the material's {@code kind}, never its name; null is neutral.
     */
    public static float kindPitch(Material.Kind kind) {
        if (kind == null)
            return 1f;
        return switch (kind) {
            case METAL -> 1.12f;
            case GLASS, CRYSTAL -> 1.25f;
            case MINERAL -> 0.85f;
            case CERAMIC, POLYMER -> 0.9f;
            case GRANULAR -> 0.8f;
            case ORGANIC -> 0.95f;
            default -> 1f;
        };
    }

    /** {@link #cue(Cue, Material.Kind, float)} with a neutral stock and no outcome bias. */
    public void cue(Cue cue) {
        cue(cue, null, 1f);
    }

    /**
     * Plays this skin's sound for {@code cue}. {@code kind}
     * is the stock's material kind (null for none) and shifts the pitch; {@code bias} is the
     * outcome: above 1 for a result that beats the alternative, below 1 for a worse one. Reject and
     * abort sound lower and quieter. The same cue inside {@value #CUE_GAP_MS} ms of its last play is
     * silent.
     */
    public void cue(Cue cue, Material.Kind kind, float bias) {
        long now = net.minecraft.Util.getMillis();
        int i = cue.ordinal();
        if (now - CUE_LAST[i] >= CUE_GAP_MS) {
            CUE_LAST[i] = now;
            float base = cue == Cue.REJECT ? 0.7f : cue == Cue.ABORT ? 0.75f : 1f;
            play(sound(cue), base * kindPitch(kind) * bias, cue == Cue.REJECT ? 0.5f : 0.6f);
        }
    }

    /** A screen sound, a touch off-pitch each time so repeats do not sound stamped out. */
    public static void play(SoundEvent sound, float pitch) {
        play(sound, pitch, 1f);
    }

    /** As {@link #play(SoundEvent, float)}, at a fraction of full volume. */
    public static void play(SoundEvent sound, float pitch, float volume) {
        if (sound != null)
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound,
                    pitch * (0.96f + 0.08f * ThreadLocalRandom.current().nextFloat()), volume));
    }

    static int lerp(int a, int b, float t) {
        return FastColor.ARGB32.lerp(t, a, b);
    }
}
