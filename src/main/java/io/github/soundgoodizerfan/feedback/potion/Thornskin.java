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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FEffects;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Thornskin. A creature that strikes the wearer in melee takes a share of the damage the wearer took (after armour), capped per hit. The reflected
 * hit is vanilla thorns damage, which this handler ignores, so two thornskin wearers cannot bounce a blow forever. The price is a speed modifier
 * on the effect itself ({@link FEffects#THORNSKIN}).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Thornskin {

    private Thornskin() {}

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide || event.getNewDamage() <= 0f)
            return;
        MobEffectInstance e = wearer.getEffect(FEffects.THORNSKIN);
        DamageSource src = event.getSource();
        // melee only: the blow's direct and causing entity are the same creature (no projectile, no explosion)
        if (e == null || src.is(DamageTypes.THORNS) || !(src.getDirectEntity() instanceof LivingEntity attacker) || attacker != src.getEntity()
                || attacker == wearer)
            return;
        float back = Math.min(event.getNewDamage() * PotionTuning.THORNSKIN_SHARE * (e.getAmplifier() + 1), PotionTuning.THORNSKIN_CAP);
        attacker.hurt(wearer.damageSources().thorns(wearer), back);
    }
}
