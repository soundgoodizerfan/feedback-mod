// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.soundgoodizerfan.feedback.combat.cast;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.Rearms;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Proficiency's passive riders on blows: Blood Up (warm muscles: a clean run of hits, none taken between, lifts stamina regain for a while; it heals nothing) and Breather (a kill with a
 * held weapon gives back stamina by the weapon's mass). They ride hits already happening, so no key.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class BloodUp {

    /** STAND-IN: hits in the run, ticks a run may gap. */
    static final int RUN = 3, GAP = 60;
    /** STAND-IN: stamina per Mu of weapon, and the bounds of one breather. */
    static final float PER_MU = 10f, MIN = 10f, MOST = 40f;

    private record Run(int hits, long last) {
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();
    /** Game tick each fighter's warm muscles cool. */
    private static final Map<UUID, Long> WARM = new HashMap<>();

    /** The factor on stamina regain: warm muscles ({@link CastTuning#WARM_REGEN}) while they last, else 1. */
    static float warmFactor(ServerPlayer p) {
        Long until = WARM.get(p.getUUID());
        if (until == null)
            return 1f;
        if (p.level().getGameTime() > until) {
            WARM.remove(p.getUUID());
            return 1f;
        }
        return CastTuning.WARM_REGEN;
    }

    private BloodUp() {
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0f)
            return;
        if (event.getEntity() instanceof ServerPlayer hurt)
            RUNS.remove(hurt.getUUID()); // a run is clean or it is nothing
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getSource().getDirectEntity() != p
                || !event.getSource().is(DamageTypes.PLAYER_ATTACK) || !Skills.unlocked(p, "blood_up"))
            return;
        long now = p.level().getGameTime();
        Run r = RUNS.get(p.getUUID());
        int hits = r == null || now - r.last() > GAP ? 1 : r.hits() + 1;
        if (hits < RUN || !Rearms.ready(p, "blood_up")) {
            RUNS.put(p.getUUID(), new Run(hits, now));
            return;
        }
        RUNS.remove(p.getUUID());
        WARM.put(p.getUUID(), now + CastTuning.WARM_TICKS);
        Rearms.spend(p, "blood_up");
        Vec3 at = p.position().add(0, 1, 0);
        p.serverLevel().sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.6, at.z, 6, 0.3, 0.3, 0.3, 0.02);
        CastFx.sound(p.serverLevel(), at, SoundEvents.PLAYER_BREATH, 0.6f, 0.9f);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || !Skills.unlocked(p, "breather"))
            return;
        ModularFigures f = ModularStacks.figures(p.getMainHandItem());
        if (f == null || f.mass().value() <= 0f)
            return;
        Stamina.gain(p, Math.max(MIN, Math.min(MOST, f.mass().value() * PER_MU)));
        Vec3 at = p.position().add(0, 1.6, 0);
        p.serverLevel().sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 5, 0.2, 0.2, 0.2, 0.02);
        CastFx.sound(p.serverLevel(), at, SoundEvents.PLAYER_BREATH, 0.8f, 1.1f);
    }
}
