// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.modular.ModularTraits;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * Poleyn (spec 5.3, armour addon on leggings): a cap over the knee takes the landing and the kick. A fall does
 * {@code fall_mult} of its damage, and a kick or a slide's trip staggers the wearer for {@code stagger_mult} of the
 * time ({@link #staggerMult}). Worn: counts once.
 */
public final class Poleyn implements ModularTrait {

    @Override
    public void onWornFall(LivingFallEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        event.setDamageMultiplier(event.getDamageMultiplier() * params.get("fall_mult", 0.75f));
    }

    /** What {@code target}'s stagger from a kick or a leg sweep is multiplied by: 1 without poleyns. */
    public static float staggerMult(LivingEntity target) {
        for (ModularTraits.Worn w : ModularTraits.worn(target))
            if (w.trait() instanceof Poleyn)
                return w.params().get("stagger_mult", 0.5f);
        return 1f;
    }
}
