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

import io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * Aid (Proficiency): the things you do for another player. Every one needs an ally (another {@link ServerPlayer}) in
 * reach and fails at no cost with none. Stamina is the only meter and it is yours: Tend drains it while the ally
 * mends, Hand off passes part of it on, Haul out and Cut loose pay their cast cost. Each delivery pays a small trickle
 * into Proficiency ({@link SkillEarning#aided}), decaying per ally.
 */
final class Aid {

    /** STAND-IN [OPEN]: Tend reach, blocks. */
    static final double TEND_REACH = 2.0;
    /** STAND-IN [OPEN]: Haul out reach, blocks. */
    static final double HAUL_REACH = 12.0;
    /** STAND-IN [OPEN]: Hand off reach, blocks. */
    static final double HAND_REACH = 3.0;
    /** STAND-IN [OPEN]: Cut loose reach, blocks. */
    static final double CUT_REACH = 5.0;
    /** STAND-IN [OPEN]: stamina a Hand off spends. */
    static final float HAND_COST = 30f;
    /** STAND-IN [OPEN]: the share of a handed-off bar that arrives; the rest is lost in the handling. */
    static final float HAND_KEPT = 0.6f;
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

    // ---- Haul out

    /** A friendly Reel In: the same line and haul, the ally arrives on their feet. Over 2.5 times your mass will not come. */
    static boolean haulOut(ServerPlayer p) {
        ServerPlayer a = ally(p, HAUL_REACH, 0.9);
        if (a == null || BodyMass.of(a) > BodyMass.of(p) * 2.5f)
            return false;
        ReelIn.start(p, a, true);
        return true;
    }

    // ---- Hand off

    /** Passes {@link #HAND_COST} of your stamina; {@link #HAND_KEPT} of it reaches the ally. */
    static boolean handOff(ServerPlayer p) {
        ServerPlayer a = ally(p, HAND_REACH, 0.5);
        if (a == null || Stamina.get(a) >= Stamina.MAX)
            return false;
        Stamina.gain(a, HAND_COST * HAND_KEPT);
        Vec3 from = p.position().add(0, 1, 0), to = a.position().add(0, 1, 0);
        CastFx.line(p.serverLevel(), ParticleTypes.HAPPY_VILLAGER, from, to, 0.3);
        CastFx.sound(p.serverLevel(), to, SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1f, 1.4f);
        SkillEarning.aided(p, a);
        return true;
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
