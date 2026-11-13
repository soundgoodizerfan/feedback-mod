// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Resonance weight: a weight threaded on the barrel tunes its node to one muzzle speed ({@code tuned_ms}, m/s). A
 * round near that speed shoots a tight group ({@code 0.7}); the further off it is, in ratio, the wider the group
 * opens, past 1 (worse than a bare barrel) beyond about a third off. The position is the part's shape, so the
 * choice is three named positions, never a dial. Read off the figures as aimed, so wear and heat that slow the round
 * detune it.
 */
public class GunResonance implements ModularTrait {

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        float speed = f.muzzleSpeed() * 20f, tuned = p.get("tuned_ms", 450f);
        if (speed <= 0f)
            return f;
        float off = Math.abs((float) Math.log(speed / tuned));
        return f.withSpread(Mth.clamp(0.7f + off * 1.2f, 0.7f, 1.5f));
    }
}
