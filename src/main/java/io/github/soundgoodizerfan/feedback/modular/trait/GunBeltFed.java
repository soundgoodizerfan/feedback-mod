// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.weapon.HandlingPhysics;
import net.minecraft.world.item.ItemStack;

/** Belt fed (spec 8.4): the rounds are a chain, so the belt is a long reload, longer on a rougher feed way ({@link HandlingPhysics#beltReload}). */
public class GunBeltFed implements ModularTrait {
    @Override
    public float gunReloadMult(ItemStack gun, Params p) {
        var m = GunFitting.of(gun, "belt_feed");
        return m == null ? 2f : HandlingPhysics.beltReload(m);
    }
}
