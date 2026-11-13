// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.HandlingPhysics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Foregrip: the off hand pulls the muzzle down whatever the shooter is doing (a stock needs a crouch, a still body). The grip
 * holds by its metal's friction and a stiff frame keeps the muzzle down ({@link HandlingPhysics#foregrip}), by the variant's
 * {@code leverage}.
 */
public class GunForegrip implements ModularTrait {
    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        var b = HandlingPhysics.foregrip(p.get("leverage", 1f), GunFitting.of(gun, "foregrip"));
        return f.withKick(b.kick()).withSpread(b.spread());
    }
}
