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
package io.github.soundgoodizerfan.feedback.combat.grab;

import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.skill.Rearms;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Throw (skill {@code throw}, spec 4.4): a body that has been pulled in and is staggered, on a grab or a
 * grapple that reached the thrower, is released past them along the look, fast or slow by
 * thrower mass over its own ({@link BodyMass}) so a goat flies and a golem barely stumbles. No damage is
 * dealt here: the thrown body meets whatever stands there (a wall, a drop, a fence) and that hazard code
 * does the rest. The price is position: the thrower is left in reach, recovering as after a missed parry.
 * Spent until the thrower staggers something, so it is a follow-up to a break, not a free move.
 */
public final class Throwing {

    private Throwing() {
    }

    /**
     * Throws {@code target} if {@code thrower} has the skill, it is ready, and the target is staggered.
     * @return whether the throw happened
     */
    public static boolean attempt(LivingEntity thrower, LivingEntity target) {
        if (!(thrower instanceof ServerPlayer p) || !Skills.unlocked(p, "throw") || !Rearms.ready(p, "throw")
                || CombatState.of(target).phase() != CombatPhase.STAGGER)
            return false;
        Vec3 dir = p.getLookAngle().multiply(1, 0, 1);
        dir = dir.lengthSqr() < 1e-6 ? Vec3.directionFromRotation(0, p.getYRot()) : dir.normalize();
        double speed = Mth.clamp(GrabTuning.THROW_SPEED * BodyMass.of(p) / BodyMass.of(target),
                GrabTuning.THROW_MIN, GrabTuning.THROW_MAX);
        target.setDeltaMovement(dir.x * speed, GrabTuning.THROW_UP, dir.z * speed);
        target.hurtMarked = true;
        CombatState s = CombatState.of(p);
        CombatState.set(p, new CombatState(CombatPhase.PARRY_RECOVERY, GrabTuning.THROW_RECOVERY, 0, s.iFrames()));
        Rearms.spend(p, "throw");
        CombatCues.everyone(p, FSounds.FEEL_HEAVY.get(), FeelTuning.HEAVY_VOLUME, 1.1f);
        FeelTuning.HEAVY.fire(p, target.getBoundingBox().getCenter());
        return true;
    }
}
