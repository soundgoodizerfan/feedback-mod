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
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Dropkick: dive forward and down; the landing is a kick on what is under it. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Dropkick {

    private Dropkick() {
    }

    private static final java.util.Set<java.util.UUID> DIVING = new java.util.HashSet<>();

    /** Dropkick: in the air, dive forward and down; the landing is a kick on whatever is under it, by your mass over its. */
    static boolean act(ServerPlayer p) {
        if (p.onGround() || p.isPassenger())
            return false;
        Vec3 f = p.getLookAngle().multiply(1, 0, 1);
        f = f.lengthSqr() < 1e-6 ? Vec3.ZERO : f.normalize().scale(0.9);
        p.setDeltaMovement(f.x, -1.0, f.z);
        p.hurtMarked = true;
        p.fallDistance = 0f;
        DIVING.add(p.getUUID());
        CastFx.spray(p.serverLevel(), ParticleTypes.CLOUD, p.position().add(0, 1, 0), 14, 0.25);
        CastFx.sound(p.serverLevel(), p.position(), SoundEvents.PHANTOM_SWOOP, 1f, 0.6f);
        CombatCues.everyone(p, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 0.8f);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        if (DIVING.contains(p.getUUID())) {
            p.fallDistance = 0f;
            if (!p.onGround()) {
                p.serverLevel().sendParticles(CastFx.fade(CastFx.EMBER, 0x331100, 1.6f), p.getX(), p.getY() + 0.6, p.getZ(), 4, 0.25, 0.4, 0.25, 0);
                p.serverLevel().sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1.2, p.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
            }
            if (p.onGround()) {
                DIVING.remove(p.getUUID());
                float joules = (BodyMass.of(p) + BodyMass.armour(p)) * 9.8f * 3f;
                for (LivingEntity e : near(p, p.position(), 2.5)) {
                    e.invulnerableTime = 0;
                    e.hurt(p.damageSources().playerAttack(p), 4f);
                    stagger(p, e, joules);
                }
                Vec3 land = p.position().add(0, 0.2, 0);
                p.serverLevel().sendParticles(ParticleTypes.EXPLOSION_EMITTER, land.x, land.y, land.z, 1, 0, 0, 0, 0);
                CastFx.shockwave(p, land, 2.8, 4, ParticleTypes.CLOUD, 0.3);
                CastFx.spokes(p.serverLevel(), ParticleTypes.CRIT, land, 0.4, 2.8, 8, 0.3);
                CastFx.debris(p.serverLevel(), land, 40, 1.2);
                CastFx.sound(p.serverLevel(), land, SoundEvents.ANVIL_LAND, 1f, 0.6f);
                CastFx.sound(p.serverLevel(), land, SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.6f);
                CombatCues.everyone(p, FSounds.FEEL_HEAVY.get(), FeelTuning.HEAVY_VOLUME, 0.9f);
            }
        }
    }
}
