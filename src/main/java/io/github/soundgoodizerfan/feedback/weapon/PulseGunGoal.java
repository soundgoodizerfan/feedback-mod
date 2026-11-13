// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.EnumSet;

import io.github.soundgoodizerfan.feedback.modular.item.ModularElectricGun;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.AbstractSkeleton;

/**
 * A skeleton using the pulse gun it holds: it shoots with line of sight inside the pulse's cutoff range, through
 * {@link GunShots#triggerMob}, and closes in until it has both. A pulse does not drop, so there is no aim correction; the aim error and the
 * wind-up are the powder-gun goal's ({@link GunTuning#SKELETON_GUN_AIM_ERROR_DEG}, {@link GunTuning#SKELETON_GUN_WINDUP_TICKS}).
 */
public final class PulseGunGoal extends Goal {

    private final AbstractSkeleton mob;
    private long windupStart = -1, nextShot;

    public PulseGunGoal(AbstractSkeleton mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.getMainHandItem().getItem() instanceof ModularElectricGun;
    }

    @Override
    public void start() {
        mob.setAggressive(true);
    }

    @Override
    public void stop() {
        mob.setAggressive(false);
        mob.getNavigation().stop();
        windupStart = -1;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null || !(mob.level() instanceof ServerLevel level))
            return;
        var spec = ModularElectricGun.specOf(mob.getMainHandItem());
        if (spec == null)
            return;
        long now = level.getGameTime();
        double dist = Math.sqrt(mob.distanceToSqr(target));
        boolean sees = mob.getSensing().hasLineOfSight(target);
        float range = Math.min(spec.cutoff(0f), 24f);
        mob.getLookControl().setLookAt(target, 30f, 30f);
        if (sees && dist <= range)
            mob.getNavigation().stop();
        else
            mob.getNavigation().moveTo(target, 1.0);
        if (!sees || dist > range || now < nextShot) {
            windupStart = -1;
            return;
        }
        var aim = target.getBoundingBox().getCenter();
        float yaw = (float) (Mth.atan2(aim.z - mob.getZ(), aim.x - mob.getX()) * 180 / Math.PI) - 90f;
        float pitch = (float) -(Mth.atan2(aim.y - mob.getEyeY(), Math.sqrt(mob.distanceToSqr(aim.x, mob.getY(), aim.z))) * 180 / Math.PI);
        if (Math.abs(Mth.wrapDegrees(yaw - mob.getYHeadRot())) > GunTuning.SKELETON_GUN_FACING_DEG) {
            windupStart = -1;
            return;
        }
        if (windupStart < 0)
            windupStart = now;
        if (now - windupStart < GunTuning.SKELETON_GUN_WINDUP_TICKS)
            return;
        windupStart = -1;
        float err = GunTuning.SKELETON_GUN_AIM_ERROR_DEG * Mth.sqrt(level.random.nextFloat());
        float theta = level.random.nextFloat() * Mth.TWO_PI;
        if (GunShots.triggerMob(level, mob, mob.getMainHandItem(), now, yaw + err * Mth.cos(theta), Mth.clamp(pitch + err * Mth.sin(theta), -90f, 90f))) {
            mob.swing(InteractionHand.MAIN_HAND);
            nextShot = now + GunTuning.SKELETON_GUN_MIN_INTERVAL_TICKS;
        }
    }
}
