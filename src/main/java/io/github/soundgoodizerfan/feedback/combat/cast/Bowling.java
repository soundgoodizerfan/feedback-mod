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
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
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

/** Bowling, the throw's upgrade: the thrown body is the ball, knocking over what it hits. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Bowling {

    private Bowling() {
    }

    private record Ball(UUID body, long until, Set<Integer> hit) {
    }

    private static final Map<UUID, Ball> BALLS = new HashMap<>();

    /** Throw upgrade: a thrown body strikes what it meets, staggering each by its mass; it takes the knocks too. */
    public static void launch(ServerPlayer p, LivingEntity body) {
        BALLS.put(p.getUUID(), new Ball(body.getUUID(), p.level().getGameTime() + 25, new HashSet<>()));
        CastFx.sound(p.serverLevel(), p.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.6f);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !BALLS.containsKey(p.getUUID()))
            return;
        Ball b = BALLS.get(p.getUUID());
        ServerLevel lv = p.serverLevel();
        if (!(lv.getEntity(b.body()) instanceof LivingEntity body) || !body.isAlive() || p.level().getGameTime() >= b.until()
                || body.onGround() && p.level().getGameTime() > b.until() - 22) {
            BALLS.remove(p.getUUID());
            return;
        }
        float mass = BodyMass.of(body) + BodyMass.armour(body);
        for (LivingEntity e : near(p, body.getBoundingBox().getCenter(), 1.3)) {
            if (e == body || !b.hit().add(e.getId()))
                continue;
            e.invulnerableTime = 0;
            e.hurt(p.damageSources().playerAttack(p), 4f);
            body.hurt(p.damageSources().playerAttack(p), 2f);
            stagger(p, e, mass * 9.8f * 1.5f);
            Vec3 c = e.getBoundingBox().getCenter();
            lv.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 14, 0.3, 0.3, 0.3, 0.4);
            CastFx.sound(lv, c, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.7f);
        }
        Vec3 c = body.getBoundingBox().getCenter();
        lv.sendParticles(CastFx.dust(CastFx.STEEL, 0.9f), c.x, c.y, c.z, 3, 0.2, 0.2, 0.2, 0);
    }
}
