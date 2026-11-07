// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Chest valve of a steam launcher: how much steam each shot lets out ({@code steam_mult}, read by {@code GunSteam}) and
 * how fast the gun cycles ({@code cycle_mult}). A metered valve spends little steam and cycles slowly; a dump valve spends
 * a lot and cycles fast. Does nothing on a gun that is not a steam launcher.
 */
public class GunValve implements ModularTrait {

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        return GunSpec.of(gun).filter(g -> g.launcher() == GunSpec.Launcher.STEAM).isPresent() ? f.withCycle(p.get("cycle_mult", 1f)) : f;
    }
}
