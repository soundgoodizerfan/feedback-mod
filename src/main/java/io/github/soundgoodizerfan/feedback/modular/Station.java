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

import java.util.Locale;

import com.mojang.serialization.Codec;

import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.util.StringRepresentable;

/**
 * Which bench a part is made at ({@code docs/tool_shop_rework.md}). The Tool Table takes any stock; the three others split part
 * making by what the stock is made of, read off the material's kind and traits and never its name, so a pack's own wood or
 * stone is handled the same. Only the smithing table plays the forging pattern.
 */
public enum Station implements StringRepresentable {
    TOOL, SMITHING, WOODWORKING, STONECUTTING;

    public static final Codec<Station> CODEC = StringRepresentable.fromEnum(Station::values);

    /** Whether stock of {@code m} may be shaped here. */
    public boolean accepts(Material m) {
        return switch (this) {
            case TOOL -> true;
            case SMITHING -> m.kind().filter(k -> k == Material.Kind.METAL).isPresent() || m.minTemperature().value() > 0;
            case WOODWORKING -> m.traits().contains(Material.Trait.WOODY);
            case STONECUTTING -> m.kind().filter(k -> k == Material.Kind.MINERAL).isPresent();
        };
    }

    /** Whether a shape that is forged (a rhythm played) can be made here. */
    public boolean forges() {
        return this == TOOL || this == SMITHING;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
