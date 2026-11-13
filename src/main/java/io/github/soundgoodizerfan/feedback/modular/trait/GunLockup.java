// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;

/**
 * Lockup (spec 8.4): how well the action holds the breech shut. A bolt's lugs lock it into the barrel, so the same
 * steel is rated for more than a latch or a toggle allows; {@code rating_mult} scales the chamber's rating and a stiff
 * action groups tighter by {@code spread_mult}. A stronger lockup is paid for in a slower cycle (the action's own).
 */
public class GunLockup implements ModularTrait {

    @Override
    public GunFigures gunBaked(GunFigures f, GunSpec spec, Params p) {
        return f.withRating(p.get("rating_mult", 1f)).withSpread(p.get("spread_mult", 1f));
    }
}
