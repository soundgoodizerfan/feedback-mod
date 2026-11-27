// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.combat.tether.Tether;
import io.github.soundgoodizerfan.feedback.combat.tether.Tethers;
import io.github.soundgoodizerfan.feedback.registry.FCombat;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;

/**
 * Pin by weight (Brute): kneel on a body that is down (staggered) or already held by a line, and your mass holds it. It cannot move, and
 * every blow on it lands harder (a {@link Tether} with vulnerability above 1, anchored on you); how long it takes to struggle free is the
 * ratio of your mass to its: a body near your weight is up in a couple of seconds, a light one stays down far longer, one heavy enough throws
 * you off at once. The pin ends when the time runs out, when you move off, when you take a real blow, or when you use it again.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Pin {

    private Pin() {
    }

    private record Hold(UUID target, long until) {
    }

    private static final Map<UUID, Hold> HOLDS = new HashMap<>();

    /** STAND-INS: struggle time of an equal-weight body (ticks), the shortest and longest it can be, the ratio under which you cannot hold it at all, blocks you may stray, the vulnerability a pinned body has, and the blow that breaks your hold. */
    private static final int BASE_TICKS = 40, MIN_TICKS = 10, MAX_TICKS = 240;
    private static final float THROWN_OFF_RATIO = 0.6f, VULNERABILITY = 1.5f, BREAK_DAMAGE = 4f;
    private static final double REACH = 2.2, STRAY = 3.0;

    /** Pin: kneel on the staggered or held body in front of you; used again, you let it up. */
    static boolean act(ServerPlayer p) {
        Hold held = HOLDS.get(p.getUUID());
        if (held != null) {
            release(p);
            return false;
        }
        LivingEntity t = ahead(p, REACH, 0.4);
        if (t == null || !(CombatState.of(t).phase() == CombatPhase.STAGGER || t.hasData(FCombat.TETHERS) && !t.getData(FCombat.TETHERS).all().isEmpty()))
            return false;
        float mine = BodyMass.of(p) + BodyMass.armour(p), theirs = Math.max(1f, BodyMass.of(t) + BodyMass.armour(t));
        float ratio = mine / theirs;
        if (ratio < THROWN_OFF_RATIO) {
            // Too heavy to hold: it shrugs you off.
            CombatTicker.stagger(p, 15, ParryTuning.STAGGER_SLOW);
            CastFx.pulse(p.serverLevel(), p.position().add(0, 0.1, 0), Vec3.ZERO, CastFx.STEEL, 1.2f, 7);
            CastFx.sound(p.serverLevel(), p.position(), FSounds.CAST_GUARD.get(), 0.8f, 0.8f);
            return true;
        }
        int ticks = Mth.clamp(Math.round(BASE_TICKS * ratio), MIN_TICKS, MAX_TICKS);
        Tethers.add(t, new Tether(p.getUUID(), new Tether.Anchor.OnEntity(p.getId()), 2f, 0f, VULNERABILITY, new Tether.Release(0, ticks)));
        HOLDS.put(p.getUUID(), new Hold(t.getUUID(), p.level().getGameTime() + ticks));
        CastFx.pulse(p.serverLevel(), t.position().add(0, 0.1, 0), Vec3.ZERO, CastFx.STEEL, 1.4f, 8);
        CastFx.sound(p.serverLevel(), t.position(), FSounds.CAST_SLAM.get(), 0.8f, 1.2f);
        return true;
    }

    private static void release(ServerPlayer p) {
        Hold h = HOLDS.remove(p.getUUID());
        if (h != null && p.serverLevel().getEntity(h.target()) instanceof LivingEntity t)
            Tethers.remove(t, p.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !HOLDS.containsKey(p.getUUID()))
            return;
        Hold h = HOLDS.get(p.getUUID());
        if (!(p.serverLevel().getEntity(h.target()) instanceof LivingEntity t) || !t.isAlive() || !p.isAlive()
                || p.level().getGameTime() >= h.until() || p.distanceTo(t) > STRAY || p.isPassenger()) {
            release(p);
            return;
        }
        Vec3 m = t.getDeltaMovement();
        t.setDeltaMovement(0, Math.min(0, m.y), 0);
        t.hurtMarked = true;
        if (CombatState.of(t).phase() != CombatPhase.ROLL)
            CombatTicker.stagger(t, 2, ParryTuning.STAGGER_SLOW);
    }

    @SubscribeEvent
    public static void onDamaged(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (event.getNewDamage() >= BREAK_DAMAGE && event.getEntity() instanceof ServerPlayer p && HOLDS.containsKey(p.getUUID()))
            release(p);
    }
}
