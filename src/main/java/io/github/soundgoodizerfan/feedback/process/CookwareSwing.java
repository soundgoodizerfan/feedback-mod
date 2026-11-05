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
