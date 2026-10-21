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
