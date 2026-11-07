// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
