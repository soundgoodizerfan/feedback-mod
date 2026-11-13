// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Cramped (armour, band trait): a stiff or tight fit that folds badly, so while the wearer crouches
 * their movement is scaled by {@code sneak_mult} (a transient movement-speed modifier, removed on
 * standing or when the piece comes off). Worn: counts once.
 */
public final class Cramped implements ModularTrait {

    private static final ResourceLocation SNEAK = Feedback.id("cramped_sneak");

    @Override
    public void onWornTick(Player wearer, ItemStack piece, Params params) {
        AttributeInstance speed = wearer.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null)
            return;
        boolean crouched = wearer.isCrouching();
        if (crouched && !speed.hasModifier(SNEAK))
            speed.addTransientModifier(new AttributeModifier(SNEAK, params.get("sneak_mult", 0.7f) - 1f, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else if (!crouched)
            speed.removeModifier(SNEAK);
    }

    @Override
    public void onWornRemoved(Player wearer) {
        AttributeInstance speed = wearer.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null)
            speed.removeModifier(SNEAK);
    }
}
