/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
