// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * What the shooter is doing to the spread (spec 7.1: TacZ's five stances, kept as the idea and
 * written as multipliers). Server-side, from the shooter's own state: the client says only that
 * it is aiming, and aiming is honoured only for a shooter standing on the ground and not sprinting.
 */
public final class GunStance {

    private GunStance() {
    }

    /** Pure, so a test and the server agree. {@code speed} is horizontal blocks per tick. */
    public static float multiplier(boolean aimed, boolean sneaking, boolean sprinting, boolean airborne, double speed) {
        float m = 1f;
        boolean aim = aimed && !sprinting && !airborne;
        if (aim)
            m *= GunTuning.GUN_ADS_SPREAD;
        if (sneaking)
            m *= GunTuning.GUN_STANCE_CROUCH;
        if (speed > GunTuning.GUN_MOVING_SPEED)
            m *= GunTuning.GUN_STANCE_MOVING;
        if (sprinting)
            m *= GunTuning.GUN_STANCE_SPRINT;
        if (airborne)
            m *= GunTuning.GUN_STANCE_AIRBORNE;
        return m;
    }

    public static float of(LivingEntity shooter, boolean aimed) {
        // A server-side player's deltaMovement is not their motion; the position step is.
        double speed = Math.hypot(shooter.getX() - shooter.xo, shooter.getZ() - shooter.zo);
        float m = multiplier(aimed, shooter.isCrouching(), shooter.isSprinting(), !shooter.onGround(), speed);
        // A mob has no training: its hands shake the same standing still (never a perfect aim).
        if (!(shooter instanceof Player))
            return m * GunTuning.SKELETON_GUN_STANCE;
        // Steady Walk: a share of the extra that walking adds is taken back.
        float steady = io.github.soundgoodizerfan.feedback.combat.skill.Skills.modifier(shooter, io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Figure.STEADY_WALK);
        if (steady > 0f && speed > GunTuning.GUN_MOVING_SPEED)
            m *= (1f + (GunTuning.GUN_STANCE_MOVING - 1f) * (1f - Math.min(steady, 1f))) / GunTuning.GUN_STANCE_MOVING;
        m *= io.github.soundgoodizerfan.feedback.potion.SteadyHand.factor(shooter);
        if (shooter instanceof net.minecraft.server.level.ServerPlayer sp)
            m *= io.github.soundgoodizerfan.feedback.combat.cast.QuickDraw.consume(sp);
        return m;
    }
}
