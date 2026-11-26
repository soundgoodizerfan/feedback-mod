// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.sort;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.logistics.LogisticsTuning;

import net.minecraft.core.registries.BuiltInRegistries;
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
 * a weigher blurs on stacks (it weighs the pile, not the piece); a jet cuts poorly; a kind test (grinder sparks for metal, a flame for organics) is fooled by alloys, coats and look-alikes.
 */
public final class Sorter {

    private Sorter() {
    }

    public static Verdict decide(Band band, ItemStack stack, Level level, Detector d, boolean dirty, long seed) {
        return decide(band, SortReader.read(stack, level), stack.getCount(), d, dirty, seed);
    }

    public static Verdict decide(Band band, Reading r, int count, Detector d, boolean dirty, long seed) {
        return decide(band, r, count, d, dirty, seed, 1f);
    }

    /** As above with the head's noise times {@code scale}: below 1 where other heads check the same property. */
    private static Verdict decide(Band band, Reading r, int count, Detector d, boolean dirty, long seed, float scale) {
        if (r.unknown())
            return Verdict.UNKNOWN;
        float margin = LogisticsTuning.NOISE_MARGIN;
        float noise = scale * d.noise * (d == Detector.WEIGHER ? 1f + (count - 1) / 16f : 1f);
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
                seen = seen(r, p, d, dirty);
                t = cutDistance(seen.reflectivity(), margin, LogisticsTuning.REFLECT_DULL_BELOW, LogisticsTuning.REFLECT_BRIGHT_FROM);
            }
            case DENSITY -> t = cutDistance(r.density(), margin, LogisticsTuning.DENSITY_LIGHT_BELOW, LogisticsTuning.DENSITY_HEAVY_FROM);
            case HARDNESS -> {
                t = cutDistance(r.hardness(), margin, LogisticsTuning.HARDNESS_SOFT_BELOW, LogisticsTuning.HARDNESS_HARD_FROM);
                if (d == Detector.SCRATCH && r.hardness() >= LogisticsTuning.SCRATCH_BLUNT_FROM)
                    t = Math.min(t, LogisticsTuning.SCRATCH_BLUNT_CERTAINTY);
            }
            case FRICTION -> {
                seen = seen(r, p, d, dirty);
                t = cutDistance(seen.friction(), margin, LogisticsTuning.FRICTION_SLICK_BELOW, LogisticsTuning.FRICTION_GRIPPY_FROM);
            }
            case TEMPERATURE -> {
                seen = seen(r, p, d, dirty);
                t = cutDistance(seen.temperature(), margin, ThermalTuning.WARM_TU.value(), LogisticsTuning.TEMP_HOT_FROM);
            }
            case SIZE -> t = cutDistance(r.mass(), margin, LogisticsTuning.SIZE_SMALL_BELOW, LogisticsTuning.SIZE_LARGE_FROM);
            // A spark or flame test on a sample, never a read of the kind: how plainly it shows is the piece's own (alloys, coats, look-alikes muddy it).
            case KIND -> t = r.tell() * LogisticsTuning.KIND_CLEAN_TELL;
        }
        boolean in = SortReader.bandOf(seen, p) == band;
        if (p == SortProperty.MAGNETIC && d == Detector.MAGNET && r.temperature() > ThermalTuning.WARM_TU.value())
            t = Math.min(t, 1f); // a warm permanent magnet is no worse; its own Curie weakness is the warm-up margin
        if (t < 1f && roll(seed) < LogisticsTuning.NOISE_PEAK * (1f - t) * noise)
            in = !in;
        return in ? Verdict.MATCH : Verdict.MISS;
    }

    /** The reading as a head's own weakness shows it, for the property it is reading. */
    private static Reading seen(Reading r, SortProperty p, Detector d, boolean dirty) {
        float reflectivity = r.reflectivity(), friction = r.friction(), temperature = r.temperature();
        if (p == SortProperty.REFLECTIVITY && d == Detector.OPTICAL) {
            if (r.temperature() > LogisticsTuning.TEMP_HOT_FROM * 2f)
                reflectivity += LogisticsTuning.GLOW_REFLECT_GAIN;
            if (dirty)
                reflectivity -= LogisticsTuning.DIRT_REFLECT_LOSS;
            reflectivity = Math.max(0f, reflectivity);
        }
        if (p == SortProperty.FRICTION && d == Detector.DRAG && r.sticky())
            friction += LogisticsTuning.DRAG_STICKY_GAIN;
        if (p == SortProperty.TEMPERATURE && d == Detector.THERMO && r.reflectivity() >= LogisticsTuning.REFLECT_BRIGHT_FROM)
            temperature *= LogisticsTuning.THERMO_SHINY_SHARE;
        return new Reading(r.unknown(), r.magnetic(), r.curie(), reflectivity, r.density(), r.hardness(), friction, r.kind(),
                temperature, r.mass(), r.unitMass(), r.brittle(), r.sticky(), r.tell());
    }

    /**
     * The band a head records for {@code stack}: what it truly looks like to that head, or, where the head misjudges, the
     * band next to it across the nearer cut (a kind test mistakes one kind for any other). Null for a stack no material
     * table places. {@code scale} is the share of the head's noise left, below 1 where further heads check the property.
     */
    public static Band perceive(SortProperty p, ItemStack stack, Level level, Detector d, boolean dirty, long seed, float scale) {
        Reading r = SortReader.read(stack, level);
        if (r.unknown())
            return null;
        Reading seen = seen(r, p, d, dirty);
        Band truth = SortReader.bandOf(seen, p);
        if (decide(truth, r, 1, d, dirty, seed, scale) != Verdict.MISS)
            return truth;
        List<Band> bands = new ArrayList<>();
        for (Band b : Band.values())
            if (b.property() == p)
                bands.add(b);
        int at = bands.indexOf(truth), n = bands.size();
        if (p == SortProperty.KIND)
            return bands.get((at + 1 + (int) (roll(seed + 1) * (n - 1))) % n);
        if (at == 0)
            return bands.get(1);
        if (at == n - 1)
            return bands.get(n - 2);
        float value = switch (p) {
            case REFLECTIVITY -> seen.reflectivity() - (LogisticsTuning.REFLECT_DULL_BELOW + LogisticsTuning.REFLECT_BRIGHT_FROM) / 2f;
            case DENSITY -> seen.density() - (LogisticsTuning.DENSITY_LIGHT_BELOW + LogisticsTuning.DENSITY_HEAVY_FROM) / 2f;
            case HARDNESS -> seen.hardness() - (LogisticsTuning.HARDNESS_SOFT_BELOW + LogisticsTuning.HARDNESS_HARD_FROM) / 2f;
            case FRICTION -> seen.friction() - (LogisticsTuning.FRICTION_SLICK_BELOW + LogisticsTuning.FRICTION_GRIPPY_FROM) / 2f;
            case TEMPERATURE -> seen.temperature() - (ThermalTuning.WARM_TU.value() + LogisticsTuning.TEMP_HOT_FROM) / 2f;
            case SIZE -> seen.mass() - (LogisticsTuning.SIZE_SMALL_BELOW + LogisticsTuning.SIZE_LARGE_FROM) / 2f;
            case MAGNETIC, KIND -> 0f;
        };
        return bands.get(value < 0f ? at - 1 : at + 1);
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
