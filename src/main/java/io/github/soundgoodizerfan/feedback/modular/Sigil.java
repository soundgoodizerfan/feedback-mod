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
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.process.Material;

/**
 * Sigil inscription ({@code docs/feedback_enchanting_spec.md} 3): the pen the Enchanting Table's pad runs. The pen
 * follows the mouse with the material's inertia, marks only while the button is down, the window is live, ink is left and
 * it moves fast enough, and the trail is graded against the effect's name in the Standard Galactic Alphabet
 * ({@link SgaGlyphs}). Pure: the screen steps a {@link Pen} live, the server replays the same samples through {@link #run}.
 */
public final class Sigil {

    private Sigil() {
    }

    /** The pad's inner size in screen px, and a glyph cell's size; the glyph box is centred in the pad. */
    public static final int PAD_W = 136, PAD_H = 82;
    public static final float CELL = 10f;
    public static final float ORIGIN_X = (PAD_W - SgaGlyphs.COLUMNS * CELL) / 2f, ORIGIN_Y = (PAD_H - SgaGlyphs.ROWS * CELL) / 2f;

    /** The letters to write for an effect: its id's a-z, at most {@code ENCHANT_WORD_MAX}. */
    public static String word(Attunements.Effect e) {
        String s = e.id().getPath().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        return s.substring(0, Math.min(ModularTuning.ENCHANT_WORD_MAX, s.length()));
    }

    public static int pack(int x, int y, boolean down) {
        return (x & 0xFF) | (y & 0xFF) << 8 | (down ? 1 << 16 : 0);
    }

    public static int x(int s) {
        return s & 0xFF;
    }

    public static int y(int s) {
        return s >> 8 & 0xFF;
    }

    public static boolean down(int s) {
        return (s >> 16 & 1) != 0;
    }

    public record Result(float accuracy, boolean negative, boolean reached) {}

    private static float noise(int h) {
        h ^= h >>> 16;
        h *= 0x7feb352d;
        h ^= h >>> 15;
        h *= 0x846ca68b;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 32767.5f - 1f;
    }

    /** Length of a greedy nearest-neighbour walk through a letter's cells, from the first in row-major order, in cells. */
    private static float chainLength(int[][] cells) {
        int n = cells.length;
        if (n < 2)
            return 0f;
        boolean[] used = new boolean[n];
        int at = 0;
        for (int k = 1; k < n; k++)
            if (cells[k][1] < cells[at][1] || cells[k][1] == cells[at][1] && cells[k][0] < cells[at][0])
                at = k;
        used[at] = true;
        float total = 0f;
        for (int step = 1; step < n; step++) {
            int best = -1;
            float bd = Float.MAX_VALUE;
            for (int k = 0; k < n; k++) {
                if (used[k])
                    continue;
                float d = (float) Math.hypot(cells[k][0] - cells[at][0], cells[k][1] - cells[at][1]);
                if (d < bd) {
                    bd = d;
                    best = k;
                }
            }
            used[best] = true;
            total += bd;
            at = best;
        }
        return total;
    }

    /** The pen: stepped one sample a tick, by the screen live and by the server in replay. */
    public static final class Pen {
        private final Material.Attuning p;
        private final float startTemperature, initialInk;
        private final String word;
        private final float[] ptX = new float[ModularTuning.ENCHANT_MAX_TICKS], ptY = new float[ModularTuning.ENCHANT_MAX_TICKS];
        private final int[] ptLetter = new int[ModularTuning.ENCHANT_MAX_TICKS];
        private int points, letter, lift, marksInLetter, tickIndex, finished = -1;
        private float penX, penY, prevX, prevY, inkLeft;
        private boolean started, marking;

        public Pen(Material.Attuning p, float startTemperature, String word) {
            this.p = p;
            this.startTemperature = startTemperature;
            this.word = word;
            float ink = ModularTuning.ENCHANT_INK_SLACK * word.length();
            for (int i = 0; i < word.length(); i++)
                ink += ModularTuning.ENCHANT_INK_FACTOR * chainLength(SgaGlyphs.cells(word.charAt(i)));
            inkLeft = initialInk = Math.max(ink, 0.0001f);
        }

        public void tick(int sample) {
            int i = tickIndex++;
            marking = false;
            finished = -1;
            if (letter >= word.length())
                return;
            boolean live = EnchantGame.live(p, startTemperature, i);
            float amp = ModularTuning.ENCHANT_PEN_JITTER * EnchantGame.jitter(p, startTemperature, i);
            int h1 = i * 31 + sample * 17 + Float.floatToIntBits(startTemperature), h2 = h1 * 0x9E3779B1 + 7;
            float tx = (x(sample) / 255f * PAD_W - ORIGIN_X) / CELL + amp * noise(h1);
            float ty = (y(sample) / 255f * PAD_H - ORIGIN_Y) / CELL + amp * noise(h2);
            prevX = penX;
            prevY = penY;
            if (!started) {
                started = true;
                penX = tx;
                penY = ty;
                prevX = tx;
                prevY = ty;
            } else {
                float gain = 1f / (1f + (p.lag() - 1f) * ModularTuning.ENCHANT_PEN_LAG_SCALE);
                penX += (tx - penX) * gain;
                penY += (ty - penY) * gain;
            }
            float speed = (float) Math.hypot(penX - prevX, penY - prevY);
            boolean down = down(sample);
            marking = down && live && inkLeft > 0f && speed >= p.coercive() * ModularTuning.ENCHANT_BITE_CELLS;
            if (marking) {
                if (points < ptX.length) {
                    ptX[points] = penX;
                    ptY[points] = penY;
                    ptLetter[points++] = letter;
                }
                inkLeft -= speed;
                marksInLetter++;
                lift = 0;
            } else if (!down && marksInLetter > 0 && ++lift >= ModularTuning.ENCHANT_LIFT_TICKS) {
                finished = letter++;
                lift = 0;
                marksInLetter = 0;
            }
        }

        public float penX() {
            return penX;
        }

        public float penY() {
            return penY;
        }

        public int letter() {
            return letter;
        }

        public boolean done() {
            return letter >= word.length();
        }

        public int points() {
            return points;
        }

        public float pointX(int i) {
            return ptX[i];
        }

        public float pointY(int i) {
            return ptY[i];
        }

        public int pointLetter(int i) {
            return ptLetter[i];
        }

        public boolean marking() {
            return marking;
        }

        public boolean reached() {
            return points > 0;
        }

        public int finishedLetter() {
            return finished;
        }

        public float inkFraction() {
            return Math.max(0f, Math.min(1f, inkLeft / initialInk));
        }

        /** Radius, in cells, within which a mark counts as on a star. */
        public float reach() {
            return ModularTuning.ENCHANT_REACH_CELLS * (0.6f + p.loopWidth());
        }

        public float letterAccuracy(int letter, boolean mirrored) {
            if (letter < 0 || letter >= word.length())
                return 0f;
            int[][] cells = SgaGlyphs.cells(word.charAt(letter));
            float r = reach(), r2 = r * r, wide = 1.8f * r * 1.8f * r;
            int n = 0, near = 0, hit = 0;
            for (int k = 0; k < points; k++) {
                if (ptLetter[k] != letter)
                    continue;
                n++;
                for (int[] c : cells)
                    if (sq(ptX[k], ptY[k], c, mirrored) <= wide) {
                        near++;
                        break;
                    }
            }
            if (n == 0 || cells.length == 0)
                return 0f;
            for (int[] c : cells)
                for (int k = 0; k < points; k++)
                    if (ptLetter[k] == letter && sq(ptX[k], ptY[k], c, mirrored) <= r2) {
                        hit++;
                        break;
                    }
            return (hit / (float) cells.length) * (near / (float) n);
        }

        private static float sq(float x, float y, int[] cell, boolean mirrored) {
            float cx = (mirrored ? SgaGlyphs.COLUMNS - 1 - cell[0] : cell[0]) + 0.5f, cy = cell[1] + 0.5f;
            return (x - cx) * (x - cx) + (y - cy) * (y - cy);
        }

        private float wordAccuracy(boolean mirrored) {
            if (word.isEmpty())
                return 0f;
            float sum = 0f;
            for (int i = 0; i < word.length(); i++)
                sum += letterAccuracy(i, mirrored);
            return sum / word.length();
        }

        public Result result() {
            float normal = wordAccuracy(false), mirror = wordAccuracy(true);
            return new Result(Math.max(normal, mirror), mirror > normal, points > 0);
        }
    }

    /** Replay a session's samples from {@code startTemperature}, stopping where the part locks. */
    public static Result run(Material.Attuning p, float startTemperature, String word, List<Integer> samples) {
        Pen pen = new Pen(p, startTemperature, word);
        int n = Math.min(samples.size(), ModularTuning.ENCHANT_MAX_TICKS);
        for (int i = 0; i < n; i++) {
            if (EnchantGame.locked(p, startTemperature, i))
                break;
            pen.tick(samples.get(i));
        }
        return pen.result();
    }
}
