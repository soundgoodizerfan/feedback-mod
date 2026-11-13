// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;


import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Great helm (spec 5.4, armour): a projectile does only {@code projectile_mult} of its damage to
 * the wearer, but a polished bucket on the head is seen from far off: mobs judge the wearer
 * {@code visibility_mult} as visible, so they notice from further away. Worn: counts once.
 */
public final class ClosedVisor implements ModularTrait {

    @Override
    public void onWornHurt(LivingIncomingDamageEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        if (event.getSource().is(DamageTypeTags.IS_PROJECTILE))
            event.setAmount(event.getAmount() * params.get("projectile_mult", 0.6f));
    }

    @Override
    public void onWornVisibility(LivingEvent.LivingVisibilityEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        event.modifyVisibility(params.get("visibility_mult", 1.5f));
    }
}
