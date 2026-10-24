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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
        CastFx.ring(p.serverLevel(), CastFx.dust(CastFx.ICE, 1.4f), feet, 1.2, 20, 0.25);
        CastFx.sound(p.serverLevel(), feet, SoundEvents.ANVIL_PLACE, 1f, 0.5f);
        CastFx.sound(p.serverLevel(), feet, SoundEvents.BEACON_ACTIVATE, 0.6f, 1.6f);
        CombatCues.everyone(p, FSounds.FEEL_HEAVY.get(), FeelTuning.HEAVY_VOLUME, 1.2f);
        return true;
    }

    private static void wallTick(ServerPlayer p, Wall w) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 1e-6)
            return;
        look = look.normalize();
        Vec3 face = p.position().add(look.scale(1.2)).add(0, 1, 0);
        p.serverLevel().sendParticles(ParticleTypes.CRIT, face.x, face.y, face.z, 2, 0.5, 0.6, 0.5, 0);
        // the wall itself: a curved pane of light in front, shimmering from the feet up to over the head
        if (p.level().getGameTime() % 2 == 0) {
            long left = w.until() - p.level().getGameTime();
            double flare = left < 12 ? left / 12.0 : 1; // thins out as it runs down
            for (int col = -3; col <= 3; col++) {
                double a = col * 0.22;
                Vec3 dir = new Vec3(look.x * Math.cos(a) - look.z * Math.sin(a), 0, look.x * Math.sin(a) + look.z * Math.cos(a));
                for (int row = 0; row < 4; row++)
                    if (p.getRandom().nextDouble() < flare)
                        p.serverLevel().sendParticles(CastFx.dust(row % 2 == 0 ? CastFx.ICE : CastFx.STEEL, 1.1f),
                                p.getX() + dir.x * 1.5, p.getY() + 0.2 + row * 0.55, p.getZ() + dir.z * 1.5, 1, 0, 0, 0, 0);
            }
        }
        for (LivingEntity e : near(p, face, 2.2)) {
            Vec3 to = e.position().subtract(p.position()).multiply(1, 0, 1);
            if (to.lengthSqr() < 1e-6 || to.normalize().dot(look) < 0.4 || !w.turned().add(e.getId()))
                continue;
            double speed = e.getDeltaMovement().multiply(1, 0, 1).length();
            // STAND-IN: joules from mass and a speed term; to be felt in play.
            stagger(p, e, (BodyMass.of(e) + BodyMass.armour(e)) * (5f + 40f * (float) speed));
            e.push(look.x * 0.9, 0.2, look.z * 0.9);
            e.hurtMarked = true;
            Vec3 hit = e.getBoundingBox().getCenter();
            p.serverLevel().sendParticles(ParticleTypes.EXPLOSION, hit.x, hit.y, hit.z, 1, 0, 0, 0, 0);
            CastFx.spray(p.serverLevel(), ParticleTypes.ELECTRIC_SPARK, hit, 20, 0.6);
            CastFx.ring(p.serverLevel(), CastFx.dust(CastFx.ICE, 1.3f), face, 1.0, 16, 0.3);
            CastFx.sound(p.serverLevel(), hit, SoundEvents.SHIELD_BLOCK, 1f, 0.5f);
            CastFx.sound(p.serverLevel(), hit, SoundEvents.ANVIL_LAND, 0.7f, 0.7f);
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
