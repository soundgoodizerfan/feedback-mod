// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * Leg drag (armour, band trait): wide or heavy leg plates cost effort. A sprinting wearer burns
 * {@code sprint_exhaust} extra exhaustion every tick, and in water movement is scaled by
 * {@code water_mult} (a transient swim-speed modifier, removed out of water or when the piece comes off).
 * Worn: counts once.
 */
public final class LegDrag implements ModularTrait {

    private static final ResourceLocation SWIM = Feedback.id("leg_drag_swim");

    @Override
    public void onWornTick(Player wearer, ItemStack piece, Params params) {
        if (wearer.isSprinting())
            wearer.causeFoodExhaustion(params.get("sprint_exhaust", 0.01f));
        AttributeInstance swim = wearer.getAttribute(NeoForgeMod.SWIM_SPEED);
        if (swim == null)
            return;
        boolean wet = wearer.isInWater();
        if (wet && !swim.hasModifier(SWIM))
            swim.addTransientModifier(new AttributeModifier(SWIM, params.get("water_mult", 0.85f) - 1f, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
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
