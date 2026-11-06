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

import java.util.Map;

import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.resources.ResourceLocation;

/**
 * Grades a delivered item against an {@link Order}, on the item's true figures. A pure function:
 * no villager, no world. A band scores full inside half its width and falls to nothing at two and
 * a half; a must scores full or falls away over {@link VillageTuning#MUST_SLACK} of its span, and
 * a failed must also costs a class. The closeness is the weighted mean, and the single worst miss
 * is what the villager complains about.
 */
public final class CommissionJudge {

    private CommissionJudge() {
    }

    /** Rejected to Exceptional. */
    public enum Grade {
        REJECTED, POOR, FAIR, GOOD, EXCEPTIONAL;

        public float payFactor() {
            return VillageTuning.CLASS_PAY[ordinal()];
        }

        public int xp() {
            return VillageTuning.CLASS_XP[ordinal()];
        }
    }

    /**
     * @param worst     the figure of the biggest miss, or null if there was none worth naming
     * @param worstHigh whether that figure came out above what the villager wanted
     * @param worstAsk  index of the part ask of the biggest miss, or -1; when set, {@code worst} is null
     */
    public record Result(float closeness, Grade grade, boolean mustFailed, Fig worst, boolean worstHigh, int worstAsk) {
    }

    /**
     * How well {@code got} answers what the order asked of one part: 1 for the named material or a material of the named class;
     * for a missed material, its likeness (same kind, near hardness and density) capped at {@link VillageTuning#SUBSTITUTE_CAP};
     * 0 for a missed class or an absent part.
     */
    static float score(Order.Ask ask, Material got, ResourceLocation gotId, Material wanted) {
        if (got == null)
            return 0f;
        if (ask.kind() != null)
            return got.kind().filter(ask.kind()::equals).isPresent() ? 1f : 0f;
        if (ask.material().equals(gotId))
            return 1f;
        if (wanted == null)
            return 0f;
        float kind = got.kind().isPresent() && got.kind().equals(wanted.kind()) ? 0.5f : 0f;
        float apart = (logGap(got.hardness().value(), wanted.hardness().value()) + logGap(got.density().muPerMb(), wanted.density().muPerMb())) / 2f;
        return VillageTuning.SUBSTITUTE_CAP * (kind + 0.5f * Math.max(0f, 1f - apart / (float) Math.log(4)));
    }

    private static float logGap(float a, float b) {
        return a <= 0f || b <= 0f ? (a == b ? 0f : (float) Math.log(4)) : (float) Math.abs(Math.log(a / b));
    }

    public static Result judge(Order order, ModularFigures figures, ModularDesign design, Map<ResourceLocation, Material> materials) {
        float sum = 0, weights = 0, worstMiss = 0;
        boolean mustFailed = false, worstHigh = false;
        Fig worst = null;
        int worstAsk = -1;
        for (int i = 0; i < order.asks().size(); i++) {
            Order.Ask a = order.asks().get(i);
            ModularDesign.Part part = design == null ? null : design.part(a.role());
            float score = score(a, part == null ? null : materials.get(part.material()), part == null ? null : part.material(),
                    a.material() == null ? null : materials.get(a.material()));
            sum += score;
            weights += 1f;
            if (1f - score > worstMiss) {
                worstMiss = 1f - score;
                worstAsk = i;
            }
        }
        for (int pass = 0; pass < 3; pass++) {
            float w = pass == 0 ? 1f : pass == 1 ? VillageTuning.LEAN_WEIGHT : VillageTuning.QUIRK_WEIGHT;
            for (Order.Term t : pass == 0 ? order.terms() : pass == 1 ? order.leans() : order.quirks()) {
                float v = figures.get(t.fig());
                float score;
                boolean high = v > t.target();
                if (t.must()) {
                    float short_ = t.above() ? t.target() - v : v - t.target();
                    boolean met = short_ <= 0;
                    mustFailed |= !met;
                    score = met ? 1f : Math.max(0f, 1f - short_ / t.range() / VillageTuning.MUST_SLACK);
                    high = !t.above();
                } else {
                    float dev = Math.abs(v - t.target()) / t.range();
                    score = dev <= t.tol() * 0.5f ? 1f : Math.max(0f, 1f - (dev - t.tol() * 0.5f) / (t.tol() * 2f));
                }
                sum += w * score;
                weights += w;
                float miss = w * (1f - score);
                if (miss > worstMiss) {
                    worstMiss = miss;
                    worstAsk = -1;
                    worst = t.fig();
                    worstHigh = high;
                }
            }
        }
        float closeness = weights == 0 ? 1f : sum / weights;
        int g = 0;
        for (float cut : VillageTuning.CLASS_CUTS)
            if (closeness >= cut)
                g++;
        if (mustFailed)
            g = Math.max(0, g - 1);
        return new Result(closeness, Grade.values()[g], mustFailed,
                worstMiss >= VillageTuning.COMPLAINT_FLOOR ? worst : null, worstHigh,
                worstMiss >= VillageTuning.COMPLAINT_FLOOR ? worstAsk : -1);
    }
}
