// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.soundgoodizerfan.feedback.combat.cast;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * Field dressing (Proficiency, grip of survival): a held cast. Stand still and bind your wounds: health comes back a
 * little each tick and stamina drains as it does, so the bar, not the cast, sets how much you get. Stepping off the
 * spot, taking any damage, full health or an empty bar ends it: it is for the lull between fights, not the fight. Refused at full health. Rearms on the clock, not on a blow: a cast that a
 * fight makes you need should not wait on one.
 *
 * <p>It used to heal six hearts at once. That was the trap a heal falls into: a lump sum on a cooldown is free health
 * in the middle of a fight. Paying for it in stamina and standing still makes it a choice against dodging and casting.
 */
final class FieldDressing {

    /** STAND-IN: a full bar (80 ticks) buys about four hearts; to be felt in play. */
    static final float HEAL_PER_TICK = 0.1f, DRAIN = 1.25f;
    /** How far the feet may drift (squared, horizontal) before it counts as moving. */
    private static final double STILL = 0.3 * 0.3;

    static final Casts.Hold HOLD = new Casts.Hold(DRAIN, FieldDressing::tick, FieldDressing::stop);

    /** Per dresser: where the feet were planted and the health left after the last tick's dressing. */
    private record Dressing(Vec3 at, float health) {
    }

    private static final Map<UUID, Dressing> PLANTED = new HashMap<>();

    private FieldDressing() {
    }

    static boolean act(ServerPlayer p) {
        if (p.getHealth() >= p.getMaxHealth() || Stamina.get(p) < DRAIN)
            return false;
        PLANTED.put(p.getUUID(), new Dressing(p.position(), p.getHealth()));
        CastFx.sound(p.serverLevel(), p.position().add(0, 1, 0), SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1f, 1.2f);
        return true;
    }

    private static boolean tick(ServerPlayer p) {
        Dressing d = PLANTED.get(p.getUUID());
        // health below what the last tick left means something hurt you since
        if (d == null || !p.isAlive() || p.getHealth() >= p.getMaxHealth() || p.getHealth() < d.health()
                || p.position().subtract(d.at()).horizontalDistanceSqr() > STILL) {
            stop(p);
            return false;
        }
        p.heal(HEAL_PER_TICK);
        PLANTED.put(p.getUUID(), new Dressing(d.at(), p.getHealth()));
        if (p.level().getGameTime() % 10 == 0) {
            Vec3 h = p.position().add(0, 1, 0);
            p.serverLevel().sendParticles(ParticleTypes.HEART, h.x, h.y, h.z, 1, 0.4, 0.5, 0.4, 0);
        }
        return true;
    }

    private static void stop(ServerPlayer p) {
        PLANTED.remove(p.getUUID());
    }
}
