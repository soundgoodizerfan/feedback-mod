// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * Density, in {@code Mu/mB} -- kilograms per litre, so water is 1 and steel about 7.85.
 *
 * <h2>A material's figure, not a block's</h2>
 * It lives on {@code process/Material}, read from {@code material/*.json}, because everything
 * made of a material should weigh what that material weighs whatever shape it was given -- an
 * engine, a plate, a tube. Sable's per-block mass is the figure for a placed block; this is the
 * figure for the stuff.
 */
public record Density(float muPerMb) implements Unit {

    @Override
    public float raw() {
        return muPerMb;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.density";
    }
}
