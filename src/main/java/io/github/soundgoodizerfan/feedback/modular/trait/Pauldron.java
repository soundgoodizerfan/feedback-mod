// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.combat.StrikeSource;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Pauldron (spec 5.3, armour addon on a chestplate): a plate over the shoulder takes the downward blow. An overhead
 * blow (a weapon with the {@code overhead} trait, in melee) does {@code overhead_mult} of its damage to the wearer, and
 * a falling block, anvil or stalactite {@code falling_mult}. Worn: counts once.
 */
public final class Pauldron implements ModularTrait {

    @Override
    public void onWornHurt(LivingIncomingDamageEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        DamageSource source = event.getSource();
        if (source.is(DamageTypes.FALLING_BLOCK) || source.is(DamageTypes.FALLING_ANVIL) || source.is(DamageTypes.FALLING_STALACTITE)) {
            event.setAmount(event.getAmount() * params.get("falling_mult", 0.6f));
            return;
        }
        if (source.getDirectEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = StrikeSource.weapon(attacker, source);
            if (ModularTraits.actives(weapon).stream().anyMatch(a -> a.id().equals("overhead")))
                event.setAmount(event.getAmount() * params.get("overhead_mult", 0.7f));
        }
    }
}
