// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;

/**
 * Wooden gun parts drawn with their own wood: the side of the material's log block (bark), copied
 * once per log into a texture of its own with the contrast taken down, so a stock reads as timber
 * without the log's bold stripes. Any wood a datapack gives a {@code *_log} form works with no art.
 */
final class GunWood {

    /** Texels across the copy, a block texture's width. */
    static final int SIZE = 16;
    /** How far each texel is pulled toward the texture's average colour. */
    private static final float FLATTEN = 0.5f;

    /** Log item id to its copy's texture id; absent from the map until first asked. */
    private static final Map<ResourceLocation, ResourceLocation> MADE = new HashMap<>();

    private GunWood() {
    }

    /**
     * The bark texture for a part of {@code material} shaped from {@code form}: the log of the form's
     * species if the material has one (an {@code oak_planks} part takes {@code oak_log}), else the
     * material's first log. Null when the material has no log.
     */
    static ResourceLocation texture(Material material, String form) {
        if (material == null)
            return null;
        ResourceLocation log = null;
        int cut = form.lastIndexOf('_');
        if (cut > 0)
            log = material.forms().get(form.substring(0, cut) + "_log");
        if (log == null)
            log = material.forms().get("oak_log");
        if (log == null)
            log = material.forms().keySet().stream().sorted()
                    .filter(k -> k.endsWith("_log") && !k.startsWith("stripped")).findFirst()
                    .map(material.forms()::get).orElse(null);
        if (log == null)
            return null;
        // shortcut: copies live until the game closes, so a resource-pack reload keeps the old bark; clear MADE on reload if that matters.
        if (!MADE.containsKey(log))
            MADE.put(log, copy(log));
        return MADE.get(log);
    }

    private static ResourceLocation copy(ResourceLocation log) {
        if (!(BuiltInRegistries.ITEM.get(log) instanceof BlockItem item))
            return null;
        var state = item.getBlock().defaultBlockState();
        var model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        var quads = model.getQuads(state, Direction.NORTH, RandomSource.create(0));
        TextureAtlasSprite side = quads.isEmpty() ? model.getParticleIcon() : quads.get(0).getSprite();
        NativeImage from = side.contents().getOriginalImage();
        int w = Math.min(SIZE, side.contents().width()), h = Math.min(SIZE, side.contents().height());
        long[] sum = new long[4];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int c = from.getPixelRGBA(x, y);
                for (int k = 0; k < 4; k++)
                    sum[k] += c >> (k * 8) & 0xFF;
            }
        NativeImage out = new NativeImage(SIZE, SIZE, false);
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++) {
                int c = from.getPixelRGBA(x % w, y % h), flat = 0;
                for (int k = 0; k < 4; k++) {
                    // Channel order does not matter: each is pulled toward its own mean; alpha is made opaque.
                    float mean = sum[k] / (float) (w * h);
                    int ch = c >> (k * 8) & 0xFF;
                    int v = k == 3 ? 255 : Math.round(ch + (mean - ch) * FLATTEN);
                    flat |= (v & 0xFF) << (k * 8);
                }
                out.setPixelRGBA(x, y, flat);
            }
        ResourceLocation id = Feedback.id("gun_wood/" + log.getNamespace() + "/" + log.getPath());
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(out));
        return id;
    }
}
