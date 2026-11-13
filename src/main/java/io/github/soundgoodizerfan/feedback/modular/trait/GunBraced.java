// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.weapon.GunFigures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Braced (spec 8.4): supported fire. A shoulder stock or a heavy frame takes the kick into the body when the
 * shooter is crouched and still: kick falls by {@code kick_mult} and spread by {@code spread_mult}. Named
 * {@code gun_braced} because armour's long leg guards already hold {@code braced}.
 */
public class GunBraced implements ModularTrait {

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        if (!shooter.isCrouching() || shooter.getDeltaMovement().horizontalDistanceSqr() > 1.0E-3)
            return f;
        return f.withKick(p.get("kick_mult", 0.7f)).withSpread(p.get("spread_mult", 0.6f));
    }
}
