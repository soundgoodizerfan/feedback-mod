// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.air;

import io.github.soundgoodizerfan.feedback.core.FTuning;

/** What a gas cell is wrapped in: how far it stretches before it weeps, and whether it burns with its gas. */
public enum Skin {
    CLOTH(FTuning.GAS_STRETCH_CLOTH, true),
    RUBBER(FTuning.GAS_STRETCH_RUBBER, true),
    PLATE(FTuning.GAS_STRETCH_PLATE, false);

    /** Gas volume over the skin's rest volume past which it leaks. */
    public final float maxStretch;
    /** Whether the skin is lost when its gas burns out. */
    public final boolean burns;

    Skin(float maxStretch, boolean burns) {
        this.maxStretch = maxStretch;
        this.burns = burns;
    }
}
