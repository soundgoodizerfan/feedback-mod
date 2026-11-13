// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.bench;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.thermal.Incandescence;
import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.render.GlowColours;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The bench's background fire, driven by the temperature of the stock. A tiny image (56 x 28) is rewritten each frame from
 * scrolling value noise cut into three hard tones (outer, body, core) and drawn stretched with nearest filtering, so the flame
 * reads as chunky pixel art. Its colour follows the stock's heat band: dull ember red, then the glow ramp
 * ({@link GlowColours}) through orange and yellow to white, then soul-fire blue at the top of the visible scale. A halo of the
 * same colour is drawn just outside the box ({@link #halo}). Stateless: a pure function of time, position and temperature.
 * No stock or cold stock draws nothing.
 */
final class BenchFire {

    private static final int TW = 56, TH = 28;
    private static final ResourceLocation TEXTURE = Feedback.id("bench_fire");
    private static DynamicTexture texture;

    private BenchFire() { }

    /** The flame's base colour at {@code tu}, 0xRRGGBB: ember red below the faint-red band, the glow ramp above it, soul-fire blue past white. */
    static int colour(float tu) {
        float[] c = GlowColours.rgb(GlowColours.along(Incandescence.light(tu)));
        float blue = Mth.clamp((tu - 1450f) / (ThermalTuning.MAX_VISIBLE_TU.value() - 1450f), 0f, 1f);
        int r = Math.round(Mth.lerp(blue, c[0], 0.2f) * 255f);
        int gr = Math.round(Mth.lerp(blue, c[1], 0.7f) * 255f);
        int b = Math.round(Mth.lerp(blue, c[2], 1f) * 255f);
        return r << 16 | gr << 8 | b;
    }

    /** {@code rgb} scaled toward black by {@code dark} or toward white by {@code light}, as ABGR with alpha 255. */
    private static int tone(int rgb, float dark, float light) {
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        r = Math.round(Mth.lerp(light, r * dark, 255f));
        g = Math.round(Mth.lerp(light, g * dark, 255f));
        b = Math.round(Mth.lerp(light, b * dark, 255f));
        return 0xFF000000 | b << 16 | g << 8 | r;
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

    private static void paint(float t, float h0, int rgb) {
        NativeImage img = texture.getPixels();
        int outer = tone(rgb, 0.55f, 0f), body = tone(rgb, 1f, 0f), core = tone(rgb, 1f, 0.55f);
        float reach = 0.15f + 0.75f * h0; // fraction of the height the flame can climb
        float fierce = 0.35f + 0.8f * h0;
        for (int j = 0; j < TH; j++) {
            float ny = 1f - (j + 0.5f) / TH; // 0 at the bottom edge, 1 at the top
            float profile = Mth.clamp(1f - ny / reach, 0f, 1f);
            for (int i = 0; i < TW; i++) {
                float nx = (i + 0.5f) / TW;
                float n = noise(nx * 9f, (ny - t * 0.03f) * 5f);
                float side = 1f - Math.abs(nx - 0.5f) * 0.9f;
                float v = profile * (0.4f + 0.9f * n) * side * fierce;
                v = Math.max(v, (1f - ny * 7f) * 0.55f * fierce); // the bed along the bottom
                img.setPixelRGBA(i, j, v > 0.62f ? core : v > 0.4f ? body : v > 0.2f ? outer : 0);
            }
        }
        texture.upload();
    }

    /**
     * The glow the fire casts just outside the box (x, y, w, h): four strips fading outward, in the flame's colour. Draw it before
     * the box's own fill and outside any scissor, so it bleeds past the edge.
     */
    static void halo(GuiGraphics g, int x, int y, int w, int h, float tu) {
        float h0 = Mth.clamp(tu / 1100f, 0f, 1f);
        if (h0 < 0.03f)
            return;
        int rgb = colour(tu), d = 6;
        for (int k = 1; k <= d; k++) { // rings one pixel thick, fading outward
            float f = 1f - k / (d + 1f);
            int c = (int) (0x60 * h0 * f * f) << 24 | rgb;
            g.fill(x - k, y - k, x + w + k, y - k + 1, c);
            g.fill(x - k, y + h + k - 1, x + w + k, y + h + k, c);
            g.fill(x - k, y - k + 1, x - k + 1, y + h + k - 1, c);
            g.fill(x + w + k - 1, y - k + 1, x + w + k, y + h + k - 1, c);
        }
    }

    /**
     * Draw the fire in the rectangle (x, y, w, h), burning up from its bottom edge. {@code tu} is the stock's temperature; a cold bench draws nothing.
     */
    static void draw(GuiGraphics g, int x, int y, int w, int h, float tu) {
        boolean still = ClientSettings.reduceMotion();
        float t = still ? 0f : GuiStyle.time();
        float h0 = Mth.clamp(tu / 1100f, 0f, 1f);
        int rgb = colour(tu);
        if (texture == null) {
            texture = new DynamicTexture(new NativeImage(NativeImage.Format.RGBA, TW, TH, true));
            texture.setFilter(false, false);
            Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
        }
        if (h0 < 0.03f) // cold stock, or none: no fire
            return;
        // The glow cast on the whole bench.
        int glow = (int) (0x50 * h0);
        g.fillGradient(x, y + h / 3, x + w, y + h, 0, glow << 24 | rgb);
        paint(t, h0, rgb);
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
                    g.fill(px, py, px + 1 + (i & 1), py + 1 + (i & 1), a << 24 | (rgb | 0x808080));
            }
        }
    }
}
