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
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Smoke Step: a roll out of a cloud that mobs cannot see through. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class SmokeStep {

    private SmokeStep() {
    }

    private record Cloud(UUID owner, ServerLevel level, Vec3 at, double radius, long until) {
    }

    private static final List<Cloud> CLOUDS = new ArrayList<>();

    /** Smoke step: roll out of a cloud left where you stood; anything inside it loses you, unless it is already at your elbow. */
    static boolean act(ServerPlayer p) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        if (p.isPassenger() || look.lengthSqr() < 1e-6)
            return false;
        boolean thick = Skills.unlocked(p, "thick_smoke");
        Cloud c = new Cloud(p.getUUID(), p.serverLevel(), p.position().add(0, 0.8, 0), thick ? 4.5 : 3.0,
                p.level().getGameTime() + (thick ? 100 : 60));
        CLOUDS.add(c);
        Vec3 d = look.normalize();
        p.setDeltaMovement(d.x * 0.9, 0.15, d.z * 0.9);
        p.hurtMarked = true;
        c.level().sendParticles(ParticleTypes.LARGE_SMOKE, c.at().x, c.at().y, c.at().z, 40, c.radius() * 0.3, 0.4, c.radius() * 0.3, 0.03);
        CastFx.ring(c.level(), CastFx.dust(CastFx.SHADOW, 1.6f), c.at(), 1.0, 16, 0.15);
        CastFx.sound(c.level(), c.at(), SoundEvents.FIRECHARGE_USE, 1f, 0.6f);
        CombatCues.everyone(p, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 0.9f);
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (CLOUDS.isEmpty())
            return;
        for (Iterator<Cloud> it = CLOUDS.iterator(); it.hasNext(); ) {
            Cloud c = it.next();
            if (c.level().getGameTime() >= c.until()) {
                it.remove();
                continue;
            }
            if (c.level().getGameTime() % 2 == 0) // a billow, thinning as it runs down
                c.level().sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.at().x, c.at().y + 0.5, c.at().z, 3, c.radius() * 0.35, 0.5, c.radius() * 0.35, 0.005);
            var owner = c.level().getPlayerByUUID(c.owner());
            for (Mob m : c.level().getEntitiesOfClass(Mob.class, new AABB(c.at(), c.at()).inflate(c.radius()), Mob::isAlive))
                if (m.getTarget() != null && m.getTarget().getUUID().equals(c.owner()) && (owner == null || m.distanceToSqr(owner) > 2.25))
                    m.setTarget(null);
        }
    }
}
