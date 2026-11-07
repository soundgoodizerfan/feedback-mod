// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.sort;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.logistics.LogisticsTuning;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The shared decision: is this unit in this band, as seen by this detector? The true figure is
 * compared against the cut, with a seeded margin: far from the cut the answer is always right,
 * near it there is a chance of the wrong fate that climbs toward the cut and grows with the head's
 * {@link Detector#noise}. The seed (a slot number xor the run's phase tick) makes a replay repeat
 * but the player cannot exploit it. No detector reaches zero noise.
 * <p>
 * Weaknesses (one each, so a second detector covers the first): a magnet misses cargo over its
 * Curie temperature; an optical head reads glowing-hot cargo bright and, dirty, everything duller;
 * a weigher blurs on stacks (it weighs the pile, not the piece); a jet cuts poorly.
 */
public final class Sorter {

    private Sorter() {
    }

    /** Tag test (a datapack-friendly route; modded equivalents work for free). Exact, no noise. */
    public static boolean hasTag(ItemStack stack, TagKey<Item> tag) {
        return stack.is(tag);
    }

    public static Verdict decide(Band band, ItemStack stack, Level level, Detector d, boolean dirty, long seed) {
        return decide(band, SortReader.read(stack, level), stack.getCount(), d, dirty, seed);
    }

    public static Verdict decide(Band band, Reading r, int count, Detector d, boolean dirty, long seed) {
        if (r.unknown())
            return Verdict.UNKNOWN;
        float margin = LogisticsTuning.NOISE_MARGIN;
        float noise = d.noise * (d == Detector.WEIGHER ? 1f + (count - 1) / 16f : 1f);
        SortProperty p = band.property();
        // t: distance from the nearest cut as a share of the margin (0 on the cut, >=1 safe).
        float t = 1f;
        Reading seen = r;
        switch (p) {
            case MAGNETIC -> {
                // Only a magnetic piece near its Curie point is uncertain; a hot one reads weaker.
                if (r.magnetic() && r.curie() < Float.MAX_VALUE)
                    t = Math.abs(r.temperature() - r.curie()) / Math.max(1f, r.curie()) / margin;
            }
            case REFLECTIVITY -> {
                float v = r.reflectivity();
                if (d == Detector.OPTICAL) {
                    if (r.temperature() > LogisticsTuning.TEMP_HOT_FROM * 2f)
                        v += LogisticsTuning.GLOW_REFLECT_GAIN;
                    if (dirty)
                        v -= LogisticsTuning.DIRT_REFLECT_LOSS;
                }
                seen = new Reading(false, r.magnetic(), r.curie(), Math.max(0f, v), r.density(), r.hardness(), r.friction(), r.kind(),
                        r.temperature(), r.mass(), r.unitMass(), r.brittle(), r.sticky());
                t = cutDistance(v, margin, LogisticsTuning.REFLECT_DULL_BELOW, LogisticsTuning.REFLECT_BRIGHT_FROM);
            }
            case DENSITY -> t = cutDistance(r.density(), margin, LogisticsTuning.DENSITY_LIGHT_BELOW, LogisticsTuning.DENSITY_HEAVY_FROM);
            case HARDNESS -> t = cutDistance(r.hardness(), margin, LogisticsTuning.HARDNESS_SOFT_BELOW, LogisticsTuning.HARDNESS_HARD_FROM);
            case FRICTION -> t = cutDistance(r.friction(), margin, LogisticsTuning.FRICTION_SLICK_BELOW, LogisticsTuning.FRICTION_GRIPPY_FROM);
            case TEMPERATURE -> t = cutDistance(r.temperature(), margin, FTuning.WARM_TU.value(), LogisticsTuning.TEMP_HOT_FROM);
            case SIZE -> t = cutDistance(r.mass(), margin, LogisticsTuning.SIZE_SMALL_BELOW, LogisticsTuning.SIZE_LARGE_FROM);
            case KIND -> t = 1f; // a material's kind is not a measurement
        }
        boolean in = SortReader.bandOf(seen, p) == band;
        if (p == SortProperty.MAGNETIC && d == Detector.MAGNET && r.temperature() > FTuning.WARM_TU.value())
            t = Math.min(t, 1f); // a warm permanent magnet is no worse; its own Curie weakness is the warm-up margin
        if (t < 1f && roll(seed) < LogisticsTuning.NOISE_PEAK * (1f - t) * noise)
            in = !in;
        return in ? Verdict.MATCH : Verdict.MISS;
    }

    private static float cutDistance(float v, float margin, float... cuts) {
        float best = Float.MAX_VALUE;
        for (float c : cuts)
            best = Math.min(best, Math.abs(v - c) / Math.max(0.05f, Math.abs(c)));
        return best / margin;
    }

    /** A repeatable roll in [0,1) from a seed (splitmix64). */
    public static float roll(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (z >>> 40) / (float) (1 << 24);
    }
}
