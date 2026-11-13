// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * How loud and how low a shot sounds, from the figures ({@code docs/specs/feedback_guns_spec.md} section 7:
 * every feel parameter is derived from a physical figure).
 *
 * <p>Create Big Cannons' shape, taken as a design: one sound re-pitched by power, the pitch
 * falling as power rises (spec 3.6). The jitter is TacZ's (0.9 to 1.025). The sound itself is a
 * vanilla placeholder until there is an asset to layer; the spec's layered shot, tail and
 * mechanism is the next step, not this one. The blast is one of three recording classes (light, medium, heavy) by
 * power, each several takes picked at random; the thump and clack are still vanilla.
 */
public final class GunSound {

    /**
     * Semitones at zero power. The blast is a recording of a 9 mm, which is about 0.44 power, so
     * it plays at its own pitch there and falls from it as the charge grows (spec 3.6: pitch
     * drops with power).
     */
    private static final float PITCH_SEMITONES_AT_ZERO = 1.5f;

    private GunSound() {
    }

    /** 0 to 1: muzzle energy against what reads as full power. */
    public static float power(GunFigures f) {
        return Mth.clamp(f.muzzleEnergy() / FTuning.GUN_SOUND_REFERENCE_WORK, 0f, 1f);
    }

    public static float pitch(GunFigures f, RandomSource random) {
        float semitones = PITCH_SEMITONES_AT_ZERO - 3f * power(f);
        return (float) Math.pow(2, semitones / 12f) * (0.9f + random.nextFloat() * 0.125f);
    }

    /** Volume, which is also how far it carries (vanilla hears a sound to sixteen blocks a unit): the report scales it. */
    public static float volume(GunFigures f) {
        return (1.5f + 2.5f * power(f)) * f.report();
    }

    /** One layer of a shot: what plays, how loud, how high. */
    public record Layer(SoundEvent event, float volume, float pitch) {
    }

    /**
     * A shot as layers, all from the figures (spec 7: a shot is a blast, a body and a mechanism,
     * not one sample): the blast, a low thump that only a big charge has, and the action's clack
     * that every gun has and a light one has most of. The same jitter is on all of them, so they
     * stay a chord rather than flam.
     */
    public static java.util.List<Layer> layers(GunFigures f, RandomSource random) {
        float jitter = 0.9f + random.nextFloat() * 0.125f;
        float p = power(f);
        var out = new java.util.ArrayList<Layer>(3);
        // A suppressor quiets the report: its own recording, softer and duller, whatever the power.
        out.add(new Layer(f.report() < 0.6f ? io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_SHOT_SUPPRESSED.get() : event(p), volume(f), (float) Math.pow(2, (PITCH_SEMITONES_AT_ZERO - 3f * p) / 12f) * jitter));
        if (p > 0.25f)
            out.add(new Layer(SoundEvents.GENERIC_EXPLODE.value(), 0.5f * p * f.report(), 1.7f - 0.7f * p));
        out.add(new Layer(SoundEvents.IRON_DOOR_CLOSE, 0.35f, (1.6f - 0.5f * p) * jitter));
        return out;
    }

    /** The recording class for a power: the class carries most of the weight, the pitch only trims within it. */
    public static SoundEvent event(float power) {
        return (power < 0.33f ? io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_SHOT_LIGHT
                : power < 0.66f ? io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_SHOT_MEDIUM
                : io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_SHOT_HEAVY).get();
    }
}
