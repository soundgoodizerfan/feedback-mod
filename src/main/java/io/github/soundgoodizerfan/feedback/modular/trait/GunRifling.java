// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;

/** Rifling spins the bullet, so it flies true: the spread of a single bullet is the plain bore's times {@code spread_mult}. */
public class GunRifling implements ModularTrait {

    @Override
    public GunFigures gunBaked(GunFigures f, GunSpec spec, Params p) {
        return f.withSpread(p.get("spread_mult", 0.6f));
    }
}
