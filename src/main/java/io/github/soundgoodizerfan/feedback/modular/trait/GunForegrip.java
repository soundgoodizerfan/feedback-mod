// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.weapon.GunFigures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Foregrip: the off hand pulls the muzzle down, so kick falls by {@code kick_mult} and spread by
 * {@code spread_mult} whatever the shooter is doing (a stock needs a crouch, a still body).
 */
public class GunForegrip implements ModularTrait {

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        return f.withKick(p.get("kick_mult", 0.85f)).withSpread(p.get("spread_mult", 0.8f));
    }
}
