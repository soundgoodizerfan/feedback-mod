// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;

/**
 * Gas operated (spec 8.4): the cycling energy is taken from the gas, so the action is smooth and a high
 * pressure does not hurt it (no pressure limit, unlike blowback), paid in the piston's mass and in heat at the
 * port.
 */
public class GunGasOperated implements ModularTrait {

    @Override
    public GunFigures gunBaked(GunFigures f, GunSpec spec, Params p) {
        return f.withCycle(p.get("cycle_mult", 0.9f)).withHeat(p.get("heat_mult", 1.15f)).withMass(p.get("mass", 0.15f));
    }
}
