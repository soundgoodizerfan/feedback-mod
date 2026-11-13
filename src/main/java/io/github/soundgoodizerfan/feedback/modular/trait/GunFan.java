// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.weapon.GunFigures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fan (spec 8.4): a hammer and a cylinder. Crouched, the shooter dumps the cylinder as fast as the hand can slap
 * the hammer -- a rate no cycle limit gives, {@code fan_ms} between shots, held by the attack button -- paid in
 * spread ({@code spread_mult}), kick ({@code kick_mult}) and a hot cylinder ({@code heat_mult}). Simplified: the
 * cost is a flat multiple while fanning, not one that grows with each shot of the streak.
 */
public class GunFan implements ModularTrait {

    /** Whether {@code shooter} is fanning now: crouched. The client's fire loop asks the same. */
    public static boolean fanning(Player shooter) {
        return shooter.isCrouching();
    }

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        if (!fanning(shooter))
            return f;
        return f.withCycleMs(Math.round(p.get("fan_ms", 140f))).withSpread(p.get("spread_mult", 2f))
                .withKick(p.get("kick_mult", 1.3f)).withHeat(p.get("heat_mult", 1.5f));
    }
}
