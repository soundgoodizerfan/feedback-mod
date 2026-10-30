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

import io.github.soundgoodizerfan.feedback.process.Material;

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
     * One plant's piece of the path (potions spec 15): a curve of {@code shape} turned to {@code heading} and scaled to {@code reach}, plus
     * rot's drift ({@code dx, dy}, spread over the walk). Walked by arc fraction {@code s} (0..1), so a stir of a given distance covers that much
     * curve however it twists. Fixed when the plant goes in. The stand and the screen both draw and walk this one record.
     *
     * shortcut: the length ignores the drift's small addition, so a rotten plant is walked a touch faster than its true arc; fine until the drift is large.
     */
    public record Segment(float heading, float reach, PotionShape shape, float dx, float dy) {

        /** The offset from the segment's start after walking arc fraction {@code s}, written into {@code out} (x, y). */
        public void at(float s, float[] out) {
            shape.atArc(s, out);
            float a = heading * Mth.DEG_TO_RAD, c = Mth.cos(a), n = Mth.sin(a), u = out[0], v = out[1];
            out[0] = reach * (u * c - v * n) + s * dx;
            out[1] = reach * (u * n + v * c) + s * dy;
        }

        /** How far the whole segment walks, in map units. */
        public float arcLength() {
            return reach * shape.length();
        }

        /** Where the segment ends, relative to where it starts. */
        public float[] end() {
            float[] out = new float[2];
            at(1f, out);
            return out;
        }
    }

    /**
     * The segment a plant adds: its reach less what rot took, times how far it has come out, along its shape. Rot adds a drift toward
     * {@code rotDegrees}, the nearest hazard from the centre, of {@link PotionTuning#ROT_PULL} of the reach it would have had.
     */
    public static Segment segment(Material.Potion plant, float rot, float extraction, float rotDegrees) {
        float reach = plant.reach() * extraction, a = rotDegrees * Mth.DEG_TO_RAD, drift = rot * PotionTuning.ROT_PULL * reach;
        return new Segment(plant.heading(), reach * (1f - PotionTuning.ROT_LOSS * rot), plant.shape(), Mth.cos(a) * drift, Mth.sin(a) * drift);
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
