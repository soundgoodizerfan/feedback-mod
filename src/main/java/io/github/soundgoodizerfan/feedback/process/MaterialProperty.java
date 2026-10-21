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
package io.github.soundgoodizerfan.feedback.process;

import java.util.Optional;
import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * A number a material's file states that a recipe may ask about: "any wire whose {@code ampacity}
 * is at least 0.5". Named, never an id (philosophy 11), and a closed list on purpose -- each is a
 * figure the table already carries, read through the same accessor the machines use. Absent means
 * unmeasured, never zero (mechanics 5.6), so a material without the figure matches no range.
 */
public enum MaterialProperty implements StringRepresentable {
    AMPACITY("ampacity", "ampacity", m -> m.ampacity()),
    HARDNESS("hardness", "hardness", m -> Optional.of(m.hardness().value())),
    DENSITY("density", "density", m -> Optional.of(m.density().muPerMb())),
    MELTING_POINT("melting_point", "melting point", m -> m.thermal().meltingPoint().map(t -> t.value())),
    TOUGHNESS("toughness", "toughness", m -> m.mechanical().toughness().map(t -> t.value())),
    YIELD_STRENGTH("yield_strength", "yield strength", m -> m.mechanical().yieldStrength().map(t -> t.value()));

    public static final Codec<MaterialProperty> CODEC = StringRepresentable.fromEnum(MaterialProperty::values);

    private final String key;
    private final String label;
    private final Function<Material, Optional<Float>> reader;

    MaterialProperty(String key, String label, Function<Material, Optional<Float>> reader) {
        this.key = key;
        this.label = label;
        this.reader = reader;
    }

    public Optional<Float> of(Material material) {
        return reader.apply(material);
    }

    /** The name a tooltip uses for it. */
    public String label() {
        return label;
    }

    @Override
    public String getSerializedName() {
        return key;
    }
}
