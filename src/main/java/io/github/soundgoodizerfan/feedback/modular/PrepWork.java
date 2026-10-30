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
package io.github.soundgoodizerfan.feedback.modular;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.FoodCut;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.util.Mth;

/**
 * The Prep Table's two hand minigames, graded: pure functions of what the
 * player did, shared by the server (which decides) and the screens (which draw the same picture live, so they agree).
 */
public final class PrepWork {

    /** Marks sit on a 0..{@value} line along the food. */
    public static final int LINE = 1000;
    /** The pour's classes. */
    public static final int CLEAN = 0, SHAKY = 1, HEAVY = 2, LIGHT = 3;
    /** How long the judging music of each result runs, in ticks (its file's length): the pour's classes, and the cut's even, mixed, ragged (indices 0 to 2) share the table. */
    public static final int[] JUDGE_TICKS = {120, 98, 108, 108};

    private PrepWork() {
    }

    /** The cut {@code n} marks (in any order) make; level 0 is whole. A ragged cut is bad, never refused. */
    public static FoodCut cut(int[] marks, int n) {
        if (n <= 0)
            return new FoodCut(0, FoodCut.EVEN);
        int[] at = Arrays.copyOf(marks, n);
        Arrays.sort(at);
        float mean = (float) LINE / (n + 1), spread = 0;
        int last = 0;
        for (int i = 0; i <= n; i++) {
            int to = i < n ? at[i] : LINE;
            float d = (to - last) - mean;
            spread += d * d;
            last = to;
        }
        float cv = (float) Math.sqrt(spread / (n + 1)) / mean;
        int level = n >= FTuning.CUT_MINCED_MARKS ? 2 : 1;
        return new FoodCut(level, cv < FTuning.CUT_EVEN_CV ? FoodCut.EVEN : cv < FTuning.CUT_MIXED_CV ? FoodCut.MIXED : FoodCut.RAGGED);
    }

    /**
     * Where a stroke aimed at {@code clicked} lands in this food: fibre pulls it toward the nearest grain line (tough food is
     * hard to cut evenly, soft food is exact) and fat lets the blade slide on along the stroke.
     */
    public static int land(int clicked, Material.Food food) {
        int grain = Math.round((float) clicked / FTuning.CUT_GRAIN) * FTuning.CUT_GRAIN;
        float pull = Mth.clamp(food.fibre() * FTuning.CUT_GRAIN_PULL, 0f, 0.9f);
        float at = clicked + (grain - clicked) * pull + food.fat() * FTuning.CUT_FAT_SLIDE;
        return Mth.clamp(Math.round(at), 0, LINE);
    }

    /** Whether submitted marks are something the screen could have made. */
    public static boolean valid(List<Integer> marks) {
        return marks.size() <= FTuning.CUT_MAX_MARKS && marks.stream().allMatch(m -> m >= 0 && m <= LINE);
    }

    /** Units a pour of this viscosity (water 1; none given reads as water) puts in the vessel each tick. */
    public static float flow(Optional<Float> viscosity) {
        float v = Math.max(1f, viscosity.orElse(1f));
        return FTuning.POUR_FLOW / (1f + FTuning.POUR_DRAG * (float) Math.log10(v));
    }

    public static float flow(Material material) {
        return flow(material.molten().viscosity());
    }

    /** Units in the vessel after {@code held} ticks of pouring. */
    public static float poured(int held, float flow) {
        return Math.min(held, FTuning.POUR_MAX_TICKS) * flow;
    }

    /** Poured over the line, kept inside the floor and ceiling: what multiplies the finish's compounds. */
    public static float strength(float poured) {
        return Mth.clamp(poured / FTuning.POUR_TARGET, FTuning.POUR_MIN, FTuning.POUR_MAX);
    }

    /** How a finish goes on, by what it is: runs (viscosity) is poured, whole pieces (vegetal) are placed, dry powder is shaken. */
    public static final int POUR = 0, SHAKE = 1, PLACE = 2;

    public static int mode(Material material) {
        if (material.molten().viscosity().isPresent())
            return POUR;
        return material.traits().contains(Material.Trait.VEGETAL) ? PLACE : SHAKE;
    }

    /** The strength of a shake of {@code strokes} sweeps. */
    public static float shaken(int strokes) {
        return strength(Math.min(strokes, FTuning.SHAKE_MAX_STROKES) * (float) FTuning.SHAKE_UNITS);
    }

    /** Whether submitted placements are something the screen could have made. */
    public static boolean validPlaced(List<Integer> marks) {
        return marks.size() <= FTuning.PLACE_MAX && marks.stream().allMatch(m -> m >= 0 && m <= LINE);
    }

    /** The strength of pieces laid at {@code marks}: how many, discounted by how unevenly they lie. */
    public static float placed(List<Integer> marks) {
        int n = marks.size();
        if (n == 0)
            return 0f;
        int[] at = marks.stream().mapToInt(Integer::intValue).toArray();
        int grade = cut(at, n).grade();
        float share = grade == FoodCut.EVEN ? 1f : grade == FoodCut.MIXED ? FTuning.PLACE_MIXED_SHARE : FTuning.PLACE_RAGGED_SHARE;
        return Mth.clamp(n * share / FTuning.PLACE_TARGET, FTuning.POUR_MIN, FTuning.POUR_MAX);
    }

    /** The class a strength falls in: clean or shaky near the line, else heavy or light by which side. */
    public static int pourClass(float strength) {
        float off = Math.abs(strength - 1f);
        return off <= FTuning.POUR_CLEAN ? CLEAN : off <= FTuning.POUR_SHAKY ? SHAKY : strength > 1f ? HEAVY : LIGHT;
    }
}
