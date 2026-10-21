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
     * Forge-era iron: a dark riveted plate framed in brass, cast-brass buttons, the primary one
     * enamelled the colour of a fire. The screen opens like a hatch, two iron shutters parting
     * from the middle with a latch, and closes the same way.
     */
    FORGE(0xFFE8D9B0, 0xFF9A8A6A, 0xFFFF9A3C, 0xFFFF5A3C, 0xFF0E0C0B, 0xFF8C6A32, 0xFF5E4520) {
        private static final int IRON = 0xFF2E2A27, EDGE = 0xFF0E0C0B, BRASS = 0xFF8C6A32, BRASS_HI = 0xFFE8C878,
                BRASS_LO = 0xFF2A1E10;
        /** Brushing on the plate, fixed: x, y, length, as fractions of the plate. */
        private static final float[] STREAKS = streaks();

        private static float[] streaks() {
            Random r = new Random(0xF0E6E);
            float[] out = new float[3 * 110];
            for (int i = 0; i < out.length; i++)
                out[i] = r.nextFloat();
            return out;
        }

        private static void rivet(GuiGraphics g, int x, int y) {
            g.fill(x, y, x + 3, y + 3, BRASS);
            g.fill(x, y, x + 1, y + 1, BRASS_HI);
            g.fill(x + 2, y + 2, x + 3, y + 3, BRASS_LO);
        }

        /** Bare iron plate with its bevel, for the panel and the hatch shutters. */
        private static void plate(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x, y, x + w, y + h, EDGE);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, IRON);
            g.fillGradient(x + 1, y + 1, x + w - 1, y + Math.min(h - 1, 40), 0x18FFE0B0, 0x00FFE0B0);
            g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x30FFFFFF);
            g.fill(x + 1, y + 1, x + 2, y + h - 1, 0x30FFFFFF);
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x60000000);
            g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0x60000000);
        }

        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x60000000);
            plate(g, x, y, w, h);
            g.fillGradient(x + 1, y + h - 40, x + w - 1, y + h - 2, 0x00000000, 0x40000000);
            for (int i = 0; i < STREAKS.length; i += 3) {
                int sx = x + 3 + (int) (STREAKS[i] * (w - 6)), sy = y + 3 + (int) (STREAKS[i + 1] * (h - 6));
                int len = Math.min((int) ((0.1f + 0.3f * STREAKS[i + 2]) * w), x + w - 3 - sx);
                g.fill(sx, sy, sx + len, sy + 1, (i & 1) == 0 ? 0x08FFFFFF : 0x10000000);
            }
            g.renderOutline(x + 4, y + 4, w - 8, h - 8, BRASS);
            g.renderOutline(x + 5, y + 5, w - 10, h - 10, 0x30E8C878);
            // Rivets ride the trim, clear of anything a screen puts inside it.
            int across = Math.max(1, (w - 6) / 48), down = Math.max(1, (h - 6) / 48);
            for (int i = 0; i <= across; i++) {
                rivet(g, x + 3 + i * (w - 9) / across, y + 3);
                rivet(g, x + 3 + i * (w - 9) / across, y + h - 6);
            }
            for (int i = 1; i < down; i++) {
                rivet(g, x + 3, y + 3 + i * (h - 9) / down);
                rivet(g, x + w - 6, y + 3 + i * (h - 9) / down);
            }
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF141210);
            g.fill(x - 1, y - 1, x + 17, y, 0xFF000000);
            g.fill(x - 1, y - 1, x, y + 17, 0xFF000000);
            g.fill(x - 1, y + 16, x + 17, y + 17, 0x30E8C878);
            g.fill(x + 16, y - 1, x + 17, y + 17, 0x30E8C878);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int top, bottom, text;
            if (!active) {
                top = 0xFF4A4644;
                bottom = 0xFF34302E;
                text = 0xFF7A7470;
            } else if (primary) {
                top = lerp(0xFFB0401C, 0xFFE0602C, glow);
                bottom = lerp(0xFF6E2410, 0xFF8E3418, glow);
                text = 0xFFFFE6C0;
            } else {
                top = lerp(0xFF9C7A3C, 0xFFC8A054, glow);
                bottom = lerp(0xFF5E4520, 0xFF76582A, glow);
                text = 0xFF1E1408;
            }
            if (down) {
                int t = top;
                top = bottom;
                bottom = t;
            }
            g.fill(x, y, x + w, y + h, BRASS_LO);
            g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, top, bottom);
            if (!down && active)
                g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x60FFF0C0);
            if (primary && active && glow > 0) // the enamel's fire, catching
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
                g.fill(x + 1, y + top - 2, x + w - 1, y + top, BRASS);
            }
            if (bottom > 0) {
                plate(g, x, mid + gap, w, bottom);
                g.fill(x + 1, mid + gap, x + w - 1, mid + gap + 2, BRASS);
            }
        }
    },

    /**
     * An early instrument panel: black crinkle-finish steel, cream engraving, chrome bezels, amber
     * lamps. Push-buttons sit in chrome; the primary one is an amber lamp. The panel powers up --
     * drops into its mounting and its lamps come on with a flicker -- and goes dark to close.
     */
    PANEL(0xFFEDE6D0, 0xFF8E968C, 0xFFFFB347, 0xFFFF4A3A, 0xFF121514, 0x70D8DCD8, 0xFF343A38) {
        private static final int FACE = 0xFF232826, EDGE = 0xFF0A0C0B, CHROME = 0xFF8A908C;
        private static final float[] CRINKLE = crinkle();

        private static float[] crinkle() {
            Random r = new Random(0xC41C);
            float[] out = new float[2 * 420];
            for (int i = 0; i < out.length; i++)
                out[i] = r.nextFloat();
            return out;
        }

        private static void screw(GuiGraphics g, int x, int y) {
            g.fill(x + 1, y, x + 4, y + 5, CHROME);
            g.fill(x, y + 1, x + 5, y + 4, CHROME);
            g.fill(x + 1, y + 1, x + 2, y + 2, 0xFFD8DCD8);
            g.fill(x + 1, y + 2, x + 4, y + 3, 0xFF3A3E3C); // the slot
        }

        @Override
        public void panel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x60000000);
            g.fill(x, y, x + w, y + h, EDGE);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, FACE);
            g.fillGradient(x + 1, y + 1, x + w - 1, y + Math.min(h - 1, 36), 0x14FFFFFF, 0x00FFFFFF);
            for (int i = 0; i < CRINKLE.length; i += 2) {
                int sx = x + 2 + (int) (CRINKLE[i] * (w - 4)), sy = y + 2 + (int) (CRINKLE[i + 1] * (h - 4));
                g.fill(sx, sy, sx + 1, sy + 1, (i & 2) == 0 ? 0x0CFFFFFF : 0x22000000);
            }
            g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x28FFFFFF);
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x60000000);
            g.renderOutline(x + 3, y + 3, w - 6, h - 6, 0x60D8DCD8);
            screw(g, x + 4, y + 4);
            screw(g, x + w - 9, y + 4);
            screw(g, x + 4, y + h - 9);
            screw(g, x + w - 9, y + h - 9);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF121514);
            g.fill(x - 1, y + 16, x + 17, y + 17, 0x40D8DCD8);
            g.fill(x + 16, y - 1, x + 17, y + 17, 0x40D8DCD8);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int top, bottom, text;
            if (!active) {
                top = 0xFF262A28;
                bottom = 0xFF1E2220;
                text = 0xFF5A605C;
            } else if (primary) {
                top = lerp(0xFF6A4614, 0xFFC08028, glow);
                bottom = lerp(0xFF4A3010, 0xFF8A5A1A, glow);
                text = 0xFFFFE6B0;
            } else {
                top = lerp(0xFF343A38, 0xFF46504C, glow);
                bottom = lerp(0xFF202422, 0xFF2C3230, glow);
                text = lerp(dim(), ink(), glow);
            }
            if (down) {
                top = lerp(top, 0xFF000000, 0.3f);
                bottom = lerp(bottom, 0xFF000000, 0.3f);
            }
            g.fill(x, y, x + w, y + h, CHROME); // the bezel
            g.fill(x, y, x + w, y + 1, 0xFFD8DCD8);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, EDGE);
            g.fillGradient(x + 2, y + 2, x + w - 2, y + h - 2, top, bottom);
            if (primary && active && glow > 0) // the lamp's light on the panel round it
                g.renderOutline(x - 1, y - 1, w + 2, h + 2, (int) (glow * 0x70) << 24 | 0xFFB347);
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

        @Override
        public void beginReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            g.pose().pushPose();
            g.pose().translate(0, -(1 - t) * 10, 0);
        }

        @Override
        public void endReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            if (t < 1) {
                // Dark until powered; the lamps catch with a flicker on the way up.
                float dark = 1 - t;
                if (t > 0.55f && t < 0.7f)
                    dark += 0.35f;
                g.fill(x, y, x + w, y + h, (int) (Math.min(1, dark) * 0xE0) << 24);
            }
            g.pose().popPose();
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
        inset(g, x - 1, y - 1, w + 2, h + 2);
        int f = Math.round(Mth.clamp(fill, 0, 1) * w), m = mark < 0 ? w : Math.round(Mth.clamp(mark, 0, 1) * w);
        g.fill(x, y, x + Math.min(f, m), y + h, ink);
        if (f > m)
            g.fill(x + m, y, x + f, y + h, warn);
        if (mark >= 0)
            g.fill(x + m, y - 2, x + m + 1, y + h + 2, accent);
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
