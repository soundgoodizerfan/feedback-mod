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
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Rebuke: for a few seconds a man who strikes you is struck back for most of what he dealt. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Rebuke {

    private Rebuke() {
    }

    private static final Map<UUID, Long> UNTIL = new HashMap<>();
    private static boolean returning;

    /** Rebuke: for three seconds, every melee blow you take is struck back at the attacker for sixty percent of what it dealt. You still take all of it. */
    static boolean act(ServerPlayer p) {
        UNTIL.put(p.getUUID(), p.level().getGameTime() + 60);
        Vec3 c = p.position().add(0, 1, 0);
        CastFx.ring(p.serverLevel(), CastFx.dust(CastFx.CRIMSON, 1.3f), p.position().add(0, 0.1, 0), 1.0, 16, 0.1);
        CastFx.sound(p.serverLevel(), c, SoundEvents.IRON_GOLEM_DAMAGE, 0.9f, 0.7f);
        return true;
    }

    @SubscribeEvent
    public static void onStruck(LivingDamageEvent.Post event) {
        if (returning || !(event.getEntity() instanceof ServerPlayer p) || !(event.getSource().getEntity() instanceof LivingEntity foe)
                || event.getSource().getDirectEntity() != foe || event.getNewDamage() <= 0f)
            return;
        Long until = UNTIL.get(p.getUUID());
        if (until == null || p.level().getGameTime() > until)
            return;
        returning = true;
        try {
            foe.invulnerableTime = 0;
            foe.hurt(p.damageSources().playerAttack(p), event.getNewDamage() * 0.6f);
        } finally {
            returning = false;
        }
        Vec3 c = foe.getBoundingBox().getCenter();
        p.serverLevel().sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 12, 0.3, 0.3, 0.3, 0.4);
        p.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 8, 0.2, 0.2, 0.2, 0.4);
        CastFx.sound(p.serverLevel(), c, SoundEvents.PLAYER_ATTACK_CRIT, 0.9f, 0.7f);
    }
}
