// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Vanish: true invisibility, held. It holds only while you stand still and the light is low; striking or moving breaks it.
 * In the world it is an uncanny stillness, a stealthiness indistinguishable from invisibility in the dark, never magic.
 * With Long shadow it also holds while you walk crouched.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Vanish {

    private Vanish() {
    }

    static final Casts.Hold HOLD = new Casts.Hold(CastTuning.VANISH_DRAIN, Vanish::tick, Vanish::stop);

    private static final Set<UUID> ACTIVE = new HashSet<>();
    private static final Map<UUID, Vec3> LAST = new HashMap<>();

    /** Whether {@code p} is dark enough and still (or crouch-walking, with Long shadow) to be unseen. */
    private static boolean hidden(ServerPlayer p) {
        Vec3 last = LAST.put(p.getUUID(), p.position());
        double moved = last == null ? 0 : p.position().subtract(last).multiply(1, 0, 1).lengthSqr();
        boolean slow = moved <= CastTuning.VANISH_STILL
                || p.isCrouching() && moved <= CastTuning.VANISH_CROUCH_WALK && Skills.unlocked(p, "long_shadow");
        return slow && !p.isSprinting() && p.level().getMaxLocalRawBrightness(p.blockPosition()) <= CastTuning.VANISH_DARK;
    }

    /** Vanish: starts the hold if you are still and in the dark. */
    static boolean act(ServerPlayer p) {
        LAST.remove(p.getUUID());
        if (!hidden(p) || p.isPassenger())
            return false;
        ACTIVE.add(p.getUUID());
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 10, 0, false, false));
        Vec3 c = p.position().add(0, 1, 0);
        p.serverLevel().sendParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 10, 0.3, 0.6, 0.3, 0.02);
        CastFx.sound(p.serverLevel(), c, SoundEvents.ENDERMAN_TELEPORT, 0.4f, 1.8f);
        return true;
    }

    private static boolean tick(ServerPlayer p) {
        if (!ACTIVE.contains(p.getUUID()))
            return false;
        if (!hidden(p) || p.isPassenger()) {
            stop(p);
            return false;
        }
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 10, 0, false, false));
        return true;
    }

    private static void stop(ServerPlayer p) {
        if (ACTIVE.remove(p.getUUID()))
            p.removeEffect(MobEffects.INVISIBILITY);
        LAST.remove(p.getUUID());
    }

    /** A blow landed gives you away at once. */
    @SubscribeEvent
    public static void onStrike(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof ServerPlayer p && event.getSource().getDirectEntity() == p
                && ACTIVE.contains(p.getUUID())) {
            stop(p);
            Vec3 c = p.position().add(0, 1, 0);
            p.serverLevel().sendParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 14, 0.3, 0.6, 0.3, 0.03);
        }
    }
}
