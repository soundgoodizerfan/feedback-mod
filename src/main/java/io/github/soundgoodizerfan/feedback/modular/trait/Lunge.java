// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Javelin (spec 5.4): a blow struck at a run lunges, {@code sprint_mult} of the blow; one struck
 * standing still is only {@code still_mult}. The cost is the run-up, and that the javelin has no
 * charge held out to make up for it (that is the spear's).
 */
public final class Lunge implements ModularTrait {

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        event.setAmount(event.getAmount() * (attacker.isSprinting() ? params.get("sprint_mult", 1.4f) : params.get("still_mult", 0.85f)));
    }
}
