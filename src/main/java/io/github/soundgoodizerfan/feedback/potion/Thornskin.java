// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
