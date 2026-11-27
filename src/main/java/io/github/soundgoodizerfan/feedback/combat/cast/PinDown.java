// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;

/** Pin Down, always on once owned (a rider on firing, not a cast): rounds you fire that pass close to a living thing slow it hard, hit or not. */
public final class PinDown {

    private PinDown() {
    }

    /** A round of {@code shooter} flew {@code from} to {@code to}: anything it passed close to is pinned if Pin Down is up. Called by the shot's step. */
    public static void flyby(ServerLevel level, LivingEntity shooter, Vec3 from, Vec3 to) {
        if (!Skills.unlocked(shooter, "pin_down"))
            return;
        double near = 1.8;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(from, to).inflate(near),
                x -> x != shooter && x.isAlive() && !x.isSpectator() && !shooter.isAlliedTo(x))) {
            Vec3 c = e.getBoundingBox().getCenter(), seg = to.subtract(from);
            double len2 = seg.lengthSqr(), u = len2 < 1e-9 ? 0 : Mth.clamp(c.subtract(from).dot(seg) / len2, 0, 1);
            if (from.add(seg.scale(u)).distanceTo(c) > near)
                continue;
            e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
            if (shooter instanceof net.minecraft.server.level.ServerPlayer sp)
                Disorder.note(sp, e, Disorder.Kind.PINNED);
            // the round's near miss: a pinning ring at the feet and a whine as it passes
            CastFx.pulse(level, e.position().add(0, 0.1, 0), Vec3.ZERO, CastFx.EMBER, 0.9f, 8);
            CastFx.streak(level, from, to, CastFx.GOLD, 0.02f, 4);
            CastFx.sound(level, c, FSounds.CAST_LOCK.get(), 0.6f, 1.4f);
        }
    }
}
