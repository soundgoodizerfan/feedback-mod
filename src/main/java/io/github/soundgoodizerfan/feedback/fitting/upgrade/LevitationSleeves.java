// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting.upgrade;

import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.machine.electromagnet.MagnetField;
import io.github.soundgoodizerfan.feedback.machine.levitation.LevitationTuning;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** What a fitted levitation sleeve does to its holder: the one reader the shaft and the rotary bearing share. */
public final class LevitationSleeves {

    private LevitationSleeves() {
    }

    /** The sleeve a holder wears, if any. */
    @Nullable
    public static LevitationSleeveFitting of(Fittable holder) {
        for (UpgradeFitting u : holder.getUpgrades())
            if (u instanceof LevitationSleeveFitting s)
                return s;
        return null;
    }

    /**
     * The share of the holder's drag the sleeve removes, 0 to {@link LevitationTuning#SLEEVE_DRAG_CUT}: the sleeve material's
     * diamagnetism times the strongest powered electromagnet touching the holder. Nothing is looked up for a holder with no sleeve.
     */
    public static float cut(@Nullable Level level, BlockPos pos, Fittable holder) {
        LevitationSleeveFitting sleeve = of(holder);
        if (sleeve == null || level == null)
            return 0f;
        return LevitationTuning.SLEEVE_DRAG_CUT * sleeve.diamagnetism() * MagnetField.touching(level, pos);
    }
}
