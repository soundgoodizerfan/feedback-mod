// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.HandlingPhysics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A bipod's legs on the ground: crouched and still, kick and spread fall by how rigid the leg's metal is ({@link HandlingPhysics#bipod}). */
public class GunBipod implements ModularTrait {
    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        if (!shooter.isCrouching() || shooter.getDeltaMovement().horizontalDistanceSqr() > 1.0E-3)
            return f;
        var b = HandlingPhysics.bipod(GunFitting.of(gun, "bipod"));
        return f.withKick(b.kick()).withSpread(b.spread());
    }
}
