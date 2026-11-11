// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;

/**
 * A horn or sinew layer on a bow's limbs: more energy for the length ({@code velocity_mult} and {@code draw_mult}, multiplied in by
 * {@link io.github.soundgoodizerfan.feedback.modular.RangedDispatch}), but it weakens in damp: an arrow loosed in rain or water goes
 * {@code wet_mult} as fast.
 */
public final class Laminate implements ModularTrait {

    @Override
    public void onArrow(AbstractArrow arrow, ItemStack weapon, Params params) {
        if (arrow.isInWaterOrRain())
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(params.get("wet_mult", 0.85f)));
    }
}
