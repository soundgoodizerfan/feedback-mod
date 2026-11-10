// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.air;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

/** What a lift gas weighs and whether it burns, read from its material: a gas cell never names a gas. */
public final class Gases {
    private static final ResourceLocation AIR = ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, "air");
    private static final float AIR_FALLBACK = 0.001f;

    private Gases() {}

    private static Material of(FluidStack gas) {
        return gas.isEmpty() ? null : MaterialTable.get().materials().get(BuiltInRegistries.FLUID.getKey(gas.getFluid()));
    }

    /** Mass of one mB of the gas, in Mu. */
    public static float muPerMb(FluidStack gas) {
        Material m = of(gas);
        return m == null || m.density().muPerMb() <= 0f ? AIR_FALLBACK : m.density().muPerMb();
    }

    /** Mass of one mB of sea-level air, in Mu. */
    public static float airMuPerMb() {
        Material m = MaterialTable.get().materials().get(AIR);
        return m == null || m.density().muPerMb() <= 0f ? AIR_FALLBACK : m.density().muPerMb();
    }

    /** Ticks the gas takes to burn out, or 0 when it does not burn. */
    public static int burnTicks(FluidStack gas) {
        Material m = of(gas);
        if (m == null || m.combustion().isEmpty() || m.combustion().get().flameTemperature().isEmpty())
            return 0;
        return m.combustion().get().burnTicks().orElse(100);
    }

    /** Whether the fluid is a gas worth holding: anything lighter than a bucket of water. */
    public static boolean isGas(FluidStack gas) {
        Material m = of(gas);
        return m != null && m.kind().orElse(null) == Material.Kind.FLUID && m.density().muPerMb() < 0.5f;
    }
}
