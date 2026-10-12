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
 * How loud and how low a shot sounds, from the figures ({@code feedback_guns_spec.md} section 7:
 * every feel parameter is derived from a physical figure).
 *
 * <p>Create Big Cannons' shape, taken as a design: one sound re-pitched by power, the pitch
 * falling as power rises (spec 3.6). The jitter is TacZ's (0.9 to 1.025). The sound itself is a
 * vanilla placeholder until there is an asset to layer; the spec's layered shot, tail and
 * mechanism is the next step, not this one.
 */
public final class GunSound {

    private GunSound() {
    }

    /** 0 to 1: muzzle energy against what reads as full power. */
    public static float power(GunFigures f) {
        return Mth.clamp(f.muzzleEnergy() / FTuning.GUN_SOUND_REFERENCE_WORK, 0f, 1f);
    }

    public static float pitch(GunFigures f, RandomSource random) {
        float semitones = 2f - 8f * power(f);
        return (float) Math.pow(2, semitones / 12f) * (0.9f + random.nextFloat() * 0.125f);
    }

    public static float volume(GunFigures f) {
        return 1.5f + 2.5f * power(f);
    }

    public static SoundEvent event() {
        return SoundEvents.FIREWORK_ROCKET_BLAST;
    }
}
