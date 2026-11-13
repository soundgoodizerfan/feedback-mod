// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.weapon.GunFigures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Lit reticle: a dot of glowing mineral in the sight, no battery to run flat. It shows only in the dark (a light level
 * of {@code dark_at} or less at the shooter): there the group tightens by {@code spread_cut}, times the stuff's
 * luminance over 15. In daylight the dot washes out and does nothing.
 */
public class GunReticle implements ModularTrait {

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        var m = GunFitting.of(gun, "lit_reticle");
        if (m == null || shooter.level().getMaxLocalRawBrightness(shooter.blockPosition()) > p.get("dark_at", 7f))
            return f;
        return f.withSpread(1f - p.get("spread_cut", 0.2f) * m.luminance().orElse(0) / 15f);
    }
}
