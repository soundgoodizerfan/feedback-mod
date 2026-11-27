// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.combat;

import io.github.soundgoodizerfan.feedback.combat.cast.CastState;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The cast radial's per-tree life: a small procedural overlay drawn inside a wedge, one look for each skill tree (pool).
 * Every mark is a few {@code fill}s tested against the wedge, so nothing leaks over the gaps or the neighbours and no
 * texture is needed. Coordinates are offsets from the ring's centre, the same space as the wedge runs.
 */
final class RadialFx {

    private static final float HALF = (float) Math.PI / CastState.SLOTS;
    private static final int R_LO = CastRadialScreen.R_IN + 4, R_HI = CastRadialScreen.R_OUT - 4;

    private RadialFx() {
    }

    /** Draws the overlay of {@code pool} in wedge {@code slot}; {@code t} is seconds, {@code e} the strength 0..1. */
    static void draw(GuiGraphics g, String pool, int slot, float t, float e, int rgb) {
        switch (pool) {
            case "anchor" -> anchor(g, slot, t, e, rgb);
            case "brute" -> brute(g, slot, t, e, rgb);
            case "chaos" -> chaos(g, slot, t, e, rgb);
            case "observation" -> observation(g, slot, t, e, rgb);
            case "onslaught" -> onslaught(g, slot, t, e, rgb);
            case "proficiency" -> proficiency(g, slot, t, e, rgb);
            case "veil" -> veil(g, slot, t, e, rgb);
            default -> {
            }
        }
    }

    /** Rings close in from the rim and lock against the hub. */
    private static void anchor(GuiGraphics g, int slot, float t, float e, int rgb) {
        for (int k = 0; k < 3; k++) {
            float ph = (t * 0.7f + k / 3f) % 1f, r = R_HI - ph * (R_HI - R_LO);
            int size = ph > 0.9f ? 2 : 1;
            for (float da = -HALF; da <= HALF; da += 1.2f / r)
                dot(g, slot, x(slot, r, da), y(slot, r, da), size, rgb, e * (0.3f + 0.7f * ph));
        }
    }

    /** Cracks run in from the rim, branching as they go, and fade. */
    private static void brute(GuiGraphics g, int slot, float t, float e, int rgb) {
        float cycle = t / 1.8f, grow = cycle % 1f;
        for (int k = 0; k < 3; k++) {
            int seed = slot * 31 + k * 7 + (int) cycle * 101;
            float a = (float) CastRadialScreen.angle(slot) + (rnd(seed) - 0.5f) * HALF * 1.4f;
            float px = (float) Math.cos(a) * (CastRadialScreen.R_OUT - 2), py = (float) Math.sin(a) * (CastRadialScreen.R_OUT - 2);
            float reach = grow * 10f;
            for (int i = 0; i < 7 && reach > i; i++) {
                float nx = px - (float) Math.cos(a) * 8 + (rnd(seed + i * 3 + 1) - 0.5f) * 10;
                float ny = py - (float) Math.sin(a) * 8 + (rnd(seed + i * 3 + 2) - 0.5f) * 10;
                line(g, slot, px, py, nx, ny, rgb, e * (1 - grow * grow * grow), Math.min(1f, reach - i));
                px = nx;
                py = ny;
            }
        }
    }

    /** Shards jump about in the wedge a dozen times a second. */
    private static void chaos(GuiGraphics g, int slot, float t, float e, int rgb) {
        int frame = (int) (t * 12);
        for (int k = 0; k < 9; k++) {
            int seed = frame * 13 + k + slot * 101;
            if (rnd(seed + 4) < 0.35f)
                continue;
            float r = R_LO + rnd(seed) * (R_HI - R_LO), da = (rnd(seed + 1) - 0.5f) * 2 * HALF * 0.85f;
            int w = 2 + (int) (rnd(seed + 2) * 5), h = 1 + (int) (rnd(seed + 3) * 2);
            float x = x(slot, r, da), y = y(slot, r, da);
            for (int o = 0; o < w; o++)
                dot(g, slot, x + o, y, h, k % 3 == 0 ? 0xFFFFFF : rgb, e);
        }
    }

    /** A sweep line scans the wedge with a fading trail and a reticle on its head. */
    private static void observation(GuiGraphics g, int slot, float t, float e, int rgb) {
        for (int j = 0; j < 4; j++) {
            float da = (float) Math.sin(t * 1.7f - j * 0.18f) * HALF * 0.8f, a = e * (1 - j * 0.28f);
            for (float r = R_LO; r <= R_HI; r += 1f)
                dot(g, slot, x(slot, r, da), y(slot, r, da), 1, rgb, a);
            if (j == 0) {
                float hx = x(slot, R_HI - 8, da), hy = y(slot, R_HI - 8, da);
                for (int s = -1; s <= 1; s += 2)
                    for (int u = -1; u <= 1; u += 2) {
                        dot(g, slot, hx + s * 5, hy + u * 5, 1, rgb, e);
                        dot(g, slot, hx + s * 4, hy + u * 5, 1, rgb, e);
                        dot(g, slot, hx + s * 5, hy + u * 4, 1, rgb, e);
                    }
            }
        }
    }

    /** Embers rise; now and then a pair of slash streaks cuts across. */
    private static void onslaught(GuiGraphics g, int slot, float t, float e, int rgb) {
        for (int k = 0; k < 9; k++) {
            int seed = slot * 17 + k * 5;
            float ph = (t * (0.35f + rnd(seed) * 0.3f) + rnd(seed + 1)) % 1f;
            float r = R_LO + 2 + rnd(seed + 3) * (R_HI - R_LO - 4), da = (rnd(seed + 2) - 0.5f) * 2 * HALF * 0.8f;
            dot(g, slot, x(slot, r, da) + (float) Math.sin(t * 3 + k) * 2, y(slot, r, da) + 10 - ph * 34, 1 + k % 2,
                    ph > 0.5f ? 0xFFD060 : rgb, e * (float) Math.sin(ph * Math.PI));
        }
        float cycle = t / 1.1f, lt = cycle % 1f;
        if (lt < 0.35f) {
            float frac = lt / 0.35f, cx = (float) Math.cos(CastRadialScreen.angle(slot)) * CastRadialScreen.LABEL_R,
                    cy = (float) Math.sin(CastRadialScreen.angle(slot)) * CastRadialScreen.LABEL_R;
            int flip = rnd(slot + (int) cycle * 7) < 0.5f ? 1 : -1;
            for (int s = 0; s < 2; s++)
                line(g, slot, cx - 14 + s * 3, cy - 9 * flip - s * 2, cx + 14 + s * 3, cy + 9 * flip - s * 2, 0xFFE0A0, e * (1 - frac * 0.5f), frac);
        }
    }

    /** Two meshed cogs turn; sparks twinkle between them. */
    private static void proficiency(GuiGraphics g, int slot, float t, float e, int rgb) {
        cog(g, slot, x(slot, R_LO + 10, -HALF * 0.45f), y(slot, R_LO + 10, -HALF * 0.45f), 5, 6, t * 1.2f, rgb, e);
        cog(g, slot, x(slot, R_HI - 10, HALF * 0.45f), y(slot, R_HI - 10, HALF * 0.45f), 4, 5, -t * 1.5f, rgb, e);
        for (int k = 0; k < 4; k++) {
            float cycle = t * 0.9f + rnd(slot * 9 + k), ph = cycle % 1f;
            int seed = slot * 41 + k * 11 + (int) cycle * 53;
            float r = R_LO + rnd(seed) * (R_HI - R_LO), da = (rnd(seed + 1) - 0.5f) * 2 * HALF * 0.8f;
            float sx = x(slot, r, da), sy = y(slot, r, da), a = e * (float) Math.sin(ph * Math.PI);
            dot(g, slot, sx, sy, 1, 0xFFFFFF, a);
            for (int s = -1; s <= 1; s += 2) {
                dot(g, slot, sx + s * 2, sy, 1, rgb, a);
                dot(g, slot, sx, sy + s * 2, 1, rgb, a);
            }
        }
    }

    /** Wisps of smoke drift across the wedge, thickening and thinning. */
    private static void veil(GuiGraphics g, int slot, float t, float e, int rgb) {
        for (int k = 0; k < 4; k++) {
            float ph = (t * 0.25f + k * 0.25f) % 1f, fade = (float) Math.sin(ph * Math.PI);
            for (int s = 0; s <= 24; s++) {
                float u = s / 24f, r = R_LO + 2 + u * (R_HI - R_LO - 4);
                float da = (float) Math.sin(u * 5 + t * 0.9f + k * 1.7f) * HALF * 0.45f + (k - 1.5f) * HALF * 0.25f;
                dot(g, slot, x(slot, r, da), y(slot, r, da), 1 + (s + k) % 2, rgb, e * 0.6f * fade * (1 - Math.abs(u - 0.5f) * 1.2f));
            }
        }
    }

    private static void cog(GuiGraphics g, int slot, float cx, float cy, float rad, int teeth, float rot, int rgb, float a) {
        for (int i = 0; i < 16; i++) {
            double ang = i * Math.PI / 8;
            dot(g, slot, cx + (float) Math.cos(ang) * rad, cy + (float) Math.sin(ang) * rad, 1, rgb, a * 0.7f);
        }
        for (int i = 0; i < teeth; i++) {
            double ang = rot + i * 2 * Math.PI / teeth;
            dot(g, slot, cx + (float) Math.cos(ang) * (rad + 2), cy + (float) Math.sin(ang) * (rad + 2), 2, rgb, a);
        }
    }

    private static float x(int slot, float r, float da) {
        return (float) Math.cos(CastRadialScreen.angle(slot) + da) * r;
    }

    private static float y(int slot, float r, float da) {
        return (float) Math.sin(CastRadialScreen.angle(slot) + da) * r;
    }

    /** A square mark, drawn only where it lands inside the wedge. */
    private static void dot(GuiGraphics g, int slot, float x, float y, int size, int rgb, float alpha) {
        int ix = Math.round(x), iy = Math.round(y), a = (int) (Math.min(1f, alpha) * 255);
        if (a < 6 || !CastRadialScreen.inSlot(slot, ix, iy))
            return;
        g.fill(ix, iy, ix + size, iy + size, a << 24 | rgb & 0xFFFFFF);
    }

    /** A line from one point towards another, drawn as far as {@code frac} of its length. */
    private static void line(GuiGraphics g, int slot, float x0, float y0, float x1, float y1, int rgb, float alpha, float frac) {
        int n = (int) Math.max(1, Math.hypot(x1 - x0, y1 - y0));
        for (int i = 0; i <= n * frac; i++)
            dot(g, slot, x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * i / n, 1, rgb, alpha);
    }

    /** A steady pseudo-random 0..1 from an integer, so a mark keeps its place while its seed holds. */
    private static float rnd(int n) {
        n *= 0x9E3779B1;
        n ^= n >>> 15;
        n *= 0x85EBCA6B;
        n ^= n >>> 13;
        return (n & 0xFFFF) / 65535f;
    }
}
