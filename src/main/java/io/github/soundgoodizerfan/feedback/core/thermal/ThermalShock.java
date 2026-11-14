// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.thermal;

import io.github.soundgoodizerfan.feedback.process.Material;

/**
 * Thermal shock: a brittle stuff cracks when its temperature is made to jump faster than the heat can even out through it. The
 * material's {@code thermal.shock_limit} is the jump it takes without harm; a jump over it crazes the thing, by how many times the
 * limit it overshot. Nothing under the limit costs anything, so a part that is never hit with a sudden change is never marked.
 */
public final class ThermalShock {

    private ThermalShock() {
    }

    /**
     * How many times the material's shock limit a sudden jump of {@code jumpTu} overshoots it, 0 when it is within it or the material
     * is not rated. A part dipped hot in cold water has jumped by the whole difference between them.
     */
    public static float overshoot(Material material, float jumpTu) {
        if (material == null)
            return 0f;
        float limit = material.thermal().shockLimit().map(io.github.soundgoodizerfan.feedback.core.unit.Tu::value).orElse(0f);
        return limit <= 0f ? 0f : Math.max(0f, (Math.abs(jumpTu) - limit) / limit);
    }
}
