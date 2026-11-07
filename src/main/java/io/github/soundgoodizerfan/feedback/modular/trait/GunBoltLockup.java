// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;

/**
 * Bolt lockup (spec 8.4): the strongest receiver there is. Lugs lock the bolt into the barrel, so the same steel
 * is rated for {@code rating_mult} times the chamber pressure, and the stiff action groups tighter by
 * {@code spread_mult}. Paid for in a manual cycle (the action's own).
 */
public class GunBoltLockup implements ModularTrait {

    @Override
    public GunFigures gunBaked(GunFigures f, GunSpec spec, Params p) {
        return f.withRating(p.get("rating_mult", 1.25f)).withSpread(p.get("spread_mult", 0.8f));
    }
}
