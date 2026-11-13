// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Shadow Step (a flanking dash) and its Ambush rider. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class ShadowStep {

    private ShadowStep() {
    }

    /** The game tick until which the first blow after a Shadow Step is an Ambush. */
    private static final java.util.Map<java.util.UUID, Long> AMBUSH = new java.util.HashMap<>();

    /** A flank in progress: where it ends, who it is aimed at, ticks left, and the health you had (a hurt breaks it). */
    private record Dash(Vec3 to, java.util.UUID target, int left, float health) {
    }

    private static final java.util.Map<java.util.UUID, Dash> DASHES = new java.util.HashMap<>();

    /**
     * Shadow step: a fast flanking sidestep toward the blind side of the nearest thing in front, at most a few blocks. A real dash on the
     * ground: it stops short at any wall, and a blow taken or a bump ends it.
     */
    static boolean act(ServerPlayer p) {
        LivingEntity t = ahead(p, CastTuning.FLANK_REACH, 0.5);
        if (t == null || p.isPassenger())
            return false;
        Vec3 facing = t.getLookAngle().multiply(1, 0, 1);
        facing = facing.lengthSqr() < 1e-6 ? Vec3.directionFromRotation(0, t.getYRot()) : facing.normalize();
        Vec3 blind = t.position().subtract(facing.scale(1.3));
        Vec3 way = blind.subtract(p.position()).multiply(1, 0, 1);
        if (way.length() < 0.6)
            return false;
        Vec3 to = p.position().add(way.normalize().scale(Math.min(way.length(), CastTuning.FLANK_SIDE)));
        Vec3 up = new Vec3(0, 0.5, 0);
        if (p.level().clip(new net.minecraft.world.level.ClipContext(p.position().add(up), to.add(up),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p))
                .getType() != net.minecraft.world.phys.HitResult.Type.MISS
                || !p.level().noCollision(p, p.getBoundingBox().move(to.subtract(p.position()))))
            return false;
        ServerLevel level = p.serverLevel();
        Vec3 from = p.position().add(0, 1, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, from.x, from.y, from.z, 8, 0.3, 0.5, 0.3, 0.02);
        CastFx.sound(level, from, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);
        CombatCues.everyone(p, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 1.6f);
        DASHES.put(p.getUUID(), new Dash(to, t.getUUID(), CastTuning.FLANK_TICKS, p.getHealth()));
        AMBUSH.put(p.getUUID(), p.level().getGameTime() + CastTuning.AMBUSH_TICKS);
        return true;
    }

    @SubscribeEvent
    public static void onTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (DASHES.isEmpty() || !(event.getEntity() instanceof ServerPlayer p))
            return;
        Dash d = DASHES.get(p.getUUID());
        if (d == null)
            return;
        Vec3 left = d.to().subtract(p.position()).multiply(1, 0, 1);
        if (d.left() <= 0 || left.length() < 0.35 || p.getHealth() < d.health() || p.horizontalCollision || p.isPassenger()) {
            DASHES.remove(p.getUUID());
            Entity t = p.serverLevel().getEntity(d.target());
            if (t != null && left.length() < 0.35) { // arrived: turn to the back in front of you
                Vec3 face = t.position().subtract(p.position());
                p.setYRot((float) (Mth.atan2(-face.x, face.z) * Mth.RAD_TO_DEG));
                p.connection.teleport(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
            }
            return;
        }
        Vec3 step = left.normalize().scale(Math.min(CastTuning.FLANK_SPEED, left.length()));
        p.setDeltaMovement(step.x, p.getDeltaMovement().y, step.z);
        p.hurtMarked = true;
        Vec3 at = p.position().add(0, 0.9, 0);
        p.serverLevel().sendParticles(CastFx.fade(CastFx.SHADOW, 0x000000, 1.2f), at.x, at.y, at.z, 3, 0.15, 0.4, 0.15, 0);
        DASHES.put(p.getUUID(), new Dash(d.to(), d.target(), d.left() - 1, d.health()));
    }

    /** Ambush (rider): the first blow within three seconds of a Shadow step staggers its target, whatever it faces. */
    @SubscribeEvent
    public static void onAmbush(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        var src = event.getSource();
        if (!(src.getEntity() instanceof ServerPlayer p) || src.getDirectEntity() != p || event.getNewDamage() <= 0f
                || !Skills.unlocked(p, "ambush"))
            return;
        Long until = AMBUSH.remove(p.getUUID());
        if (until == null || p.level().getGameTime() > until)
            return;
        stagger(p, event.getEntity(), event.getNewDamage() * ParryTuning.JOULES_PER_DAMAGE * 3f);
    }
}
