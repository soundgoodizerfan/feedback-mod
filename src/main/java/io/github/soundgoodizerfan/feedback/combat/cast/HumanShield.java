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
import io.github.soundgoodizerfan.feedback.combat.grab.Grabbing;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Human Shield: the body in your hands takes what was meant for you. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class HumanShield {

    private HumanShield() {
    }

    private static final Set<UUID> SHIELDING = new HashSet<>();

    /** Human shield: the body you hold takes every blow aimed at you for as long as you hold it. If it dies, so does the cover. Needs a hold. */
    static boolean act(ServerPlayer p) {
        if (Grabbing.held(p) == null)
            return false;
        SHIELDING.add(p.getUUID());
        CastFx.sound(p.serverLevel(), p.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8f, 0.6f);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !SHIELDING.contains(p.getUUID()))
            return;
        LivingEntity body = Grabbing.held(p);
        if (body == null || !body.isAlive()) {
            SHIELDING.remove(p.getUUID());
            return;
        }
        if (event.getSource().getEntity() == body)
            return;
        event.setCanceled(true);
        body.invulnerableTime = 0;
        body.hurt(event.getSource(), event.getAmount());
        Vec3 c = body.getBoundingBox().getCenter();
        p.serverLevel().sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 10, 0.25, 0.3, 0.25, 0.3);
        CastFx.sound(p.serverLevel(), c, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8f, 0.8f);
    }
}
