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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.resources.ResourceLocation;

/**
 * The tint a part is drawn in: one grey sprite per role, coloured by the material it is made of
 * (Tinkers' {@code ToolModel} does the same with a sprite per part -- the technique, not its
 * assets). The colour is the material file's {@code color}. A material without one takes the
 * average of its ingot's or block's texture, sampled by the client through {@link #sampler}, and
 * failing that its kind's grey. Everything is placeholder like the rest of the art.
 */
public final class MaterialColors {

    private MaterialColors() {
    }

    /**
     * Average RGB of an item's texture, or empty. Set by the client; the server never draws, so
     * on a dedicated server it stays null and the kind's grey is the answer.
     */
    public static volatile Function<ResourceLocation, OptionalInt> sampler;

    private static final Map<ResourceLocation, Integer> CACHE = new ConcurrentHashMap<>();

    /** Forget sampled colours: a resource reload may have changed a texture or a material. */
    public static void clear() {
        CACHE.clear();
    }

    /** RGB, no alpha. */
    public static int of(ResourceLocation material) {
        Integer cached = CACHE.get(material);
        if (cached != null)
            return cached;
        Material m = ClientMaterials.get().get(material);
        if (m == null)
            m = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials().get(material);
        if (m == null)
            m = io.github.soundgoodizerfan.feedback.registry.FormItems.materials().get(material);
        if (m == null)
            return KIND_FALLBACK;
        int rgb = m.color().isPresent() ? m.color().get() : derive(m);
        CACHE.put(material, rgb);
        return rgb;
    }

    private static final int KIND_FALLBACK = 0xC0C0C0;

    /** Texture of the ingot, then the block, then the first form that is not ours; else the kind's colour. */
    private static int derive(Material m) {
        Function<ResourceLocation, OptionalInt> sample = sampler;
        if (sample != null) {
            List<ResourceLocation> items = new ArrayList<>();
            for (String form : List.of("ingot", "block"))
                if (m.forms().containsKey(form))
                    items.add(m.forms().get(form));
            m.forms().values().forEach(items::add);
            for (ResourceLocation item : items) {
                if (item.getNamespace().equals("feedback"))
                    continue; // generated forms are one grey sprite, tinted by this very colour
                OptionalInt rgb = sample.apply(item);
                if (rgb.isPresent())
                    return rgb.getAsInt();
            }
        }
        return m.kind().isEmpty() ? KIND_FALLBACK : switch (m.kind().get()) {
            case METAL -> 0xB0B0B0;
            case ORGANIC -> 0x8F6B3C;
            case MINERAL -> 0x808080;
            case CRYSTAL -> 0x9FE0E8;
            default -> KIND_FALLBACK;
        };
    }
}
