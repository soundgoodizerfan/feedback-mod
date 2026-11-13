// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.soundgoodizerfan.feedback.combat.cast;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * Aid (Proficiency): the things you do for another player. Every one needs an ally (another {@link ServerPlayer}) in
 * reach and fails at no cost with none. Stamina is the only meter and it is yours: Tend drains it while the ally
 * mends, Cut loose pays its cast cost. Each delivery pays a small trickle
 * into Proficiency ({@link SkillEarning#aided}), decaying per ally.
 */
final class Aid {

    /** STAND-IN [OPEN]: Tend reach, blocks. */
    static final double TEND_REACH = 2.0;
    /** STAND-IN [OPEN]: Cut loose reach, blocks. */
    static final double CUT_REACH = 5.0;
    /** How far the feet may drift (squared, horizontal) before standing still is broken. */
    private static final double STILL = 0.3 * 0.3;

    /** Tend's drain and heal are Field dressing's: the same bandage, on someone else. */
    static final Casts.Hold TEND = new Casts.Hold(FieldDressing.DRAIN, Aid::tendTick, Aid::tendStop);

    private record Tending(UUID ally, Vec3 me, Vec3 them, float myHealth, float theirHealth) {
    }

    private static final Map<UUID, Tending> TENDING = new HashMap<>();

    private Aid() {
    }

    /** The nearest other player within {@code range} whose direction is within {@code minDot} of your look, or null. */
    static ServerPlayer ally(ServerPlayer p, double range, double minDot) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        ServerPlayer best = null;
        double bestD = range * range;
        for (ServerPlayer o : p.serverLevel().players()) {
            if (o == p || !o.isAlive() || o.isSpectator())
                continue;
            Vec3 to = o.getBoundingBox().getCenter().subtract(eye);
            double d = to.lengthSqr();
            if (d < bestD && to.normalize().dot(look) > minDot) {
                bestD = d;
                best = o;
            }
        }
        return best;
    }

    // ---- Tend

    static boolean tend(ServerPlayer p) {
        ServerPlayer a = ally(p, TEND_REACH, -1);
        if (a == null || a.getHealth() >= a.getMaxHealth() || Stamina.get(p) < FieldDressing.DRAIN)
            return false;
        TENDING.put(p.getUUID(), new Tending(a.getUUID(), p.position(), a.position(), p.getHealth(), a.getHealth()));
        CastFx.sound(p.serverLevel(), p.position().add(0, 1, 0), SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1f, 1.2f);
        return true;
    }

    private static boolean tendTick(ServerPlayer p) {
        Tending t = TENDING.get(p.getUUID());
        ServerPlayer a = t == null ? null : p.server.getPlayerList().getPlayer(t.ally());
        // health below what the last tick left means something hurt one of you since
        if (a == null || !a.isAlive() || !p.isAlive() || a.level() != p.level() || a.getHealth() >= a.getMaxHealth()
                || p.getHealth() < t.myHealth() || a.getHealth() < t.theirHealth()
                || p.distanceTo(a) > TEND_REACH + 0.5
                || p.position().subtract(t.me()).horizontalDistanceSqr() > STILL
                || a.position().subtract(t.them()).horizontalDistanceSqr() > STILL) {
            tendStop(p);
            return false;
        }
        a.heal(FieldDressing.HEAL_PER_TICK);
        TENDING.put(p.getUUID(), new Tending(t.ally(), t.me(), t.them(), p.getHealth(), a.getHealth()));
        long now = p.level().getGameTime();
        if (now % 10 == 0) {
            Vec3 h = a.position().add(0, 1, 0);
            p.serverLevel().sendParticles(ParticleTypes.HEART, h.x, h.y, h.z, 1, 0.4, 0.5, 0.4, 0);
        }
        if (now % 20 == 0)
            SkillEarning.aided(p, a);
        return true;
    }

    private static void tendStop(ServerPlayer p) {
        TENDING.remove(p.getUUID());
    }

    // ---- Cut loose

    /** Frees an ally of every rope others put on them and any grab, staggering the holder by mass (Break free's own helper). */
    static boolean cutLoose(ServerPlayer p) {
        ServerPlayer a = ally(p, CUT_REACH, 0.5);
        if (a == null || !ShakeOff.freeOf(a, p))
            return false;
        Vec3 at = a.position().add(0, 1, 0);
        p.serverLevel().sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 12, 0.4, 0.5, 0.4, 0.1);
        CastFx.sound(p.serverLevel(), at, SoundEvents.SHEEP_SHEAR, 1f, 1f);
        SkillEarning.aided(p, a);
        return true;
    }
}
