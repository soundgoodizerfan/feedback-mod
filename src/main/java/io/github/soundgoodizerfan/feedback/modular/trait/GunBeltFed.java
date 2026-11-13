// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;


/** Belt fed (spec 8.4): the rounds are a chain, so the belt is a long reload ({@code reload_mult}) and as long as the belt. */
public class GunBeltFed implements ModularTrait {

    @Override
    public float gunReloadMult(Params p) {
        return p.get("reload_mult", 2f);
    }
}
