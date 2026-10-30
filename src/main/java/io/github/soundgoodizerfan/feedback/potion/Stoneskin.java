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

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Stoneskin: incoming damage is cut to a fraction (never nothing). Knockback resistance and the slowing (the price of a heavy body) are attribute
 * modifiers on the effect itself ({@link FEffects#STONESKIN}), so they scale per level and vanish with it.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Stoneskin {

    private Stoneskin() {}

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity who = event.getEntity();
        MobEffectInstance e = who.getEffect(FEffects.STONESKIN);
        if (e == null || who.level().isClientSide)
            return;
        float f = Math.max(PotionTuning.STONESKIN_DAMAGE_FLOOR, PotionTuning.STONESKIN_DAMAGE - PotionTuning.STONESKIN_DAMAGE_PER_LEVEL * e.getAmplifier());
        if (who.hasEffect(MobEffects.DAMAGE_RESISTANCE))
            f = Math.max(f, PotionTuning.STONESKIN_WITH_RESISTANCE);
        event.setAmount(event.getAmount() * f);
    }
}
