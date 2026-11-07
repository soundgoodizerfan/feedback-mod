// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * War hammer (spec 5.4): the blow ignores {@code share} of the target's armour and toughness, and
 * dents what it hits through: an armoured target is slowed {@code dent_seconds}. Against bare
 * flesh it is only a small hammer. The cost is the small head's weak stagger and mass.
 */
public final class Pierce implements ModularTrait {

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        LivingEntity target = event.getEntity();
        float armor = target.getArmorValue();
        if (armor <= 0)
            return;
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float keep = 1f - params.get("share", 0.5f);
        float amount = event.getAmount();
        float with = CombatRules.getDamageAfterAbsorb(target, amount, event.getSource(), armor, toughness);
        float without = CombatRules.getDamageAfterAbsorb(target, amount, event.getSource(), armor * keep, toughness * keep);
        if (with > 0)
            event.setAmount(amount * without / with);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.round(params.get("dent_seconds", 4f) * 20), 0), attacker);
    }
}
