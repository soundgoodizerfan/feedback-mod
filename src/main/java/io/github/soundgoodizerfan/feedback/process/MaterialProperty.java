// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.Optional;
import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * A number a material's file states that a recipe may ask about: "any wire whose {@code ampacity}
 * is at least 0.5". Named, never an id (philosophy 12), and a closed list on purpose -- each is a
 * figure the table already carries, read through the same accessor the machines use. Absent means
 * unmeasured, never zero (mechanics 5.6), so a material without the figure matches no range.
 */
public enum MaterialProperty implements StringRepresentable {
    AMPACITY("ampacity", "ampacity", m -> m.ampacity()),
    HARDNESS("hardness", "hardness", m -> Optional.of(m.hardness().value())),
    DENSITY("density", "density", m -> Optional.of(m.density().muPerMb())),
    MELTING_POINT("melting_point", "melting point", m -> m.thermal().meltingPoint().map(t -> t.value())),
    TOUGHNESS("toughness", "toughness", m -> m.mechanical().toughness().map(t -> t.value())),
    YIELD_STRENGTH("yield_strength", "yield strength", m -> m.mechanical().yieldStrength().map(t -> t.value())),
    PIEZO_COUPLING("piezo_coupling", "piezoelectric coupling", m -> m.electrical().piezoCoupling()),
    RECTIFYING("rectifying", "rectifying spot quality", m -> m.electrical().rectifying());

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
