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
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Springheel and its payback. Jump height is a jump-strength attribute modifier on the effect itself ({@code FEffects.SPRINGHEEL}). A landing
 * from a real fall lets only a fraction of the damage through and springs the body back up by a fraction of the arrival speed; sneaking lands
 * dead. Vanilla zeroes the vertical speed right after the fall event, so the bounce is stashed and applied on the next player tick. Both sides
 * run it: the client owns a player's movement. When it ends the legs repay it as slowness.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Springheel {

    private static final String BOUNCE = "feedback_springheel_bounce";

    private Springheel() {}

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player p) || !p.hasEffect(FEffects.SPRINGHEEL))
            return;
        event.setDamageMultiplier(event.getDamageMultiplier() * PotionTuning.SPRINGHEEL_PASS);
        if (!p.isShiftKeyDown() && event.getDistance() > PotionTuning.SPRINGHEEL_BOUNCE_MIN)
            // arrival speed from the fall height (gravity 0.08 per tick squared), as the bounce's base
            p.getPersistentData().putDouble(BOUNCE, Math.sqrt(2 * 0.08 * event.getDistance()) * PotionTuning.SPRINGHEEL_BOUNCE);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!p.getPersistentData().contains(BOUNCE))
            return;
        double v = p.getPersistentData().getDouble(BOUNCE);
        p.getPersistentData().remove(BOUNCE);
        p.setDeltaMovement(p.getDeltaMovement().x, v, p.getDeltaMovement().z);
        p.setOnGround(false);
        p.hurtMarked = true;
    }

    /** The spring runs out: the crash, longer for a stronger one. */
    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance e = event.getEffectInstance();
        if (e == null || !e.is(FEffects.SPRINGHEEL) || event.getEntity().level().isClientSide)
            return;
        event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PotionTuning.SPRINGHEEL_CRASH_TICKS * (e.getAmplifier() + 1)));
    }
}
