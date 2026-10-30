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
package io.github.soundgoodizerfan.feedback.potion;

import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.process.Compound;
import io.github.soundgoodizerfan.feedback.process.CompoundTable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The brewing map's arithmetic (potions spec 3), pure over primitives and the loaded rows, so the stand and the screen's path preview
 * ask the same functions. Nothing is rolled.
 */
public final class PotionBrew {

    private PotionBrew() {}

    /** Where a brew ended inside a node: {@code strength} 0..1 from the x offset, {@code duration} {@link PotionTuning#DURATION_AT_EDGE}..1 from the y offset. */
    public record Landing(ResourceLocation id, PotionFeature feature, float strength, float duration) {}

    /**
     * How much of what an ingredient carries goes into the path (0..1): the cut's surface, gated by the pot's heat (stalled at
     * {@link PotionTuning#COLD_TU}, full at {@link PotionTuning#COOK_TU}). Worked out once, at the moment the ingredient goes in, from the cut and
     * the pot's heat then; the line it makes is fixed from there and never grows with time. The stand and the screen's hover preview both call this.
     */
    public static float extraction(float surface, float tu) {
        float heat = Mth.clamp((tu - PotionTuning.COLD_TU) / (PotionTuning.COOK_TU - PotionTuning.COLD_TU), 0f, 1f);
        return surface * heat;
    }

    /**
     * The vector an ingredient adds to the marker: each compound's strength, less what rot took, times how far it has come out,
     * along the compound's direction. Rot adds a small fixed vector toward {@code rotDegrees}, the nearest hazard from the centre.
     */
    public static float[] vector(Map<String, Float> compounds, float rot, float extraction, float rotDegrees) {
        float x = 0f, y = 0f;
        for (Map.Entry<String, Float> e : compounds.entrySet()) {
            Compound c = CompoundTable.get().find(e.getKey()).orElse(null);
            if (c == null || c.direction().isEmpty())
                continue;
            float len = e.getValue() * (1f - PotionTuning.ROT_LOSS * rot) * c.gain() * extraction;
            float a = c.direction().get() * Mth.DEG_TO_RAD;
            x += Mth.cos(a) * len;
            y += Mth.sin(a) * len;
        }
        float a = rotDegrees * Mth.DEG_TO_RAD;
        return new float[] {x + Mth.cos(a) * rot * PotionTuning.ROT_PULL * extraction, y + Mth.sin(a) * rot * PotionTuning.ROT_PULL * extraction};
    }

    /** The effect node the point is inside, nearest centre first, or null. */
    public static Landing land(List<? extends Map.Entry<ResourceLocation, PotionFeature>> map, float x, float y) {
        Landing best = null;
        float bestDistance = Float.MAX_VALUE;
        for (Map.Entry<ResourceLocation, PotionFeature> e : map) {
            PotionFeature f = e.getValue();
            float d = f.distance(x, y);
            if (f.kind() != PotionFeature.Kind.EFFECT || d > f.radius() || d >= bestDistance)
                continue;
            bestDistance = d;
            // x offset costs strength, y offset costs duration (spec 3.2).
            // The flask's centre must be inside the ring: the radius means what it says.
            best = new Landing(e.getKey(), f, 1f - Math.abs(x - f.x()) / f.radius(),
                    1f - (1f - PotionTuning.DURATION_AT_EDGE) * Math.abs(y - f.y()) / f.radius());
        }
        return best;
    }

    /** The compound class that pulls most nearly toward (x, y) from the centre: the one whose direction is closest to the point's bearing. Empty if none steers. */
    public static String dominantClass(float x, float y) {
        float bearing = (float) Math.toDegrees(Math.atan2(y, x));
        String best = "";
        float bestOff = Float.MAX_VALUE;
        for (Map.Entry<String, Compound> e : CompoundTable.get().all().entrySet()) {
            if (e.getValue().direction().isEmpty())
                continue;
            float off = Math.abs(Mth.wrapDegrees(e.getValue().direction().get() - bearing));
            if (off < bestOff) {
                bestOff = off;
                best = e.getKey();
            }
        }
        return best;
    }

    /** The level (0 is I) a landing's strength buys, before the potion's potency fades it. */
    public static int amplifier(Landing l) {
        return Math.round(Mth.clamp(l.strength(), 0f, 1f) * l.feature().effect().orElseThrow().maxAmplifier());
    }

    public static int duration(Landing l) {
        return Math.max(20, Math.round(l.feature().effect().orElseThrow().baseDuration() * l.duration()));
    }

    /**
     * The stabilisers' nudge (spec 3.3): toward the nearest node along one axis each, by at most the pull they sum to, and only when the
     * point is within {@link PotionTuning#PULL_REACH_RADII} radii of one, so it tightens a near miss and never rescues a wild one.
     */
    public static float[] pull(List<? extends Map.Entry<ResourceLocation, PotionFeature>> map, float x, float y, float pullX, float pullY) {
        PotionFeature near = null;
        float best = Float.MAX_VALUE;
        for (Map.Entry<ResourceLocation, PotionFeature> e : map) {
            PotionFeature f = e.getValue();
            float d = f.distance(x, y);
            if (f.kind() == PotionFeature.Kind.EFFECT && d < best) {
                best = d;
                near = f;
            }
        }
        if (near == null || best > near.radius() * PotionTuning.PULL_REACH_RADII)
            return new float[] {x, y};
        return new float[] {x + Mth.clamp(near.x() - x, -pullX, pullX), y + Mth.clamp(near.y() - y, -pullY, pullY)};
    }
}
