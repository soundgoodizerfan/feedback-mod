// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunOptions;
import io.github.soundgoodizerfan.feedback.weapon.HandlingPhysics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Counterweight: a mass running against the bolt. It cancels kick by its share of the gun's mass and slows the cycle by the same
 * ({@link HandlingPhysics}); as the bore wears the stroke is cut short and the mass slaps the receiver, so the kick climbs at full
 * wear to worse than bare, the more so the more brittle its metal. Read live off the stack so the client predicts it.
 */
public class GunCounterweight implements ModularTrait {
    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        var m = GunFitting.of(gun, "counterweight");
        var option = GunOptions.ofForm("counterweight");
        if (m == null || option.isEmpty())
            return f;
        float share = HandlingPhysics.share(m, option.get(), f.gunMass().value());
        float kick = Mth.lerp(Math.min(1f, GunCondition.wear(gun)), HandlingPhysics.counterKick(share), HandlingPhysics.counterSlap(m));
        return f.withKick(kick).withCycle(HandlingPhysics.counterCycle(share));
    }
}
