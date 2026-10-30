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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * The Haste potion: vanilla Haste carried alongside for the same length, so {@code core/Haste} (every timer the mod owns: gun cycle and reload,
 * swings, hand tools) and mining speed follow it with no code of their own. When it runs out the stimulant is repaid as mining fatigue, which
 * {@code core/Haste} reads as the opposite: every timer runs slow for a few seconds. The crash hangs off this effect and not off vanilla Haste,
 * so a beacon's Haste pays nothing back.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class HastePotion {

    private HastePotion() {}

    /** Carry the vanilla Haste for the same length and level, no icon or particles of its own. */
    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        MobEffectInstance e = event.getEffectInstance();
        if (e.is(FEffects.HASTE) && !event.getEntity().level().isClientSide)
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, e.getDuration(), e.getAmplifier(), e.isAmbient(), false, false));
    }

    /** The draw runs out: the crash, longer for a stronger one. */
    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance e = event.getEffectInstance();
        if (e == null || !e.is(FEffects.HASTE) || event.getEntity().level().isClientSide)
            return;
        event.getEntity().addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, PotionTuning.HASTE_CRASH_TICKS * (e.getAmplifier() + 1)));
    }
}
