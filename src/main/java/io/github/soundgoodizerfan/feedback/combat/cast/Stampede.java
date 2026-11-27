// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
            CastFx.streak(lv, p.position().add(0, 1, 0), m.position().add(0, m.getBbHeight() / 2, 0), CastFx.EMBER, 0.03f, 5);
            n++;
        }
        if (n == 0)
            return false;
        CastFx.pulse(lv, p.position().add(0, 0.2, 0), Vec3.ZERO, CastFx.EMBER, (float) RANGE * 0.6f, 12);
        CastFx.pulse(lv, p.position().add(0, 0.2, 0), Vec3.ZERO, 0xFFFFFF, (float) RANGE * 0.3f, 8);
        CastFx.sound(lv, p.position(), FSounds.CAST_SURGE.get(), 1f, 0.7f);
        CastFx.sound(lv, p.position(), FSounds.CAST_SLAM.get(), 0.8f, 0.8f);
        return true;
    }
}
