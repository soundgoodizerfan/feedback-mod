// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.modular.RangedDispatch;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.AbstractArrow;

/**
 * The recurve bow's snap (spec 5.4): a bow drawn all the way ({@code AbstractArrow.isCritArrow},
 * which is vanilla's own full-draw flag) shoots an arrow that does {@code 1 + bonus} damage and
 * passes through {@code pierce} mob. Cost: a part draw gets neither, and the limbs draw
 * {@code draw_mult} slower, so the bonus is for the patient.
 */
public final class Recurve implements ModularTrait {

    @Override
    public void onArrow(AbstractArrow arrow, ItemStack weapon, Params params) {
        if (!arrow.isCritArrow())
            return;
        arrow.setBaseDamage(arrow.getBaseDamage() * (1 + params.get("bonus", 0.25f)));
        RangedDispatch.pierce(arrow, params.get("pierce", 1f));
    }
}
