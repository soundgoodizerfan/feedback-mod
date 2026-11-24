// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * How soft a telescope's view is right now. The baked blur is for a tube at the Overworld's ambient; a tube warmer or colder (the
 * item's own heat, else the dimension's ambient) is longer or shorter by its expansion times the difference, so the lenses sit
 * off their focus and the view blurs more. A steel tube is soft in the Nether and the Gap, invar and silica glass hold.
 */
public final class TelescopeFocus {

    private TelescopeFocus() {
    }

    /** The blur of {@code telescope} in {@code level}, 0 (sharp) to 1 (smudge). */
    public static float blur(ItemStack telescope, Level level) {
        ModularFigures f = ModularStacks.figures(telescope);
        if (f == null)
            return 0f;
        float drift = Math.abs(f.get(Fig.TUBE_EXPANSION)) * Math.abs(ItemHeat.get(telescope, level).value() - ThermalTuning.AMBIENT_TU.value());
        return Math.min(ModularTuning.TELESCOPE_BLUR_MAX, f.get(Fig.TELESCOPE_BLUR) + drift * ModularTuning.TELESCOPE_EXPANSION_GAIN * ModularTuning.TELESCOPE_MISMATCH_BLUR);
    }
}
