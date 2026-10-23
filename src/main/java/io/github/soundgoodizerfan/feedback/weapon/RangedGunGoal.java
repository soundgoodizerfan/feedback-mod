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

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.Haste;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/**
 * A skeleton using the gun it holds ({@code docs/spec_armed_skeletons.md} B). It shoots only with line of
 * sight inside the gun's effective range, which is worked out from the muzzle speed and the drop
 * ({@link #range}), aims high by the drop at the target's distance, and fires through
 * {@link GunShots#triggerMob}: the same swept shot, bake and cycle a player's gun has. Spread is the gun's
 * own times a mob's stance ({@link FTuning#SKELETON_GUN_STANCE}) plus a per-shot aim error
 * ({@link FTuning#SKELETON_GUN_AIM_ERROR_DEG}), never zero. When the gun is empty it
 * reloads from its spares a round a beat; with nothing in the gun and nothing spare the goal stops and
 * the vanilla goals take over (no reload loop). It sits ahead of the melee goal vanilla adds for a skeleton
 * that holds no bow.
 */
public final class RangedGunGoal extends Goal {

    private final AbstractSkeleton mob;
    /** The game tick the reload beat in progress is done, or -1. */
    private long reloadDue = -1;
    /** The game tick the current wind-up began, or -1. */
    private long windupStart = -1;
    /** The game tick the next shot may begin its wind-up. */
    private long nextShot;
    private GunSpec rangedFor;
    private float range;

    public RangedGunGoal(AbstractSkeleton mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private ItemStack gun() {
        return mob.getMainHandItem();
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        ItemStack gun = gun();
        return target != null && target.isAlive() && GunSpec.of(gun).isPresent() && !GunHazard.isSpent(gun)
                && (GunAmmo.count(gun) > 0 || ArmedSkeletons.spares(mob) > 0);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        mob.setAggressive(true);
    }

    @Override
    public void stop() {
        mob.setAggressive(false);
        mob.getNavigation().stop();
        reloadDue = -1;
        windupStart = -1;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * How far a flat-fired round of these figures flies before it has dropped {@link FTuning#SKELETON_GUN_DROP_BLOCKS},
     * stepped as {@code GunShots} steps a shot (drag, then gravity), or to the end of its life.
     */
    public static float range(GunFigures f) {
        return trace(f, Float.MAX_VALUE, FTuning.SKELETON_GUN_DROP_BLOCKS)[0];
    }

    /** {x, drop}: how far the round has flown when it first drops {@code maxDrop}, or reaches {@code maxX}, or ends its life. */
    private static float[] trace(GunFigures f, float maxX, float maxDrop) {
        double vx = f.muzzleSpeed(), vy = 0, x = 0, y = 0;
        for (int t = 0; t < FTuning.GUN_MAX_LIFE_TICKS && x < maxX && -y < maxDrop; t++) {
            x += vx;
            y += vy;
            vx *= 1 - FTuning.GUN_DRAG_PER_TICK;
            vy = vy * (1 - FTuning.GUN_DRAG_PER_TICK) - FTuning.GUN_GRAVITY_PER_TICK;
        }
        return new float[]{(float) x, (float) -y};
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null || !(mob.level() instanceof ServerLevel level))
            return;
        ItemStack gun = gun();
        GunSpec design = GunSpec.of(gun).orElse(null);
        if (design == null)
            return;
        if (!design.equals(rangedFor)) {
            rangedFor = design;
            range = design.bake(MaterialTable.get().materials()).map(RangedGunGoal::range).orElse(0f);
        }
        long now = level.getGameTime();
        double flat = Math.sqrt(mob.distanceToSqr(target.getX(), mob.getY(), target.getZ()));
        boolean sees = mob.getSensing().hasLineOfSight(target);
        mob.getLookControl().setLookAt(target, 30f, 30f);
        if (sees && flat <= range)
            mob.getNavigation().stop();
        else
            mob.getNavigation().moveTo(target, 1.0);

        if (reloadDue >= 0) {
            if (now >= reloadDue)
                reload(level, gun, design, now);
            return;
        }
        if (GunAmmo.count(gun) == 0) {
            if (ArmedSkeletons.spares(mob) > 0)
                reloadDue = now + beatTicks(gun, design);
            return;
        }
        if (!sees || flat > range || now < nextShot) {
            windupStart = -1;
            return;
        }
        // Aim high by what the round will have dropped by the target's distance.
        var aim = target.getBoundingBox().getCenter();
        double dy = aim.y - mob.getEyeY();
        float drop = design.bake(MaterialTable.get().materials()).map(f -> trace(f, (float) flat, Float.MAX_VALUE)[1]).orElse(0f);
        float yaw = (float) (Mth.atan2(aim.z - mob.getZ(), aim.x - mob.getX()) * 180 / Math.PI) - 90f;
        float pitch = (float) -(Mth.atan2(dy + drop, flat) * 180 / Math.PI);
        // Must be facing the shot before the wind-up counts; the body turns at the look control's pace.
        if (Math.abs(Mth.wrapDegrees(yaw - mob.getYHeadRot())) > FTuning.SKELETON_GUN_FACING_DEG) {
            windupStart = -1;
            return;
        }
        if (windupStart < 0) {
            windupStart = now;
            level.playSound(null, mob.getX(), mob.getEyeY(), mob.getZ(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.HOSTILE, 1f, 0.8f);
        }
        if ((now - windupStart) % 4 == 0) {
            var muzzle = mob.getEyePosition().add(mob.getLookAngle().scale(0.8));
            level.sendParticles(ParticleTypes.CRIT, muzzle.x, muzzle.y - 0.2, muzzle.z, 2, 0.05, 0.05, 0.05, 0.02);
        }
        if (now - windupStart < FTuning.SKELETON_GUN_WINDUP_TICKS)
            return;
        windupStart = -1;
        float err = FTuning.SKELETON_GUN_AIM_ERROR_DEG * Mth.sqrt(level.random.nextFloat());
        float theta = level.random.nextFloat() * Mth.TWO_PI;
        yaw += err * Mth.cos(theta);
        pitch = Mth.clamp(pitch + err * Mth.sin(theta), -90f, 90f);
        if (GunShots.triggerMob(level, mob, gun, now, yaw, pitch)) {
            mob.swing(InteractionHand.MAIN_HAND);
            nextShot = now + FTuning.SKELETON_GUN_MIN_INTERVAL_TICKS;
        }
    }

    /** One beat: a round a beat into an internal feed, a swap's time for a box. */
    private int beatTicks(ItemStack gun, GunSpec design) {
        long ms = design.internalCapacity().isPresent() ? FTuning.GUN_RELOAD_ROUND_MS : GunAmmo.reloadMs(gun);
        return (int) Math.max(1, Haste.time(ms, mob) / 50);
    }

    private void reload(ServerLevel level, ItemStack gun, GunSpec design, long now) {
        Cartridge spare = ArmedSkeletons.takeSpare(mob);
        reloadDue = -1;
        if (spare == null)
            return;
        gun.set(FWeapons.MAGAZINE.get(), GunAmmo.magazine(gun).push(spare));
        GunAmmo.feed(gun);
        level.playSound(null, mob.getX(), mob.getEyeY(), mob.getZ(), SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.HOSTILE, 0.5f, 1.6f);
        // Keep loading to the gun's capacity while rounds last; it shoots again once it is full or out.
        if (GunAmmo.count(gun) < GunAmmo.capacity(gun) && ArmedSkeletons.spares(mob) > 0)
            reloadDue = now + beatTicks(gun, design);
    }
}
