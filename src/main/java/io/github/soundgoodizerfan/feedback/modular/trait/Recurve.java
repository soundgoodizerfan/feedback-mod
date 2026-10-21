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
