// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Work;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/**
 * Shoulder charge (Brute): a short sprint, shoulder first, that bowls over what it meets by the ratio of your mass to theirs. A body well lighter
 * than you is sent flying along the charge; one near your weight is staggered where it stands and stops the charge; one much heavier than you
 * stops you instead and leaves you staggered. Each body is hit once per charge.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class ShoulderCharge {

    private ShoulderCharge() {
    }

    private record Run(long until, Set<UUID> hit) {
    }

    private static final Map<UUID, Run> RUNNING = new HashMap<>();

    /** STAND-INS: ticks of the sprint, blocks per tick it covers, reach in front, the ratio above which a body is sent flying and the one below which it holds you, and the speed a flying body gets per unit of ratio (m/s, capped). */
    private static final int TICKS = 10;
    private static final double SPEED = 0.55, REACH = 1.3;
    private static final float FLY_RATIO = 1.6f, HOLD_RATIO = 0.55f, FLY_PER_RATIO = 3f, FLY_MAX = 14f;

    /** Shoulder charge: ten ticks of sprint along your look; needs your feet on the ground. */
    static boolean act(ServerPlayer p) {
        if (!p.onGround() || p.isPassenger() || p.getLookAngle().multiply(1, 0, 1).lengthSqr() < 1e-6)
            return false;
        RUNNING.put(p.getUUID(), new Run(p.level().getGameTime() + TICKS, new HashSet<>()));
        CastFx.sound(p.serverLevel(), p.position(), FSounds.CAST_WHOOSH.get(), 1f, 0.7f);
        CastFx.trail(p.serverLevel(), p, new Vec3(0, 1.1, 0), CastFx.STEEL, 0.2f, TICKS);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !RUNNING.containsKey(p.getUUID()))
            return;
        Run run = RUNNING.get(p.getUUID());
        long now = p.level().getGameTime();
        Vec3 d = p.getLookAngle().multiply(1, 0, 1);
        if (now >= run.until() || d.lengthSqr() < 1e-6 || p.isPassenger() || p.isInWater()) {
            RUNNING.remove(p.getUUID());
            return;
        }
        d = d.normalize();
        p.setDeltaMovement(d.x * SPEED, p.getDeltaMovement().y, d.z * SPEED);
        p.hurtMarked = true;
        float mine = BodyMass.of(p) + BodyMass.armour(p);
        ServerLevel lv = p.serverLevel();
        for (LivingEntity e : near(p, p.position().add(d.scale(REACH * 0.7)).add(0, 0.9, 0), REACH)) {
            if (!run.hit().add(e.getUUID()))
                continue;
            float theirs = Math.max(1f, BodyMass.of(e) + BodyMass.armour(e));
            float ratio = mine / theirs;
            CastFx.debris(lv, e.position(), 6, 0.3);
            CastFx.sound(lv, e.position(), FSounds.CAST_PUNCH.get(), 1f, 0.7f);
            CastFx.pulse(lv, e.position().add(0, 0.9, 0), d, CastFx.STEEL, 1.4f, 7);
            CastFx.shards(lv, e.position().add(0, 0.9, 0), 0xFFFFFF, 1.2f, 6);
            if (ratio >= FLY_RATIO) {
                float v = Mth.clamp(ratio * FLY_PER_RATIO, 3f, FLY_MAX);
                e.setDeltaMovement(d.x * v / FTuning.TICKS_PER_SECOND, 0.3, d.z * v / FTuning.TICKS_PER_SECOND);
                e.hurtMarked = true;
                stagger(p, e, new Work(mine * FTuning.GRAVITY_BLOCKS_PER_S2));
            } else if (ratio > HOLD_RATIO) {
                stagger(p, e, new Work(mine * FTuning.GRAVITY_BLOCKS_PER_S2 * ratio));
                RUNNING.remove(p.getUUID());
                return;
            } else {
                // Too heavy to move: the charge breaks on it and the charger is rocked.
                CombatTicker.stagger(p, ParryTuning.staggerTicks(new Work(theirs * FTuning.GRAVITY_BLOCKS_PER_S2 * 0.5f), mine), ParryTuning.STAGGER_SLOW);
                p.setDeltaMovement(-d.x * 0.3, 0.1, -d.z * 0.3);
                p.hurtMarked = true;
                RUNNING.remove(p.getUUID());
                return;
            }
        }
    }
}
