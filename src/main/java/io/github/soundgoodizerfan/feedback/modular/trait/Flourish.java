// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Cutlass (spec 5.4): a kill readies the next swing at once and grants a short speed boost
 * ({@code boost_ticks}); a hit does {@code crowd_mult} when {@code crowd} or more other hostiles
 * stand within 3 blocks of the wielder. The cost is the condition: the bonuses come only from a
 * kill or from being surrounded.
 */
public final class Flourish implements ModularTrait {

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        int near = attacker.level().getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(3),
                e -> e != attacker && e != event.getEntity() && e instanceof Enemy).size();
        if (near >= params.get("crowd", 2f))
            event.setAmount(event.getAmount() * params.get("crowd_mult", 1.2f));
    }

    @Override
    public void onKill(LivingDeathEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        if (attacker instanceof Player player)
            player.resetAttackStrengthTicker();
        attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Math.round(params.get("boost_ticks", 40f))));
    }
}
