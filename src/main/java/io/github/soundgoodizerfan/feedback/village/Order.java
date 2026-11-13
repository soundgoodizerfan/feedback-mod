// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;

import net.minecraft.resources.ResourceLocation;

/**
 * One commission as the server holds it: a modular type and terms on its figures, stated and
 * hidden. Derived from the villager and the day by {@link OrderGen}, never stored.
 *
 * @param pay    emeralds for a Good delivery, before saturation
 * @param terms  what the villager says
 * @param quirks what the villager does not say; only the complaint on a miss ever shows one
 * @param leans  what the villager's home biome leans toward: knowable from where they live, never printed
 * @param asks   a tool order's stated parts, each a material or a class of material; empty for a dish, which is stated on figures
 */
public record Order(ResourceLocation type, int pay, List<Term> terms, List<Term> quirks, List<Term> leans, List<Ask> asks) {

    /**
     * A tool order's request for one part: a material, or a class of material (its kind).
     *
     * @param role     the part's role key in the type
     * @param material the material wanted, or null when a class is asked
     * @param kind     the class wanted, or null when one material is asked
     */
    public record Ask(String role, ResourceLocation material, io.github.soundgoodizerfan.feedback.process.Material.Kind kind) {
    }

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
