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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

/** Slide: a low skid along the ground that takes the legs from whatever it passes. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Slide {

    private Slide() {
    }

    private record Skid(long until, Set<Integer> swept) {
    }

    private static final Map<UUID, Skid> SKIDS = new HashMap<>();

    /** Slide: from a sprint, skid low along the ground for most of a second; each body in the way is tripped once, by your mass. */
    static boolean act(ServerPlayer p) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        if (!p.onGround() || !p.isSprinting() || p.isPassenger() || look.lengthSqr() < 1e-6)
            return false;
        SKIDS.put(p.getUUID(), new Skid(p.level().getGameTime() + 14, new HashSet<>()));
        CombatCues.everyone(p, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 0.7f);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !SKIDS.containsKey(p.getUUID()))
            return;
        Skid s = SKIDS.get(p.getUUID());
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        if (p.level().getGameTime() >= s.until() || !p.onGround() || look.lengthSqr() < 1e-6) {
            SKIDS.remove(p.getUUID());
            return;
        }
        Vec3 d = look.normalize();
        p.setDeltaMovement(d.x * 0.75, p.getDeltaMovement().y, d.z * 0.75);
        p.hurtMarked = true;
        ServerLevel lv = p.serverLevel();
        float joules = (BodyMass.of(p) + BodyMass.armour(p)) * 9.8f * 1.0f;
        for (LivingEntity e : near(p, p.position().add(d.scale(0.8)), 1.0)) {
            if (!s.swept().add(e.getId()))
                continue;
            e.invulnerableTime = 0;
            e.hurt(p.damageSources().playerAttack(p), 2f);
            e.push(0, 0.25, 0);
            e.hurtMarked = true;
            stagger(p, e, joules);
            lv.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + 0.3, e.getZ(), 8, 0.2, 0.1, 0.2, 0.2);
            CastFx.sound(lv, e.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.9f, 0.9f);
        }
        CastFx.debris(lv, p.position(), 6, 0.3);
        lv.sendParticles(ParticleTypes.CLOUD, p.getX() - d.x * 0.5, p.getY() + 0.1, p.getZ() - d.z * 0.5, 2, 0.2, 0.02, 0.2, 0.02);
        if (p.level().getGameTime() % 4 == 0)
            CastFx.sound(lv, p.position(), SoundEvents.GRAVEL_STEP, 0.7f, 0.7f);
    }
}
