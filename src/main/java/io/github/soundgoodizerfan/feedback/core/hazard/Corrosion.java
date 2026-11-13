// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What an acid or a base does to the vessel that holds it. A fluid whose material carries the {@code corrosive}
 * trait bites a vessel whose corrosion resistance is under {@link HazardTuning#CORROSIVE_BITE}, a little condition at a
 * time and never at once; a resistant lining takes no harm at all. Wear from the wrong vessel is misuse, not uptime.
 */
public final class Corrosion {

    private Corrosion() {
    }

    /** Whether {@code stack} is a corrosive liquid. */
    public static boolean corrosive(FluidStack stack) {
        if (stack.isEmpty())
            return false;
        Material m = MaterialTable.get().materials().get(BuiltInRegistries.FLUID.getKey(stack.getFluid()));
        return m != null && m.traits().contains(Material.Trait.CORROSIVE);
    }

    /** Condition lost over one second holding a corrosive liquid in a vessel of the given resistance, 0 if it resists. */
    public static float wearPerSecond(float resistance) {
        return resistance >= HazardTuning.CORROSIVE_BITE ? 0f
                : HazardTuning.CORROSION_WEAR_PER_SECOND * (HazardTuning.CORROSIVE_BITE - resistance) / HazardTuning.CORROSIVE_BITE;
    }
}
