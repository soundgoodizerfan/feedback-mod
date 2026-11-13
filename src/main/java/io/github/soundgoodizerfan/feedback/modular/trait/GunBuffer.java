// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Hydraulic buffer: a sealed chamber of something thick that the recoil has to push through. Its grip on the kick is
 * the fluid's viscosity (relative to water, as a decade count over {@code decades}: honey is four), thinned as the
 * barrel heats until at the round's ignition it is water ({@code damping} reaches 0). Kick falls to {@code kick_floor}
 * at full damping and the cycle slows by {@code cycle_slow} with it. The part must be made of a sticky stuff, so the
 * fluid is the part's material. shortcut: no tank to leak or drain, the fluid is the part; add one when a fitting can hold a fluid.
 */
public class GunBuffer implements ModularTrait {

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        var m = GunFitting.of(gun, "hydraulic_buffer");
        if (m == null)
            return f;
        float viscosity = m.molten().viscosity().orElse(1f);
        float thick = Math.min(1f, (float) Math.log10(Math.max(1f, viscosity)) / p.get("decades", 4f));
        float damping = thick * (1f - Math.min(1f, GunCondition.heatFraction(gun, shooter.level(), FTuning.GUN_PROPELLANT_IGNITION_TU)));
        return f.withKick(1f - (1f - p.get("kick_floor", 0.5f)) * damping).withCycle(1f + p.get("cycle_slow", 0.3f) * damping);
    }
}
