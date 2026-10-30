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

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.FoodKnead;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.util.Mth;

/**
 * The Prep Table's knead ({@code docs/specs/feedback_food_spec.md} 6): pressure and rest. A pure model of a dough under the hand,
 * stepped once per sample of how hard it is pressed (0..100), shared by the server (which grades the samples it is sent) and
 * the screen (which draws the same gluten and tension live, so what the player saw is what is graded).
 *
 * <p>Pressing builds gluten but also tension; only relaxed dough takes work, so rest is productive. The dough's own figures
 * set every rate: water makes it sticky (slower gluten, and a drier dough tears at a lower tension), fibre makes it tense
 * faster and rest slower, fat tenderises (slower gluten, less tension).
 */
public final class Kneading {

    private final float stick, fat, fibre, tearAt;
    private float gluten, tension, over;
    private int steps;

    public Kneading(Material.Food food) {
        this.stick = Mth.clamp(food.moisture(), 0f, 1f);
        this.fat = food.fat();
        this.fibre = food.fibre();
        this.tearAt = FTuning.KNEAD_TEAR * Mth.lerp(stick, FTuning.KNEAD_TEAR_DRY, FTuning.KNEAD_TEAR_WET);
    }

    /** One sample: the press, 0..100. */
    public void push(int sample) {
        float p = Mth.clamp(sample, 0, 100) / 100f;
        // Tense dough resists the work: only rested dough takes it.
        float g = FTuning.KNEAD_GLUTEN * p * (1f - FTuning.KNEAD_STICKY * stick) * (1f - FTuning.KNEAD_FAT_GLUTEN * fat)
                * Math.max(0f, 1f - FTuning.KNEAD_TENSE_RESIST * tension);
        float up = FTuning.KNEAD_TENSION * p * p * (1f + FTuning.KNEAD_FIBRE_TENSION * fibre) * (1f - FTuning.KNEAD_FAT_TENSION * fat);
        float down = FTuning.KNEAD_REST * (1f - p) * (1f - FTuning.KNEAD_FIBRE_REST * fibre);
        gluten = Math.min(1f, gluten + g);
        tension = Mth.clamp(tension + up - down, 0f, 2f);
        if (tension > tearAt)
            over += tension - tearAt;
        steps++;
    }

    public float gluten() {
        return gluten;
    }

    public float tension() {
        return tension;
    }

    /** The tension at which this dough tears. */
    public float tearAt() {
        return tearAt;
    }

    /** Overwork so far, in tension-past-the-limit summed over samples. */
    public float over() {
        return over;
    }

    /** Whether the dough is tearing now: the screen cracks it, the grade will count it. */
    public boolean tearing() {
        return tension > tearAt;
    }

    /** The class this much work makes. */
    public int grade() {
        if (over >= FTuning.KNEAD_TOUGH_OVER)
            return FoodKnead.TOUGH;
        if (gluten < FTuning.KNEAD_GOOD_AT)
            return FoodKnead.CRUMBLY;
        return gluten >= FTuning.KNEAD_GLOSSY_AT && tension <= FTuning.KNEAD_RELAXED ? FoodKnead.GLOSSY : FoodKnead.GOOD;
    }

    /** Whether submitted samples are something the screen could have made. */
    public static boolean valid(List<Integer> samples) {
        return samples.size() >= FTuning.KNEAD_MIN_SAMPLES && samples.size() <= FTuning.KNEAD_MAX_SAMPLES
                && samples.stream().allMatch(s -> s >= 0 && s <= 100);
    }

    /** The class {@code samples} knead this dough into: the server's own grading. */
    public static int grade(Material.Food food, List<Integer> samples) {
        Kneading run = new Kneading(food);
        for (int s : samples)
            run.push(s);
        return run.grade();
    }

    /** How far a wet dough clings: the press follows the hand at this share of the gap per tick (sticky dough lags). Screen only. */
    public static float follow(float moisture) {
        return Mth.lerp(Mth.clamp(moisture, 0f, 1f), 0.5f, 0.12f);
    }
}
