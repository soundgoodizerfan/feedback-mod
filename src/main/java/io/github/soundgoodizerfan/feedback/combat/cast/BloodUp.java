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

package io.github.soundgoodizerfan.feedback.combat.cast;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.Rearms;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;

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
 * Proficiency's passive riders on blows: Blood Up (a clean run of empty-hand hits heals) and Breather (a kill with a
 * held weapon gives back stamina by the weapon's mass). They ride hits already happening, so no key.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class BloodUp {

    /** STAND-IN: hits in the run, ticks a run may gap, health healed. */
    static final int RUN = 3, GAP = 60;
    static final float HEAL = 2f;
    /** STAND-IN: stamina per Mu of weapon, and the bounds of one breather. */
    static final float PER_MU = 10f, MIN = 10f, MOST = 40f;

    private record Run(int hits, long last) {
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();

    private BloodUp() {
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0f)
            return;
        if (event.getEntity() instanceof ServerPlayer hurt)
            RUNS.remove(hurt.getUUID()); // a run is clean or it is nothing
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getSource().getDirectEntity() != p
                || !event.getSource().is(DamageTypes.PLAYER_ATTACK) || !p.getMainHandItem().isEmpty()
                || !Skills.unlocked(p, "blood_up"))
            return;
        long now = p.level().getGameTime();
        Run r = RUNS.get(p.getUUID());
        int hits = r == null || now - r.last() > GAP ? 1 : r.hits() + 1;
        if (hits < RUN || !Rearms.ready(p, "blood_up")) {
            RUNS.put(p.getUUID(), new Run(hits, now));
            return;
        }
        RUNS.remove(p.getUUID());
        p.heal(HEAL);
        Rearms.spend(p, "blood_up");
        Vec3 at = p.position().add(0, 1, 0);
        p.serverLevel().sendParticles(ParticleTypes.HEART, at.x, at.y, at.z, 3, 0.4, 0.4, 0.4, 0);
        CastFx.sound(p.serverLevel(), at, SoundEvents.PLAYER_LEVELUP, 0.4f, 1.6f);
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
