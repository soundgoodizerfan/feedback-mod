// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Counterweight: a dense mass running against the bolt. It cancels kick ({@code kick_mult}) and slows the cycle
 * ({@code cycle_mult}); as the bore wears the stroke is cut short and the mass slaps the receiver, so the kick
 * climbs to {@code slap_kick} at full wear, worse than bare. Read live off the stack so the client predicts it.
 */
public class GunCounterweight implements ModularTrait {

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        float kick = Mth.lerp(Math.min(1f, GunCondition.wear(gun)), p.get("kick_mult", 0.85f), p.get("slap_kick", 1.5f));
        return f.withKick(kick).withCycle(p.get("cycle_mult", 1.25f));
    }
}
