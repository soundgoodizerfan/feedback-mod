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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Quake Leap: vault forward and up; the landing shakes everything near. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class QuakeLeap {

    private QuakeLeap() {
    }

    /** In the air: the highest point it reached, for the fall that lands it. */
    private static final java.util.Map<java.util.UUID, double[]> LEAPING = new java.util.HashMap<>();

    /** Vaults forward and up; the landing shakes everything near, by the mass that comes down and how far. */
    static boolean act(ServerPlayer p) {
        if (!p.onGround())
            return false;
        Vec3 f = p.getLookAngle().multiply(1, 0, 1);
        f = f.lengthSqr() < 1e-6 ? Vec3.ZERO : f.normalize().scale(0.7);
        p.setDeltaMovement(f.x, 1.05, f.z);
        p.hurtMarked = true;
        LEAPING.put(p.getUUID(), new double[]{p.getY(), p.level().getGameTime()});
        ServerLevel lv = p.serverLevel();
        Vec3 at = p.position().add(0, 0.1, 0);
        CastFx.ring(lv, ParticleTypes.CLOUD, at, 0.5, 16, 0.3);
        CastFx.spokes(lv, ParticleTypes.CRIT, at, 0.3, 1.6, 6, 0.3);
        CastFx.debris(lv, at, 24, 0.6);
        lv.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        CastFx.sound(lv, at, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.8f, 0.6f);
        CastFx.sound(lv, at, SoundEvents.ANVIL_LAND, 0.4f, 0.5f);
        CombatCues.everyone(p, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 0.7f);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        double[] air = LEAPING.get(p.getUUID());
        if (air == null)
            return;
        air[0] = Math.max(air[0], p.getY());
        p.fallDistance = 0f; // the leap lands without hurting
        if (!p.onGround()) {
            p.serverLevel().sendParticles(CastFx.fade(CastFx.STEEL, CastFx.EMBER, 1.4f), p.getX(), p.getY() + 0.4, p.getZ(), 3, 0.2, 0.3, 0.2, 0);
            p.serverLevel().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 1, 0.1, 0.1, 0.1, 0.01);
        }
        if (p.level().getGameTime() - (long) air[1] < 4 || !p.onGround())
            return;
        LEAPING.remove(p.getUUID());
        double fall = Math.max(0.5, air[0] - p.getY() + 1.5);
        float joules = (float) ((BodyMass.of(p) + BodyMass.armour(p)) * 9.8 * fall);
        ServerLevel level = p.serverLevel();
        for (LivingEntity e : near(p, p.position(), 4.5)) {
            Vec3 away = e.position().subtract(p.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1e-6 ? Vec3.ZERO : away.normalize();
            e.push(away.x * 0.6, 0.35, away.z * 0.6);
            e.hurtMarked = true;
            stagger(p, e, joules);
        }
        Vec3 land = p.position().add(0, 0.1, 0);
        level.sendParticles(fall > 2.5 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, land.x, land.y, land.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CLOUD, land.x, land.y, land.z, 40, 1.8, 0.05, 1.8, 0.1);
        CastFx.shockwave(p, land, 4.5, 6, ParticleTypes.CLOUD, 0.35);
        CastFx.shockwave(p, land, 4.5, 6, CastFx.dust(CastFx.STEEL, 1.5f), 0);
        CastFx.spokes(level, ParticleTypes.CRIT, land, 0.5, 4.5, 10, 0.35);
        CastFx.debris(level, land, 60, 2.0);
        CastFx.sound(level, land, SoundEvents.ANVIL_LAND, 1f, 0.5f);
        CastFx.sound(level, land, SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 0.7f);
        level.playSound(null, p.blockPosition(), FSounds.FEEL_HEAVY.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.6f);
    }
}
