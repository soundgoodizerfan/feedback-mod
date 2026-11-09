// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;

/**
 * Overwatch: hold a loaded gun on a lane for five seconds. Every living thing that crosses the aim cone is pinned, as by
 * Pin down, with no shot fired; each one caught costs stamina, and the watch ends when the gun leaves the hand, the
 * player moves off the spot or the stamina runs dry. Area denial from a gun that is not trying to hit.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Overwatch {

    private Overwatch() {
    }

    /** STAND-INS ([OPEN]): ticks watched, cone (cosine of the half angle), range in blocks, stamina each catch costs, blocks the watcher may drift. */
    private static final int TICKS = 100, PIN_TICKS = 40;
    private static final double CONE = 0.92, RANGE = 20, DRIFT = 1.5;
    private static final float CATCH_COST = 6f;

    private record Watch(long until, Vec3 spot, Set<UUID> caught) {
    }

    private static final Map<UUID, Watch> WATCHES = new HashMap<>();

    /** Overwatch: with a loaded gun in hand, watch the lane ahead for five seconds. */
    static boolean act(ServerPlayer p) {
        var gun = p.getMainHandItem();
        if (!(gun.getItem() instanceof GunItem) || GunAmmo.count(gun) < 1)
            return false;
        WATCHES.put(p.getUUID(), new Watch(p.level().getGameTime() + TICKS, p.position(), new HashSet<>()));
        CastFx.sound(p.serverLevel(), p.position(), SoundEvents.CROSSBOW_LOADING_END.value(), 0.8f, 0.7f);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !WATCHES.containsKey(p.getUUID()))
            return;
        Watch w = WATCHES.get(p.getUUID());
        var gun = p.getMainHandItem();
        if (p.level().getGameTime() >= w.until() || !(gun.getItem() instanceof GunItem) || GunAmmo.count(gun) < 1
                || p.position().distanceTo(w.spot()) > DRIFT) {
            WATCHES.remove(p.getUUID());
            return;
        }
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        if (p.tickCount % 5 == 0) // the watched lane, drawn faintly
            CastFx.line(p.serverLevel(), CastFx.dust(CastFx.GOLD, 0.5f), eye.add(look.scale(1.5)), eye.add(look.scale(RANGE * 0.6)), 0.3);
        for (LivingEntity e : near(p, eye.add(look.scale(RANGE / 2)), RANGE / 2 + 2)) {
            if (w.caught().contains(e.getUUID()) || !p.hasLineOfSight(e))
                continue;
            if (e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < CONE)
                continue;
            if (!Stamina.spend(p, CATCH_COST)) {
                WATCHES.remove(p.getUUID());
                return;
            }
            w.caught().add(e.getUUID());
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PIN_TICKS, 2));
            Disorder.note(p, e, Disorder.Kind.PINNED);
            Vec3 c = e.getBoundingBox().getCenter();
            p.serverLevel().sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 6, 0.2, 0.2, 0.2, 0.15);
            CastFx.ring(p.serverLevel(), CastFx.dust(CastFx.EMBER, 1.0f), e.position().add(0, 0.1, 0), 0.8, 12, 0);
            CastFx.sound(p.serverLevel(), c, SoundEvents.ARROW_HIT, 0.6f, 1.7f);
        }
    }
}
