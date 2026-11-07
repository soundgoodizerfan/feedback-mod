// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;

/** Stampede: a roar over a fire that sends everything burning bolting from you. */
final class Stampede {

    private Stampede() {
    }

    /** STAND-INS: how far the roar carries, how far they run and how fast, and the ticks of the dash. */
    private static final double RANGE = 9;
    private static final int RUN = 12, DASH_TICKS = 80;

    /** Stampede: every burning creature within nine blocks that can run bolts away from you at a gallop; the fire they carry passes to whatever they pass. Nothing burning near, nothing happens. */
    static boolean act(ServerPlayer p) {
        ServerLevel lv = p.serverLevel();
        int n = 0;
        for (LivingEntity e : near(p, p.position(), RANGE)) {
            if (!e.isOnFire() || !(e instanceof PathfinderMob m))
                continue;
            Vec3 to = DefaultRandomPos.getPosAway(m, RUN, 6, p.position());
            if (to == null)
                continue;
            m.getNavigation().moveTo(to.x, to.y, to.z, 1.7);
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DASH_TICKS, 1));
            Disorder.kindled(p, m);
            Disorder.note(p, m, Disorder.Kind.PANICKED);
            lv.sendParticles(ParticleTypes.ANGRY_VILLAGER, m.getX(), m.getY() + m.getBbHeight() + 0.3, m.getZ(), 3, 0.2, 0.1, 0.2, 0);
            n++;
        }
        if (n == 0)
            return false;
        CastFx.ring(lv, ParticleTypes.SMOKE, p.position().add(0, 0.2, 0), 1.0, 20, 0.35);
        CastFx.sound(lv, p.position(), SoundEvents.RAVAGER_ROAR, 1f, 0.9f);
        CastFx.sound(lv, p.position(), SoundEvents.HORSE_GALLOP, 0.8f, 0.6f);
        return true;
    }
}
