// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The colours of an item's own sprite: up to eight representative opaque pixels (near-transparent and near-black outline pixels skipped), read once per
 * item and cached, so crushed fragments of a rose bush are red and green and of a fern green. NativeImage pixels are ABGR; they are turned into ARGB here.
 */
public final class CrushPalette {

    private static final int[] FALLBACK = {0xFF8FA070};
    private static final Map<Item, int[]> CACHE = new HashMap<>();

    private CrushPalette() {
    }

    /** ARGB colours, never empty. */
    public static int[] of(ItemStack stack) {
        return CACHE.computeIfAbsent(stack.getItem(), item -> read(stack));
    }

    private static int[] read(ItemStack stack) {
        Item item = stack.getItem();
        try {
            var sprite = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(item).getParticleIcon();
            var contents = sprite.contents();
            var image = contents.getOriginalImage();
            int w = contents.width(), h = contents.height(), n = 0;
            int[] found = new int[w * h];
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int p = image.getPixelRGBA(x, y), r = p & 255, g = p >> 8 & 255, b = p >> 16 & 255;
                    if (p >>> 24 >= 200 && (r * 3 + g * 6 + b) / 10 >= 45)
                        found[n++] = 0xFF000000 | r << 16 | g << 8 | b;
                }
            if (n == 0)
                return FALLBACK;
            int[] out = new int[Math.min(8, n)];
            // Biome-tinted items (grass, ferns, leaves, vines) have grey sprites that vanilla tints at render time: tint the palette the same way.
            int tint = Minecraft.getInstance().getItemColors().getColor(stack, 0);
            for (int k = 0; k < out.length; k++) {
                int c = found[k * n / out.length];
                out[k] = tint == -1 ? c : 0xFF000000 | (c >> 16 & 255) * (tint >> 16 & 255) / 255 << 16 | (c >> 8 & 255) * (tint >> 8 & 255) / 255 << 8 | (c & 255) * (tint & 255) / 255;
            }
            return out;
        } catch (RuntimeException e) {
            return FALLBACK;
        }
    }
}
