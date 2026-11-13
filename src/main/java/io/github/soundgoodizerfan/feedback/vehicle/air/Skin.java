// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.air;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning;

import net.minecraft.resources.ResourceLocation;

/**
 * What a gas cell is wrapped in: how far it stretches before it weeps, whether it burns with its gas, and the
 * material whose {@code mechanical.permeability} says how much gas it passes at rest (absent is gas-tight).
 */
public enum Skin {
    CLOTH(VehicleTuning.GAS_STRETCH_CLOTH, true, "wool"),
    RUBBER(VehicleTuning.GAS_STRETCH_RUBBER, true, "synthetic_rubber"),
    PLATE(VehicleTuning.GAS_STRETCH_PLATE, false, "aluminium"),
    /** A ghast's bladder: the lightest skin, gas-tight and stretching further than rubber. */
    BLADDER(VehicleTuning.GAS_STRETCH_BLADDER, true, "ghast_bladder"),
    /** Cloth coated in slime or rubber: heavier than a bladder, and it seeps. */
    MEMBRANE(VehicleTuning.GAS_STRETCH_MEMBRANE, true, "coated_cloth");

    /** Gas volume over the skin's rest volume past which it leaks. */
    public final float maxStretch;
    /** Whether the skin is lost when its gas burns out. */
    public final boolean burns;
    private final ResourceLocation material;

    Skin(float maxStretch, boolean burns, String material) {
        this.maxStretch = maxStretch;
        this.burns = burns;
        this.material = Feedback.id(material);
    }

    /** mB a tick a full cell of this skin passes at rest, read from its material. */
    public float seep() {
        var m = MaterialTable.get().materials().get(material);
        return m == null ? 0f : m.mechanical().permeability().orElse(0f);
    }
}
