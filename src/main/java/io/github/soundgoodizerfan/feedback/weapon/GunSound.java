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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * How loud and how low a shot sounds, from the figures ({@code docs/feedback_guns_spec.md} section 7:
 * every feel parameter is derived from a physical figure).
 *
 * <p>Create Big Cannons' shape, taken as a design: one sound re-pitched by power, the pitch
 * falling as power rises (spec 3.6). The jitter is TacZ's (0.9 to 1.025). The sound itself is a
 * vanilla placeholder until there is an asset to layer; the spec's layered shot, tail and
 * mechanism is the next step, not this one. The blast layer is now the author's own 9 mm
 * recording; the thump and clack are still vanilla.
 */
public final class GunSound {

    /**
     * Semitones at zero power. The blast is a recording of a 9 mm, which is about 0.44 power, so
     * it plays at its own pitch there and falls from it as the charge grows (spec 3.6: pitch
     * drops with power).
     */
    private static final float PITCH_SEMITONES_AT_ZERO = 3.5f;

    private GunSound() {
    }

    /** 0 to 1: muzzle energy against what reads as full power. */
    public static float power(GunFigures f) {
        return Mth.clamp(f.muzzleEnergy() / FTuning.GUN_SOUND_REFERENCE_WORK, 0f, 1f);
    }

    public static float pitch(GunFigures f, RandomSource random) {
        float semitones = PITCH_SEMITONES_AT_ZERO - 8f * power(f);
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
        if (f.launcher() == GunSpec.Launcher.SPRING) {
            // No gas, no blast: the release, a snap as the slug leaves, and the same clack.
            return java.util.List.of(
                    new Layer(SoundEvents.CROSSBOW_SHOOT, 0.8f, (1.5f - 0.4f * p) * jitter),
                    new Layer(SoundEvents.IRON_DOOR_CLOSE, 0.3f, 1.8f * jitter));
        }
        if (f.launcher() == GunSpec.Launcher.STEAM) {
            // No powder: a hiss of released steam, a snap as the slug leaves, and the same clack.
            return java.util.List.of(
                    new Layer(SoundEvents.FIRE_EXTINGUISH, 0.8f, (1.2f - 0.3f * p) * jitter),
                    new Layer(SoundEvents.CROSSBOW_SHOOT, 0.5f, (1.3f - 0.3f * p) * jitter),
                    new Layer(SoundEvents.IRON_DOOR_CLOSE, 0.3f, 1.8f * jitter));
        }
        if (f.coil()) {
            // No gas, no blast: the field's whine, a crack as the slug leaves, and the same clack.
            return java.util.List.of(
                    new Layer(SoundEvents.BEACON_POWER_SELECT, 0.9f, (1.9f - 0.6f * p) * jitter),
                    new Layer(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.25f + 0.35f * p, 2f - 0.5f * p),
                    new Layer(SoundEvents.IRON_DOOR_CLOSE, 0.3f, 1.8f * jitter));
        }
        var out = new java.util.ArrayList<Layer>(3);
        out.add(new Layer(event(), volume(f), (float) Math.pow(2, (PITCH_SEMITONES_AT_ZERO - 8f * p) / 12f) * jitter));
        if (p > 0.25f)
            out.add(new Layer(SoundEvents.GENERIC_EXPLODE.value(), 0.5f * p * f.report(), 1.7f - 0.7f * p));
        out.add(new Layer(SoundEvents.IRON_DOOR_CLOSE, 0.35f, (1.6f - 0.5f * p) * jitter));
        return out;
    }

    public static SoundEvent event() {
        return io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_SHOT_9MM.get();
    }
}
