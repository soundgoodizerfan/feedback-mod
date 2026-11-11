// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * Shaft seal (armour, band trait): a tall tight shaft keeps snow out of the boot. A wearer in powder
 * snow takes no freezing (the freeze ticks are held at 0), but the stiff shaft drags in water:
 * movement is scaled by {@code water_mult} (a transient swim-speed modifier, removed out of water or
 * when the piece comes off). Worn: counts once.
 */
public final class ShaftSeal implements ModularTrait {

    private static final ResourceLocation SWIM = Feedback.id("shaft_seal_swim");

    @Override
    public void onWornTick(Player wearer, ItemStack piece, Params params) {
        if (wearer.isInPowderSnow && wearer.getTicksFrozen() > 0)
            wearer.setTicksFrozen(0);
        AttributeInstance swim = wearer.getAttribute(NeoForgeMod.SWIM_SPEED);
        if (swim == null)
            return;
        boolean wet = wearer.isInWater();
        if (wet && !swim.hasModifier(SWIM))
            swim.addTransientModifier(new AttributeModifier(SWIM, params.get("water_mult", 0.9f) - 1f, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else if (!wet)
            swim.removeModifier(SWIM);
    }

    @Override
    public void onWornRemoved(Player wearer) {
        AttributeInstance swim = wearer.getAttribute(NeoForgeMod.SWIM_SPEED);
        if (swim != null)
            swim.removeModifier(SWIM);
    }
}
