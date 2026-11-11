// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Hauberk (spec 5.4, armour): rings catch a cutting edge, so a blow from a bladed weapon (a modular
 * blade, or anything in the sword or axe tags) does {@code slash_mult} of its damage; but a blow
 * from a crushing one (a modular maul, or the mace-enchantable tag) goes through the rings and the
 * padding at {@code blunt_mult}. Everything else is unchanged. Worn: counts once.
 */
public final class Mail implements ModularTrait {

    @Override
    public void onWornHurt(LivingIncomingDamageEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        if (!(event.getSource().getDirectEntity() instanceof LivingEntity attacker))
            return;
        ItemStack weapon = io.github.soundgoodizerfan.feedback.modular.StrikeSource.weapon(attacker, event.getSource());
        ModularType.Family family = io.github.soundgoodizerfan.feedback.modular.ModularFaces.combat(weapon);
        if (family == ModularType.Family.MACE || (family == null && weapon.is(ItemTags.MACE_ENCHANTABLE)))
            event.setAmount(event.getAmount() * params.get("blunt_mult", 1.3f));
        else if (family == ModularType.Family.BLADE || (family == null && (weapon.is(ItemTags.SWORDS) || weapon.is(ItemTags.AXES))))
            event.setAmount(event.getAmount() * params.get("slash_mult", 0.75f));
    }
}
