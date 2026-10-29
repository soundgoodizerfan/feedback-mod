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
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * Amount of substance. One Qu is the amount of metal in a nugget, one ninth of an ingot (user
 * ruling 2026-10-28), so an ingot is 9 Qu and a block 81; molten, 1 Qu of metal is 16 mB.
 * Concentration is {@code Qu/mB}.
 *
 * <p>It is a reference sample, as the mole was before 2019 (the atoms in 12 g of carbon-12): equal
 * Qu of different substances hold the same amount of stuff, so amounts compare across substances
 * -- a copper ingot and a zinc ingot are the same 9 Qu -- while their masses differ, mass per Qu
 * being a property of the substance (molar mass). Nothing here counts atoms or implies any.
 *
 * <p>First made live by the electrolytic cell (mechanics 6.14): what current moves is an amount
 * of metal, and counting it in ingots would make an ingot a unit. An ingot is
 * {@code FTuning.QU_PER_INGOT}.
 */
public record Qu(float value) implements Unit {

    @Override
    public float raw() {
        return value;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.qu";
    }
}
