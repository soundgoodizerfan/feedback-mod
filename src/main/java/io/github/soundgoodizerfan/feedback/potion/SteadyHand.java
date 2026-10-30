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
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.registry.FEffects;

import net.minecraft.world.entity.LivingEntity;

/**
 * Steady hand. While it lasts, a gun's sway (its shot spread, {@code weapon/GunStance}) and recoil (the camera kick, {@code client/GunClient})
 * are a fraction of what they were; never nothing. The effect is a marker, both sides read it (effects are synced to the client).
 */
public final class SteadyHand {

    private SteadyHand() {}

    /** Multiplier on spread and on kick: {@link PotionTuning#STEADY_HAND_FRACTION} while the effect lasts, else 1. */
    public static float factor(LivingEntity shooter) {
        return shooter.hasEffect(FEffects.STEADY_HAND) ? PotionTuning.STEADY_HAND_FRACTION : 1f;
    }
}
