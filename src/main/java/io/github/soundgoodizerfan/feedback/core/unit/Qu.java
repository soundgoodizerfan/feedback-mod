// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
 * <p>First made live by the electrolytic cell: what current moves is an amount
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
