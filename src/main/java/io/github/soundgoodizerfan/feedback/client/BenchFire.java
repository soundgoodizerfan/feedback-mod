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

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The bench's background fire, driven by the heat of the stock. A small image (192 x 96) is rewritten each frame from a
 * scrolling, domain-warped fractal noise shaped by a flame profile that rises from the bottom edge, mapped through a
 * black-body ramp (deep red, orange, yellow, white-hot), and drawn stretched with linear filtering so it reads as one soft
 * flame rather than blocks. Stateless: the picture is a pure function of time, position and heat, so nothing is stored but
 * the one texture. No stock or cold stock draws nothing; heat makes the fire taller, brighter and whiter.
 */
final class BenchFire {

    private static final int TW = 192, TH = 96;
    private static final ResourceLocation TEXTURE = Feedback.id("bench_fire");
    private static DynamicTexture texture;

    private BenchFire() { }

    /** A fire colour for heat 0..1: dull red, orange, yellow, then white-hot. */
    static int colour(float heat) {
        int r = 0x70 + (int) (0x8F * Mth.clamp(heat * 2f, 0f, 1f));
        int gr = (int) (0xB0 * Mth.clamp(heat * 1.5f - 0.15f, 0f, 1f));
        int b = (int) (0xC0 * Mth.clamp(heat * 2.4f - 1.3f, 0f, 1f));
        return r << 16 | gr << 8 | b;
    }

    private static float hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }

    private static float noise(float x, float y) {
        int xi = Mth.floor(x), yi = Mth.floor(y);
        float fx = x - xi, fy = y - yi;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        float top = Mth.lerp(fx, hash(xi, yi), hash(xi + 1, yi));
        float bottom = Mth.lerp(fx, hash(xi, yi + 1), hash(xi + 1, yi + 1));
        return Mth.lerp(fy, top, bottom);
    }

    private static float fbm(float x, float y) {
        return noise(x, y) * 0.55f + noise(x * 2.1f + 7.3f, y * 2.1f) * 0.3f + noise(x * 4.3f + 3.1f, y * 4.3f) * 0.15f;
    }

    /** The ramp: v 0..1 of fire intensity to ABGR, transparent where there is no flame. */
    private static int ramp(float v) {
        if (v <= 0.04f)
            return 0;
        float a = Mth.clamp((v - 0.04f) / 0.26f, 0f, 1f);
        a = a * a * (3 - 2 * a);
        float r = Mth.clamp(v * 3.2f, 0f, 1f);
        float g = Mth.clamp(v * 2.6f - 0.7f, 0f, 1f);
        float b = Mth.clamp(v * 3.6f - 2.3f, 0f, 1f);
        return (int) (a * 235) << 24 | (int) (b * 255) << 16 | (int) (g * 255) << 8 | (int) (r * 255);
    }

    private static void paint(float t, float h0) {
        NativeImage img = texture.getPixels();
        float reach = 0.1f + 0.8f * h0; // fraction of the height the flame can climb
        float fierce = 0.12f + 0.95f * h0; // overall intensity: a warm bar is a dull red glow, white only near working heat
        for (int j = 0; j < TH; j++) {
            float ny = 1f - (j + 0.5f) / TH; // 0 at the bottom edge, 1 at the top
            float rise = ny - t * 0.045f;
            float profile = Mth.clamp(1f - ny / reach, 0f, 1f);
            for (int i = 0; i < TW; i++) {
                float nx = (i + 0.5f) / TW;
                float warp = 0.12f * Mth.sin(ny * 7f - t * 0.16f + nx * 5f);
                float n = fbm((nx + warp) * 6f, rise * 4.2f);
                // Tongues: strongest mid-bench, thinning toward the sides, breaking up as they climb.
                float side = 1f - Math.abs(nx - 0.5f) * 1.1f;
                float v = (profile * (0.45f + 1.15f * n) * side - 0.1f * ny) * fierce;
                // The bed: always a few hot pixels along the bottom.
                v = Math.max(v, (1f - ny * 9f) * (0.3f + 0.2f * n) * fierce);
                img.setPixelRGBA(i, j, ramp(v));
            }
        }
        texture.upload();
    }

    /**
     * Draw the fire in the rectangle (x, y, w, h), burning up from its bottom edge. {@code heat} is 0..1 of the stock's working range; a cold
     * bench draws nothing.
     */
    static void draw(GuiGraphics g, int x, int y, int w, int h, float heat) {
        boolean still = ClientSettings.reduceMotion();
        float t = still ? 0f : GuiStyle.time();
        float h0 = Mth.clamp(heat, 0f, 1f);
        if (texture == null) {
            texture = new DynamicTexture(new NativeImage(NativeImage.Format.RGBA, TW, TH, true));
            texture.setFilter(true, false);
            Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
            texture.setFilter(true, false);
        }
        if (h0 < 0.03f) // cold stock, or none: no fire
            return;
        // The glow cast on the whole bench.
        int glow = (int) (0x50 * h0);
        g.fillGradient(x, y + h / 3, x + w, y + h, 0, glow << 24 | colour(h0));
        paint(t, h0);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEXTURE, x, y, w, h, 0f, 0f, TW, TH, TW, TH);
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
