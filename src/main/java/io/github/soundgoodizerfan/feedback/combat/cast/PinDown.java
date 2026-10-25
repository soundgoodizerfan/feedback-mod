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

import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;

/** Pin Down, always on once owned (a rider on firing, not a cast): rounds you fire that pass close to a living thing slow it hard, hit or not; Held Pin doubles the slow. */
public final class PinDown {

    private PinDown() {
    }

    /** A round of {@code shooter} flew {@code from} to {@code to}: anything it passed close to is pinned if Pin Down is up. Called by the shot's step. */
    public static void flyby(ServerLevel level, LivingEntity shooter, Vec3 from, Vec3 to) {
        if (!Skills.unlocked(shooter, "pin_down"))
            return;
        double near = Skills.unlocked(shooter, "wide_pin") ? 3.2 : 1.8;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(from, to).inflate(near),
                x -> x != shooter && x.isAlive() && !x.isSpectator() && !shooter.isAlliedTo(x))) {
            Vec3 c = e.getBoundingBox().getCenter(), seg = to.subtract(from);
            double len2 = seg.lengthSqr(), u = len2 < 1e-9 ? 0 : Mth.clamp(c.subtract(from).dot(seg) / len2, 0, 1);
            if (from.add(seg.scale(u)).distanceTo(c) > near)
                continue;
            e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, Skills.unlocked(shooter, "held_pin") ? 80 : 40, 2));
            level.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 6, 0.2, 0.2, 0.2, 0.15);
            // the round's near miss: a pinning ring at the feet and a whine as it passes
            CastFx.ring(level, CastFx.dust(CastFx.EMBER, 1.0f), e.position().add(0, 0.1, 0), 0.8, 12, 0);
            CastFx.line(level, CastFx.dust(CastFx.GOLD, 0.6f), from, to, 1.0);
            CastFx.sound(level, c, SoundEvents.ARROW_HIT, 0.6f, 1.7f);
        }
    }
}
