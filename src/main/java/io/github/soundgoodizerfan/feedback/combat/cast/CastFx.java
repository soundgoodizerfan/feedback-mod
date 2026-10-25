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

import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Staging for the cast techniques: coloured dust, rings that expand or close in, lines, spokes and ground debris, all
 * sent from the server as ordinary particles so no client code is needed. Pure show: nothing here changes a figure.
 * Shapes and counts are {@code [OPEN]} stand-ins until played.
 */
final class CastFx {

    static final int STEEL = 0xB8C4D6, EMBER = 0xFF6A1A, GOLD = 0xFFD24A, SHADOW = 0x4B1F7A, CRIMSON = 0xC01028, ICE = 0x9FE8FF;

    private CastFx() {
    }

    private static Vector3f rgb(int c) {
        return new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f);
    }

    static DustParticleOptions dust(int c, float size) {
        return new DustParticleOptions(rgb(c), size);
    }

    static DustColorTransitionOptions fade(int from, int to, float size) {
        return new DustColorTransitionOptions(rgb(from), rgb(to), size);
    }

    static void sound(ServerLevel l, Vec3 at, SoundEvent s, float volume, float pitch) {
        l.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, volume, pitch);
    }

    /** A ring of {@code n} points; with {@code outward} above zero each particle flies away from the centre (an expanding wave). */
    static void ring(ServerLevel l, ParticleOptions t, Vec3 c, double r, int n, double outward) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n, cos = Math.cos(a), sin = Math.sin(a);
            if (outward > 0)
                l.sendParticles(t, c.x + cos * r, c.y, c.z + sin * r, 0, cos, 0.04, sin, outward);
            else
                l.sendParticles(t, c.x + cos * r, c.y, c.z + sin * r, 1, 0, 0, 0, 0);
        }
    }

    static void line(ServerLevel l, ParticleOptions t, Vec3 a, Vec3 b, double step) {
        Vec3 d = b.subtract(a);
        int n = (int) Math.max(1, d.length() / step);
        for (int i = 0; i <= n; i++) {
            Vec3 q = a.add(d.scale(i / (double) n));
            l.sendParticles(t, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }

    /** {@code n} straight cracks running out of {@code c} along the ground. */
    static void spokes(ServerLevel l, ParticleOptions t, Vec3 c, double from, double to, int n, double step) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n + 0.3;
            Vec3 d = new Vec3(Math.cos(a), 0, Math.sin(a));
            line(l, t, c.add(d.scale(from)), c.add(d.scale(to)), step);
        }
    }

    /** Chips of whatever block is underfoot, thrown up. */
    static void debris(ServerLevel l, Vec3 at, int n, double spread) {
        BlockState s = l.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
        if (!s.isAir())
            l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s), at.x, at.y + 0.1, at.z, n, spread, 0.1, spread, 0.25);
    }

    /**
     * The mace's smash, the base of every slam and stomp: vanilla's dust pillar of the block under {@code at} (half
     * {@code power} thrown in a 3.5-block ring, as {@code MaceItem}'s 750) and its ground-smash sound, heavy as past the
     * mace's 5-block fall.
     */
    static void smash(ServerLevel l, Vec3 at, int power, boolean heavy) {
        l.levelEvent(LevelEvent.PARTICLES_SMASH_ATTACK, BlockPos.containing(at.x, at.y - 0.5, at.z), power);
        sound(l, at, heavy ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND, 1f, 1f);
    }

    /** Rings that grow from the centre to {@code max} over {@code steps} ticks: a shockwave you can watch travel. */
    static void shockwave(ServerPlayer p, Vec3 c, double max, int steps, ParticleOptions t, double outward) {
        ServerLevel l = p.serverLevel();
        for (int i = 1; i <= steps; i++) {
            double r = max * i / steps;
            Casts.later(p, i, () -> ring(l, t, c, r, (int) (10 + r * 6), outward));
        }
    }

    /** Rings that close in on the centre over {@code steps} ticks: a charge gathering. */
    static void converge(ServerPlayer p, Vec3 c, double from, int steps, ParticleOptions t) {
        ServerLevel l = p.serverLevel();
        for (int i = 0; i < steps; i++) {
            double r = from * (1 - i / (double) steps);
            Casts.later(p, i, () -> ring(l, t, c, r, 12, 0));
        }
    }

    /** A burst of {@code n} particles thrown out of {@code c} in every direction at {@code speed}. */
    static void spray(ServerLevel l, ParticleOptions t, Vec3 c, int n, double speed) {
        for (int i = 0; i < n; i++) {
            Vec3 d = new Vec3(l.random.nextGaussian(), l.random.nextGaussian(), l.random.nextGaussian());
            d = d.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : d.normalize();
            l.sendParticles(t, c.x, c.y, c.z, 0, d.x, d.y, d.z, speed);
        }
    }
}
