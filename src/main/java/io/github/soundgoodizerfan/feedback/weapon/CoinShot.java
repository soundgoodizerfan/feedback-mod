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
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The coin shot (skill {@code coin_flip}, spec 14.6a): the skill is the move, the coin is the stock.
 * A flip spends one coin (any item whose material form is {@code coin}, never an item id) and puts a
 * virtual one in the air on a computed arc; there is no coin entity. A bullet of the flipper's that is swept
 * past the arc's point in the coin's few ticks of flight strikes it, and what the coin does is the metal's:
 * <ul>
 * <li>A coin softer than the round is punched through and flattened; the round goes on, a little slower.</li>
 * <li>A harder coin rebounds it. It keeps more of its momentum the lighter the coin (density over the round's
 * mass), and the rebound is aimed at the nearest thing in range with a clear line: Second Mark throws two coins
 * and sends the rebound into the other coin first, Round the Corner triples the range.</li>
 * </ul>
 * A coin not struck falls back into the flipper's hands. Second Mark lets the key be pressed up to twice, the second
 * press while the first coin is still in the air. Flipping is spent until the flipper lands a blow
 * (rearm {@code strike}), so the shot is a move inside a fight and not a gun mode.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class CoinShot {

    private record Coin(UUID owner, Vec3 origin, long born, float hardness, float massMu, ItemStack stack) {
        Vec3 at(long now) {
            long t = now - born;
            return origin.add(0, GunFuTuning.COIN_UP * t - 0.5 * GunFuTuning.COIN_GRAVITY * t * t, 0);
        }
    }

    /** What a struck coin did to the round: where, which way it goes (null: unchanged), and the share of speed it keeps. */
    public record Result(Vec3 point, Vec3 direction, float keep) {
    }

    private static final Map<UUID, List<Coin>> COINS = new HashMap<>();

    private CoinShot() {
    }

    /** Whether a flip now would be the second press of a Second Mark pair: one coin up, the skill owned. */
    public static boolean followUp(ServerPlayer p) {
        List<Coin> up = COINS.get(p.getUUID());
        return up != null && up.size() == 1 && Skills.unlocked(p, "second_mark");
    }

    /**
     * A Feedbacker punch (the press with the gauntlet worn and nothing in hand): any of the puncher's coins within reach
     * and in front is knocked back up from where it hangs, its arc and its few ticks of flight starting over.
     */
    public static void punch(ServerPlayer p) {
        List<Coin> up = COINS.get(p.getUUID());
        if (up == null)
            return;
        long now = p.level().getGameTime();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        for (int i = 0; i < up.size(); i++) {
            Coin c = up.get(i);
            Vec3 to = c.at(now).subtract(eye);
            if (to.length() > 3.5 || to.normalize().dot(look) < 0.3)
                continue;
            up.set(i, new Coin(c.owner(), c.at(now), now, c.hardness(), c.massMu(), c.stack()));
            ServerLevel level = p.serverLevel();
            Vec3 at = c.at(now);
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.1, 0.1, 0.1, 0.3);
            cue(level, at, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(), 1f, 1.8f);
        }
    }

    /**
     * A flip press: needs the skill and a coin to hand; the rearm is the caller's. One coin is up at a time, or two with
     * Second Mark (the second press, while the first is still in the air).
     */
    public static boolean flip(ServerPlayer p) {
        List<Coin> up = COINS.computeIfAbsent(p.getUUID(), k -> new ArrayList<>());
        boolean ok = !p.isSpectator() && Skills.unlocked(p, "coin_flip") && (up.isEmpty() || followUp(p));
        var materials = MaterialTable.get().materials();
        for (int i = 0; ok && i < p.getInventory().getContainerSize(); i++) {
            ItemStack stack = p.getInventory().getItem(i);
            var found = stack.isEmpty() ? java.util.Optional.<Material.Located>empty() : Material.find(materials, stack);
            if (found.isEmpty() || !found.get().form().equals("coin"))
                continue;
            Material m = found.get().material();
            ItemStack one = stack.copyWithCount(1);
            stack.shrink(1);
            Vec3 look = p.getLookAngle().multiply(1, 0, 1);
            Vec3 fwd = look.lengthSqr() < 1e-6 ? Vec3.ZERO : look.normalize();
            Vec3 right = fwd.cross(new Vec3(0, 1, 0));
            // The second coin goes up beside the first and a little higher, so one can be shot into the other.
            Vec3 origin = p.getEyePosition().add(fwd.scale(0.6)).add(0, -0.3 + 0.3 * up.size(), 0).add(right.scale(0.35 * up.size()));
            up.add(new Coin(p.getUUID(), origin, p.level().getGameTime(), m.hardness().value(),
                    m.density().muPerMb() * GunFuTuning.COIN_MB, one));
            CombatCues.everyone(p, FSounds.FEEL_LIGHT.get(), FeelTuning.LIGHT_VOLUME, up.size() == 1 ? 1.9f : 2f);
            return true;
        }
        if (up.isEmpty())
            COINS.remove(p.getUUID());
        return false;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (COINS.isEmpty())
            return;
        COINS.entrySet().removeIf(entry -> {
            ServerPlayer p = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (p == null)
                return true;
            ServerLevel level = p.serverLevel();
            long now = level.getGameTime();
            entry.getValue().removeIf(c -> {
                if (now - c.born() > GunFuTuning.COIN_TICKS) {
                    GunAmmo.give(p, c.stack()); // not struck: it falls back into the hand
                    cue(level, c.at(now), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.value(), 1f, 1.2f);
                    return true;
                }
                Vec3 at = c.at(now);
                level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 2, 0.02, 0.02, 0.02, 0);
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 1, 0, 0, 0, 0);
                return false;
            });
            return entry.getValue().isEmpty();
        });
    }

    /**
     * Whether the swept round {@code from} to {@code to} of {@code shooter} strikes one of their coins (the first along
     * the segment), and if so what the coin does. A struck coin is spent.
     */
    public static Result resolve(ServerLevel level, LivingEntity shooter, Vec3 from, Vec3 to, float roundMassMu, float roundHardness) {
        List<Coin> coins = COINS.get(shooter.getUUID());
        if (coins == null)
            return null;
        Vec3 seg = to.subtract(from);
        double len2 = seg.lengthSqr();
        Coin c = null;
        Vec3 near = null;
        double bestU = 2;
        for (Coin each : coins) {
            Vec3 at = each.at(level.getGameTime());
            double u = len2 < 1e-9 ? 0 : Mth.clamp(at.subtract(from).dot(seg) / len2, 0, 1);
            Vec3 point = from.add(seg.scale(u));
            double miss = point.distanceTo(at);
            if (miss <= GunFuTuning.COIN_RADIUS) {
                if (u < bestU) {
                    bestU = u;
                    c = each;
                    near = point;
                }
            } else if (miss < GunFuTuning.COIN_RADIUS * 3) // close, but not a hit
                cue(level, at, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_SNARE.value(), 1f, 0.7f);
        }
        if (c == null)
            return null;
        coins.remove(c);
        if (coins.isEmpty())
            COINS.remove(shooter.getUUID());
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, c.stack()), near.x, near.y, near.z, 6, 0.05, 0.05, 0.05, 0.1);
        level.sendParticles(ParticleTypes.CRIT, near.x, near.y, near.z, 6, 0.05, 0.05, 0.05, 0.3);
        level.playSound(null, near.x, near.y, near.z, FSounds.FEEL_HEAVY.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 2f);
        // Softer than the round: punched through and flattened, the round slowed.
        if (c.hardness() < roundHardness) {
            cue(level, near, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.6f); // low: punched through
            return new Result(near, null, GunFuTuning.COIN_PUNCH_KEEP);
        }
        float keep = Mth.clamp(roundMassMu / (roundMassMu + c.massMu() * GunFuTuning.COIN_MOMENTUM), GunFuTuning.COIN_KEEP_MIN, 1f);
        Vec3 dir = seg.normalize();
        Vec3 aimed = aim(level, shooter, coins, near, dir);
        // High double chime: the rebound found something to go to. A single dull ping: nothing in range.
        cue(level, near, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(), 1f, aimed != null ? 1.6f : 0.9f);
        if (aimed != null)
            cue(level, near, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(), 1f, 2f);
        if (aimed == null) {
            // Nothing to send it to: a hard coin still turns the round, a glancing ping off its face, never straight on.
            double yaw = Math.toRadians((20 + level.random.nextInt(25)) * (level.random.nextBoolean() ? 1 : -1));
            double cos = Math.cos(yaw), sin = Math.sin(yaw);
            aimed = new Vec3(dir.x * cos - dir.z * sin, dir.y + 0.15, dir.x * sin + dir.z * cos).normalize();
        }
        return new Result(near, aimed, keep);
    }

    /** A diagnostic cue: each way a coin can end has its own sound, so a shot that does nothing can be told from one that nearly did. */
    private static void cue(ServerLevel level, Vec3 at, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, net.minecraft.sounds.SoundSource.PLAYERS, volume, pitch);
    }

    private static boolean clear(ServerLevel level, Vec3 a, Vec3 b) {
        return level.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()))
                .getType() == HitResult.Type.MISS;
    }

    /**
     * The rebound's direction, or null if the coin finds nothing to send it to. Another coin of the flipper's still in the
     * air comes first (Second Mark: a coin shot into a coin), then the nearest living thing within range with a clear line
     * from the coin, whether or not the flipper can see it and however the round was heading. Round the Corner triples the range.
     */
    private static Vec3 aim(ServerLevel level, LivingEntity shooter, List<Coin> others, Vec3 point, Vec3 dir) {
        long now = level.getGameTime();
        for (Coin o : others) {
            Vec3 to = o.at(now).subtract(point);
            if (to.lengthSqr() > 1e-4 && clear(level, point, o.at(now)))
                return to.normalize();
        }
        double range = GunFuTuning.COIN_RANGE_BLOCKS * (Skills.unlocked(shooter, "round_the_corner") ? 3 : 1);
        List<LivingEntity> found = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(point, point).inflate(range),
                e -> e != shooter && e.isAlive() && !e.isSpectator() && !shooter.isAlliedTo(e)));
        found.sort(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(point)));
        for (LivingEntity e : found) {
            Vec3 centre = e.getBoundingBox().getCenter();
            Vec3 to = centre.subtract(point);
            if (to.lengthSqr() < 1e-6 || to.length() > range || !clear(level, point, centre))
                continue;
            return to.normalize();
        }
        return null;
    }
}
