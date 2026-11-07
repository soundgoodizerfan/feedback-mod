// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Breastplate (spec 5.4, armour): a stiff shell spreads a fall or a blast over the whole trunk, so
 * both do only {@code absorb_mult} of their damage to the wearer. The cost is the big solid shell
 * (more cells, so the heaviest chest piece). Worn: counts once.
 */
public final class Rigid implements ModularTrait {

    @Override
    public void onWornHurt(LivingIncomingDamageEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        if (event.getSource().is(DamageTypeTags.IS_FALL) || event.getSource().is(DamageTypeTags.IS_EXPLOSION))
            event.setAmount(event.getAmount() * params.get("absorb_mult", 0.7f));
    }
}
