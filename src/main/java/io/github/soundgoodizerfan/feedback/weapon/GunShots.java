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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The server's side of a shot ({@code feedback_guns_spec.md} sections 7 and 7.1).
 *
 * <p><b>No entity per bullet</b> -- the spec's recommendation, and what TacZ does the other way
 * (an {@code EntityKineticBullet} per round, swept per tick, proven at its scale). A shot is a
 * position and a velocity held here and stepped once a tick as a swept segment: a block clip and
 * an entity box sweep over the whole tick's travel, so it cannot tunnel, and nothing is
 * synchronised because nothing is an entity. The cost is that a client sees no bullet but the
 * tracer it is told to draw; there is no tracer in this step.
 *
 * <p>A shot is stepped at most once per tick, so a bullet at 20 blocks a tick is one segment.
 * Hit energy falls with the square of the speed it has left, so drag and gravity cost a hit
 * damage honestly (spec 3.3).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class GunShots {

    private static final class Shot {
        final Player shooter;
        Vec3 pos;
        Vec3 velocity;
        final float muzzleSpeed;
        final float muzzleEnergy;
        int age;

        Shot(Player shooter, Vec3 pos, Vec3 velocity, GunFigures figures) {
            this.shooter = shooter;
            this.pos = pos;
            this.velocity = velocity;
            this.muzzleSpeed = figures.muzzleSpeed();
            this.muzzleEnergy = figures.muzzleEnergy();
        }

        /** Work this projectile would deliver now: the muzzle's, scaled by speed squared. */
        float energy() {
            float v = (float) velocity.length();
            return muzzleEnergy * (v * v) / (muzzleSpeed * muzzleSpeed);
        }
    }

    private static final Map<ServerLevel, List<Shot>> LIVE = new HashMap<>();
    /** Earliest wall-clock ms each shooter may fire again. */
    private static final Map<UUID, Long> NEXT_OK = new HashMap<>();

    private GunShots() {
    }

    /**
     * Whether {@code shooter} may fire now, and if so book the next slot. The cycle is wall-clock
     * (spec 7.1); the tolerance is for network jitter until timestamp sync replaces it.
     */
    public static boolean accept(Player shooter, GunFigures figures, long nowMs) {
        // Early by at most a quarter of a cycle, or the jitter allowance if that is less: a
        // client hacked to fire a little early gains only that, and an honest one never trips it.
        long tolerance = Math.min(FTuning.GUN_FIRE_TOLERANCE_MS, figures.cycleMs() / 4);
        long earliest = NEXT_OK.getOrDefault(shooter.getUUID(), 0L);
        if (nowMs + tolerance < earliest)
            return false;
        NEXT_OK.put(shooter.getUUID(), Math.max(earliest, nowMs) + figures.cycleMs());
        return true;
    }

    /**
     * A trigger pull on the server: the round the gun's own record says is next, in the figures of
     * this gun, the cycle validated, the round spent, overpressure booked, and the shot launched.
     * The client names only the aim; it never says what is chambered.
     *
     * @return whether a shot left the muzzle.
     */
    public static boolean trigger(ServerLevel level, Player shooter, net.minecraft.world.item.ItemStack gun,
                                  float yaw, float pitch, long nowMs) {
        GunDesign design = gun.get(io.github.soundgoodizerfan.feedback.registry.FWeapons.GUN_DESIGN.get());
        if (design == null || GunHazard.isSpent(gun))
            return false;
        var next = GunAmmo.next(gun);
        if (next.isEmpty())
            return false;
        var materials = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials();
        var figures = design.bake(materials, next.get());
        if (figures.isEmpty() || !accept(shooter, figures.get(), nowMs))
            return false;
        GunAmmo.spend(gun);
        float caseRating = next.get().caseRating(materials).map(p -> p.value()).orElse(Float.MAX_VALUE);
        float noise = 1 + (level.random.nextFloat() * 2 - 1) * FTuning.GUN_HAZARD_NOISE;
        GunHazard.apply(level, shooter, gun, GunHazard.assess(figures.get().peakPressure().value(),
                figures.get().chamberRating().value(), caseRating, noise));
        fire(level, shooter, figures.get(), yaw, pitch);
        return true;
    }

    /**
     * Swap the held gun's magazine for the fullest one in the inventory that is its bore. The old
     * rounds come back as a magazine in the slot the new one left. The swap books the reload's
     * wall-clock time, during which the gun does not fire.
     */
    public static void reload(Player player, long nowMs) {
        net.minecraft.world.item.ItemStack gun = player.getMainHandItem();
        GunDesign design = gun.getItem() instanceof GunItem && !player.isSpectator()
                ? gun.get(io.github.soundgoodizerfan.feedback.registry.FWeapons.GUN_DESIGN.get()) : null;
        if (design == null || nowMs < NEXT_OK.getOrDefault(player.getUUID(), 0L))
            return;
        var inventory = player.getInventory();
        int best = -1, most = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            var s = inventory.getItem(i);
            if (!s.is(io.github.soundgoodizerfan.feedback.registry.FWeapons.MAGAZINE_ITEM.get()))
                continue;
            Magazine m = MagazineItem.rounds(s);
            if (m.boreMm() == design.boreMm() && m.rounds().size() > most) {
                best = i;
                most = m.rounds().size();
            }
        }
        if (best < 0)
            return;
        var swapped = new net.minecraft.world.item.ItemStack(io.github.soundgoodizerfan.feedback.registry.FWeapons.MAGAZINE_ITEM.get());
        swapped.set(io.github.soundgoodizerfan.feedback.registry.FWeapons.MAGAZINE.get(),
                gun.getOrDefault(io.github.soundgoodizerfan.feedback.registry.FWeapons.MAGAZINE.get(), Magazine.EMPTY));
        gun.set(io.github.soundgoodizerfan.feedback.registry.FWeapons.MAGAZINE.get(), MagazineItem.rounds(inventory.getItem(best)));
        GunAmmo.feed(gun); // a magazine put in under an empty chamber chambers its top round
        inventory.setItem(best, swapped);
        NEXT_OK.put(player.getUUID(), nowMs + FTuning.GUN_RELOAD_MS);
        player.level().playSound(player, player.getX(), player.getEyeY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    /**
     * Launch a shot along {@code yaw}/{@code pitch} (degrees, the shooter's own view), widened by
     * the gun's spread. The aim comes from the client so the shot leaves where the player was
     * looking when they pressed, not where the server last heard they were.
     */
    public static void fire(ServerLevel level, Player shooter, GunFigures f, float yaw, float pitch) {
        float r = f.spreadDeg() * Mth.sqrt(level.random.nextFloat());
        float theta = level.random.nextFloat() * Mth.TWO_PI;
        float aimPitch = Mth.clamp(pitch + r * Mth.sin(theta), -90f, 90f);
        float aimYaw = yaw + r * Mth.cos(theta);
        Vec3 dir = Vec3.directionFromRotation(aimPitch, aimYaw);
        Vec3 muzzle = shooter.getEyePosition().add(dir.scale(0.4));
        LIVE.computeIfAbsent(level, l -> new ArrayList<>()).add(new Shot(shooter, muzzle, dir.scale(f.muzzleSpeed()), f));

        level.playSound(shooter, muzzle.x, muzzle.y, muzzle.z, GunSound.event(), SoundSource.PLAYERS,
                GunSound.volume(f), GunSound.pitch(f, level.random));
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.02, 0.02, 0.02, 0.01);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level))
            return;
        List<Shot> shots = LIVE.get(level);
        if (shots == null)
            return;
        for (Iterator<Shot> it = shots.iterator(); it.hasNext(); ) {
            if (!step(level, it.next()))
                it.remove();
        }
        if (shots.isEmpty())
            LIVE.remove(level);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LIVE.clear();
        NEXT_OK.clear();
    }

    /** One tick's travel. @return whether the shot is still flying. */
    private static boolean step(ServerLevel level, Shot s) {
        if (s.shooter.isRemoved() || ++s.age > FTuning.GUN_MAX_LIFE_TICKS)
            return false;
        Vec3 from = s.pos, to = from.add(s.velocity);

        BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, s.shooter, from, end,
                new AABB(from, end).inflate(1.0),
                e -> e.isPickable() && !e.isSpectator() && e != s.shooter);
        if (hit != null) {
            Entity target = hit.getEntity();
            float damage = s.energy() * FTuning.GUN_DAMAGE_PER_WORK;
            // Rapid fire must not be eaten by invulnerability frames; a gun is not a sword.
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(s.shooter), damage);
            return false;
        }
        if (block.getType() != HitResult.Type.MISS) {
            BlockPos at = block.getBlockPos();
            Vec3 p = block.getLocation();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(at)),
                    p.x, p.y, p.z, 6, 0.05, 0.05, 0.05, 0.1);
            return false;
        }

        // Still flying: drag in the medium it ends the tick in, then gravity.
        boolean inFluid = !level.getFluidState(BlockPos.containing(to)).isEmpty();
        float drag = inFluid ? FTuning.GUN_FLUID_DRAG_PER_TICK : FTuning.GUN_DRAG_PER_TICK;
        s.velocity = s.velocity.scale(1 - drag).add(0, -FTuning.GUN_GRAVITY_PER_TICK, 0);
        s.pos = to;
        return true;
    }
}
