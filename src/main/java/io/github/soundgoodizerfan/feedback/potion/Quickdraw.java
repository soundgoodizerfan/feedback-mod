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
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Quickdraw and its payback. While it lasts, a gun's reload takes a fraction of the time ({@code weapon/GunShots.reloadScale} multiplies by
 * {@link #reloadFactor}, so the client's reload estimate and the server agree). It is a reload-only multiplier, not a second haste. When it runs
 * out the stimulant is repaid as mining fatigue, which {@code core/Haste} reads: every gun timer runs slow for a few seconds.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Quickdraw {

    private Quickdraw() {}

    /** Multiplier on reload time: below 1 under quickdraw, deeper per level, never under {@link PotionTuning#QUICKDRAW_FLOOR}. */
    public static float reloadFactor(LivingEntity shooter) {
        MobEffectInstance e = shooter.getEffect(FEffects.QUICKDRAW);
        if (e == null)
            return 1f;
        return Math.max(PotionTuning.QUICKDRAW_FLOOR, 1f - PotionTuning.QUICKDRAW_PER_LEVEL * (e.getAmplifier() + 1));
    }

    /** The draw runs out: the crash, longer for a stronger one. */
    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance e = event.getEffectInstance();
        if (e == null || !e.is(FEffects.QUICKDRAW) || event.getEntity().level().isClientSide)
            return;
        event.getEntity().addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, PotionTuning.QUICKDRAW_CRASH_TICKS * (e.getAmplifier() + 1)));
    }
}
