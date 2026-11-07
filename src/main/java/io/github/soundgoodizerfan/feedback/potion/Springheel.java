// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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

    private static final String BOUNCE = "feedback_springheel_bounce", SOFT = "feedback_springheel_soft";

    private Springheel() {}

    /** A thrown flask: the body is launched up once and its next landing is softened (any creature, no effect applied). */
    public static void launch(net.minecraft.world.entity.LivingEntity body, int amplifier) {
        body.setDeltaMovement(body.getDeltaMovement().x, PotionTuning.SPRINGHEEL_LAUNCH * (amplifier + 1), body.getDeltaMovement().z);
        body.setOnGround(false);
        body.hurtMarked = true;
        body.getPersistentData().putBoolean(SOFT, true);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity().getPersistentData().contains(SOFT)) { // launched by a thrown flask: the landing is softened, on any body
            event.getEntity().getPersistentData().remove(SOFT);
            event.setDamageMultiplier(event.getDamageMultiplier() * PotionTuning.SPRINGHEEL_SOFT_PASS);
        }
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
