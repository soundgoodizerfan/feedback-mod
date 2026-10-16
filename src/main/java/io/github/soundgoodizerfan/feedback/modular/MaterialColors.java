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

import java.util.Map;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.resources.ResourceLocation;

/**
 * The tint a part is drawn in: one grey sprite per role, coloured by the material it is made of
 * (Tinkers' {@code ToolModel} does the same with a sprite per part -- the technique, not its
 * assets). Art, not a stat, so it is a plain table by material path; a material not listed takes
 * its kind's colour, and everything is placeholder like the rest of the art.
 */
public final class MaterialColors {

    private MaterialColors() {
    }

    private static final Map<String, Integer> BY_PATH = Map.ofEntries(
            Map.entry("iron", 0xD8D8D8), Map.entry("wood", 0x8F6B3C), Map.entry("stone", 0x7F7F7F),
            Map.entry("gold", 0xFCEE4B), Map.entry("diamond", 0x4AEDD9), Map.entry("netherite", 0x4A3F48),
            Map.entry("copper", 0xC57B5C), Map.entry("steel", 0x9AA3AD), Map.entry("hardened_steel", 0x6E7783),
            Map.entry("bronze", 0xB8864B), Map.entry("tin", 0xD6DEE0), Map.entry("zinc", 0x9DB0C4),
            Map.entry("lead", 0x6A6F8A), Map.entry("brass", 0xD9B53F), Map.entry("cast_iron", 0x55575C),
            Map.entry("leather", 0xA0602E), Map.entry("bone", 0xE8E6D0), Map.entry("wool", 0xEDEDED),
            Map.entry("deepslate", 0x4B4B52), Map.entry("heavy_core", 0x3C4A55), Map.entry("prismarine", 0x5FB3A5),
            Map.entry("blaze_rod", 0xE8A22B), Map.entry("breeze_rod", 0x9FB8E8), Map.entry("bamboo", 0xB5A64A),
            Map.entry("amethyst", 0xA77BDB), Map.entry("emerald", 0x2ECF6A), Map.entry("quartz", 0xEDE6DD),
            Map.entry("glass", 0xBFE6F2), Map.entry("obsidian", 0x1B1426));

    /** RGB, no alpha. */
    public static int of(ResourceLocation material) {
        Integer known = BY_PATH.get(material.getPath());
        if (known != null)
            return known;
        Material m = ClientMaterials.get().get(material);
        return m == null || m.kind().isEmpty() ? 0xC0C0C0 : switch (m.kind().get()) {
            case METAL -> 0xB0B0B0;
            case ORGANIC -> 0x8F6B3C;
            case MINERAL -> 0x808080;
            case CRYSTAL -> 0x9FE0E8;
            default -> 0xC0C0C0;
        };
    }
}
