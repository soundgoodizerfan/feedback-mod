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
            m *= FTuning.GUN_ADS_SPREAD;
        if (sneaking)
            m *= FTuning.GUN_STANCE_CROUCH;
        if (speed > FTuning.GUN_MOVING_SPEED)
            m *= FTuning.GUN_STANCE_MOVING;
        if (sprinting)
            m *= FTuning.GUN_STANCE_SPRINT;
        if (airborne)
            m *= FTuning.GUN_STANCE_AIRBORNE;
        return m;
    }

    public static float of(LivingEntity shooter, boolean aimed) {
        // A server-side player's deltaMovement is not their motion; the position step is.
        double speed = Math.hypot(shooter.getX() - shooter.xo, shooter.getZ() - shooter.zo);
        float m = multiplier(aimed, shooter.isCrouching(), shooter.isSprinting(), !shooter.onGround(), speed);
        // A mob has no training: its hands shake the same standing still (never a perfect aim).
        return shooter instanceof Player ? m : m * FTuning.SKELETON_GUN_STANCE;
    }
}
