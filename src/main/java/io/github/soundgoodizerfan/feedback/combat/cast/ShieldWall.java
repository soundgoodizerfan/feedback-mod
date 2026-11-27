// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Shield Wall: anything coming at you inside the cone is turned back. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class ShieldWall {

    private ShieldWall() {
    }

    private record Wall(long until, java.util.Set<Integer> turned) {
    }

    private static final java.util.Map<java.util.UUID, Wall> WALLS = new java.util.HashMap<>();

    /** Shield wall: for three seconds, anything that comes at you inside the cone in front is turned back and staggered, once each, by its mass and speed. Needs a shield-type item in hand. */
    static boolean act(ServerPlayer p) {
        if (p.getMainHandItem().getUseAnimation() != net.minecraft.world.item.UseAnim.BLOCK
                && p.getOffhandItem().getUseAnimation() != net.minecraft.world.item.UseAnim.BLOCK)
            return false;
        WALLS.put(p.getUUID(), new Wall(p.level().getGameTime() + 60, new java.util.HashSet<>()));
        Vec3 feet = p.position().add(0, 0.1, 0);
        CastFx.pulse(p.serverLevel(), feet, Vec3.ZERO, CastFx.ICE, 2.2f, 9);
        CastFx.column(p.serverLevel(), p.position(), CastFx.ICE, 2f, 9);
        CastFx.sound(p.serverLevel(), feet, FSounds.CAST_GUARD.get(), 1f, 0.7f);
        CombatCues.everyone(p, FSounds.FEEL_HEAVY.get(), FeelTuning.HEAVY_VOLUME, 1.2f);
        return true;
    }

    private static void wallTick(ServerPlayer p, Wall w) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 1e-6)
            return;
        look = look.normalize();
        Vec3 face = p.position().add(look.scale(1.2)).add(0, 1, 0);
        // the wall itself: a ring of light standing upright in front, renewed while it lasts
        if (p.level().getGameTime() % 6 == 0 && w.until() - p.level().getGameTime() > 4)
            CastFx.pulse(p.serverLevel(), p.position().add(look.scale(1.5)).add(0, 1, 0), look, CastFx.ICE, 1.3f, 6);
        // Phalanx: allies standing behind you within three blocks are covered by the wall too.
        java.util.List<LivingEntity> covered = new java.util.ArrayList<>(java.util.List.of(p));
        if (io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(p, "phalanx"))
            for (LivingEntity ally : near(p, p.position(), 3))
                if (ally instanceof net.minecraft.world.entity.player.Player && p.isAlliedTo(ally)
                        && ally.position().subtract(p.position()).multiply(1, 0, 1).dot(look) < 0)
                    covered.add(ally);
        for (LivingEntity guarded : covered)
        for (LivingEntity e : near(p, guarded == p ? face : guarded.position().add(look.scale(1.2)).add(0, 1, 0), 2.2)) {
            if (covered.contains(e) || e instanceof net.minecraft.world.entity.player.Player && p.isAlliedTo(e))
                continue;
            Vec3 to = e.position().subtract(guarded.position()).multiply(1, 0, 1);
            if (to.lengthSqr() < 1e-6 || to.normalize().dot(look) < 0.4 || !w.turned().add(e.getId()))
                continue;
            double speed = e.getDeltaMovement().multiply(1, 0, 1).length();
            // STAND-IN: work from mass and a speed term; to be felt in play.
            stagger(p, e, new Work((BodyMass.of(e) + BodyMass.armour(e)) * (5f + 40f * (float) speed)));
            e.push(look.x * 0.9, 0.2, look.z * 0.9);
            e.hurtMarked = true;
            Vec3 hit = e.getBoundingBox().getCenter();
            CastFx.shards(p.serverLevel(), hit, CastFx.ICE, 1.6f, 7);
            CastFx.pulse(p.serverLevel(), hit, look, 0xFFFFFF, 1.4f, 8);
            CastFx.sound(p.serverLevel(), hit, FSounds.CAST_GUARD.get(), 1f, 0.8f);
            CombatCues.everyone(p, FSounds.FEEL_HEAVY.get(), FeelTuning.HEAVY_VOLUME, 1.4f);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        Wall wall = WALLS.get(p.getUUID());
        if (wall != null) {
            if (p.level().getGameTime() >= wall.until())
                WALLS.remove(p.getUUID());
            else
                wallTick(p, wall);
        }
    }
}
