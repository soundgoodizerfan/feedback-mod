// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Quake Leap: vault forward and up; the landing shakes everything near. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class QuakeLeap {

    private QuakeLeap() {
    }

    /** In the air: the highest point it reached, for the fall that lands it. */
    private static final java.util.Map<java.util.UUID, double[]> LEAPING = new java.util.HashMap<>();

    /** Vaults forward and up; the landing shakes everything near, by the mass that comes down and how far. */
    static boolean act(ServerPlayer p) {
        if (!p.onGround())
            return false;
        Vec3 f = p.getLookAngle().multiply(1, 0, 1);
        f = f.lengthSqr() < 1e-6 ? Vec3.ZERO : f.normalize().scale(0.7);
        p.setDeltaMovement(f.x, 1.05, f.z);
        p.hurtMarked = true;
        LEAPING.put(p.getUUID(), new double[]{p.getY(), p.level().getGameTime()});
        ServerLevel lv = p.serverLevel();
        Vec3 at = p.position().add(0, 0.1, 0);
        CastFx.pulse(lv, at, Vec3.ZERO, CastFx.STEEL, 1.8f, 8);
        CastFx.column(lv, at, CastFx.STEEL, 1.8f, 8);
        CastFx.debris(lv, at, 24, 0.6);
        CastFx.sound(lv, at, FSounds.CAST_WHOOSH.get(), 0.9f, 0.8f);
        CastFx.sound(lv, at, FSounds.CAST_SLAM.get(), 0.5f, 1.2f);
        CombatCues.everyone(p, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 0.7f);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        double[] air = LEAPING.get(p.getUUID());
        if (air == null)
            return;
        air[0] = Math.max(air[0], p.getY());
        p.fallDistance = 0f; // the leap lands without hurting
        if (!p.onGround()) {
            if (p.level().getGameTime() % 4 == 0)
                CastFx.trail(p.serverLevel(), p, new Vec3(0, 0.4, 0), CastFx.STEEL, 0.14f, 6);
        }
        if (p.level().getGameTime() - (long) air[1] < 4 || !p.onGround())
            return;
        LEAPING.remove(p.getUUID());
        double fall = Math.max(0.5, air[0] - p.getY() + 1.5);
        float work = (float) ((BodyMass.of(p) + BodyMass.armour(p)) * FTuning.GRAVITY_BLOCKS_PER_S2 * fall);
        ServerLevel level = p.serverLevel();
        for (LivingEntity e : near(p, p.position(), 4.5)) {
            Vec3 away = e.position().subtract(p.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1e-6 ? Vec3.ZERO : away.normalize();
            e.push(away.x * 0.6, 0.35, away.z * 0.6);
            e.hurtMarked = true;
            stagger(p, e, new Work(work));
        }
        Vec3 land = p.position().add(0, 0.1, 0);
        CastFx.smash(level, land, 750, fall > 5);
        level.playSound(null, p.blockPosition(), FSounds.FEEL_HEAVY.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.6f);
    }
}
