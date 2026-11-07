// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Nullable;

/** How fast a piece of cookware (a skillet, a stockpot) follows its fire: the reference rate scaled up by how well its material conducts and down by how dense it is, against wrought iron. */
public final class CookwareSwing {

    private CookwareSwing() {
    }

    /** {@code baseRate} is the share of the gap closed per tick by cookware of the reference material; a missing or unknown material counts as the reference. */
    public static float relax(@Nullable ResourceLocation material, float baseRate) {
        float rate = baseRate;
        Material m = material == null ? null : MaterialTable.get().materials().get(material);
        if (m != null) {
            float conduct = m.thermal().conductivity().map(c -> c.workPerTickPerTu() / FTuning.COOKWARE_REF_CONDUCTIVITY).orElse(1f);
            float density = m.density().muPerMb() > 0f ? m.density().muPerMb() / FTuning.COOKWARE_REF_DENSITY : 1f;
            rate *= conduct / density;
        }
        return Mth.clamp(rate, FTuning.COOKWARE_RELAX_MIN, FTuning.COOKWARE_RELAX_MAX);
    }
}
