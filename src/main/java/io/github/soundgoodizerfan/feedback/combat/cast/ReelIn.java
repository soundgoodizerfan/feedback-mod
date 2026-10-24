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
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Reel In: a line on something far off, hauled to your feet and left staggered. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class ReelIn {

    private ReelIn() {
    }

    private record Haul(UUID target, long until) {
    }

    private static final Map<UUID, Haul> HAULS = new HashMap<>();

    /** Reel in: haul the nearest thing within twelve blocks to you on a line; it arrives staggered. Too heavy a body (over 2.5 times your mass) will not come. */
    static boolean act(ServerPlayer p) {
        LivingEntity t = ahead(p, 12, 0.9);
        if (t == null || BodyMass.of(t) > BodyMass.of(p) * 2.5f)
            return false;
        HAULS.put(p.getUUID(), new Haul(t.getUUID(), p.level().getGameTime() + 24));
        CastFx.sound(p.serverLevel(), p.position(), SoundEvents.FISHING_BOBBER_THROW, 1f, 0.6f);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !HAULS.containsKey(p.getUUID()))
            return;
        Haul h = HAULS.get(p.getUUID());
        ServerLevel lv = p.serverLevel();
        if (!(lv.getEntity(h.target()) instanceof LivingEntity t) || !t.isAlive() || p.level().getGameTime() >= h.until()) {
            HAULS.remove(p.getUUID());
            return;
        }
        Vec3 hand = p.getEyePosition().add(0, -0.4, 0), to = hand.subtract(t.getBoundingBox().getCenter());
        double dist = to.length();
        if (dist < 2.0) {
            HAULS.remove(p.getUUID());
            t.setDeltaMovement(Vec3.ZERO);
            stagger(p, t, (BodyMass.of(p) + BodyMass.armour(p)) * 9.8f * Skills.reelScale(p));
            CastFx.sound(lv, t.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.8f);
            return;
        }
        t.setDeltaMovement(to.normalize().scale(Math.min(1.0, dist * 0.25)).add(0, 0.05, 0));
        t.hurtMarked = true;
        CastFx.line(lv, CastFx.dust(CastFx.STEEL, 0.8f), hand, t.getBoundingBox().getCenter(), 0.5);
    }
}
