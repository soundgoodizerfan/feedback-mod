// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.HandlingPhysics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A stock braced on the shoulder: crouched and still, kick and spread fall by what the stock's pad cushions and its core holds
 * ({@link HandlingPhysics#stock}), scaled by the variant's {@code length}.
 */
public class GunBraced implements ModularTrait {
    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        if (!shooter.isCrouching() || shooter.getDeltaMovement().horizontalDistanceSqr() > 1.0E-3)
            return f;
        var b = HandlingPhysics.stock(p.get("length", 1f), GunFitting.slot(gun, "gun_stock", "core"), GunFitting.slot(gun, "gun_stock", "pad"));
        return f.withKick(b.kick()).withSpread(b.spread());
    }
}
