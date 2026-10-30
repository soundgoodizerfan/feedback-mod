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

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Heat ward: vanilla fire resistance, carried alongside, paid for in cold. The only cold hazard is vanilla freezing (powder snow), so while the
 * ward lasts frost builds faster and freezing damage is larger ({@link PotionTuning#HEAT_WARD_COLD_DAMAGE}). A fraction, not a kill.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class HeatWard {

    private HeatWard() {}

    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        MobEffectInstance e = event.getEffectInstance();
        if (e.is(FEffects.HEAT_WARD) && !event.getEntity().level().isClientSide)
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, e.getDuration(), 0, e.isAmbient(), false, false));
    }

    /** Frost builds an extra tick every few ticks in powder snow. */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || !p.hasEffect(FEffects.HEAT_WARD) || !p.isInPowderSnow || !p.canFreeze())
            return;
        if (p.tickCount % PotionTuning.HEAT_WARD_COLD_BUILD_EVERY == 0)
            p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze() + 20, p.getTicksFrozen() + 1));
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity who = event.getEntity();
        if (event.getSource().is(DamageTypes.FREEZE) && who.hasEffect(FEffects.HEAT_WARD))
            event.setAmount(event.getAmount() * PotionTuning.HEAT_WARD_COLD_DAMAGE);
    }
}
