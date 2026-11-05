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
package io.github.soundgoodizerfan.feedback.village;

import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;

import net.minecraft.resources.ResourceLocation;

/**
 * One commission as the server holds it: a modular type and terms on its figures, stated and
 * hidden. Derived from the villager and the day by {@link OrderGen}, never stored.
 *
 * @param pay    emeralds for a Good delivery, before saturation
 * @param terms  what the villager says
 * @param quirks what the villager does not say; only the complaint on a miss ever shows one
 * @param leans  what the villager's home biome leans toward: knowable from where they live, never printed
 */
public record Order(ResourceLocation type, int pay, List<Term> terms, List<Term> quirks, List<Term> leans) {

    /**
     * One term on one figure. A must is a test (the figure at least, or at most, the target); a
     * band wants the target within the tolerance.
     *
     * @param lo     bottom of the span the target was drawn from
     * @param hi     top of that span
     * @param tol    half-width of the band as a share of the span; unused by a must
     * @param above  for a must, whether the figure must be at least the target (else at most)
     */
    public record Term(Fig fig, float lo, float hi, float target, float tol, boolean must, boolean above) {

        public float range() {
            return Math.max(1e-3f, hi - lo);
        }

        /** Where the target sits in its span, 0 to 1. */
        public float frac() {
            return Math.max(0f, Math.min(1f, (target - lo) / range()));
        }

        /** The target's quarter of the span: 0 very low to 3 very high, the word the client picks. */
        public int band() {
            return Math.min(3, (int) (frac() * 4f));
        }
    }
}
