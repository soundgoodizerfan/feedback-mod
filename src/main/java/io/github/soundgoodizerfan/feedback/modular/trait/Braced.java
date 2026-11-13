// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

/**
 * Cuisses (spec 5.4, armour): thigh plates plant the wearer, so knockback does only
 * {@code knock_mult} of its strength; but the weight drags in water, {@code swim_slow} off the
 * swim speed (a transient attribute modifier, removed when out of water or when the piece comes
 * off). Worn: counts once.
 */
public final class Braced implements ModularTrait {

    private static final ResourceLocation SWIM = Feedback.id("braced_swim");

    @Override
    public void onWornKnockback(LivingKnockBackEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        event.setStrength(event.getStrength() * params.get("knock_mult", 0.6f));
    }

    @Override
    public void onWornTick(Player wearer, ItemStack piece, Params params) {
        AttributeInstance swim = wearer.getAttribute(NeoForgeMod.SWIM_SPEED);
        if (swim == null)
            return;
        boolean wet = wearer.isInWater();
        if (wet && !swim.hasModifier(SWIM))
            swim.addTransientModifier(new AttributeModifier(SWIM, -params.get("swim_slow", 0.15f), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
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
