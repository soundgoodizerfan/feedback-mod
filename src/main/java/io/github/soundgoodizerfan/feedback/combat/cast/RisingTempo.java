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
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Rising Tempo, always on once owned (not a cast): with empty hands, each blow landed within a second and a half of the last is fifteen percent harder than the one before, to eight deep; a pause breaks it. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class RisingTempo {

    private RisingTempo() {
    }

    /** Per fighter: the game tick the chain breaks and the blows landed so far. */
    private static final Map<UUID, long[]> CHAINS = new HashMap<>();
    private static final int WINDOW = 30, MAX = 8;
    private static final float PER_BLOW = 0.15f;

    @SubscribeEvent
    public static void onBlow(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getSource().getDirectEntity() != p)
            return;
        if (!p.getMainHandItem().isEmpty() || !io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(p, "rising_tempo"))
            return;
        // Innate: the first bare-handed blow starts the chain, so there is nothing to cast.
        long[] chain = CHAINS.computeIfAbsent(p.getUUID(), k -> new long[]{0, 0});
        event.setAmount(event.getAmount() * (1f + PER_BLOW * chain[1]));
        chain[1] = Math.min(MAX, chain[1] + 1);
        chain[0] = p.level().getGameTime() + WINDOW;
        Vec3 c = event.getEntity().getBoundingBox().getCenter();
        p.serverLevel().sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 3 + (int) chain[1], 0.2, 0.2, 0.2, 0.3);
        CastFx.sound(p.serverLevel(), c, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8f, 0.7f + chain[1] * 0.1f);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !CHAINS.containsKey(p.getUUID()))
            return;
        if (p.level().getGameTime() >= CHAINS.get(p.getUUID())[0]) { // a miss, or a pause: the tempo drops
            CHAINS.remove(p.getUUID());
            CastFx.ring(p.serverLevel(), ParticleTypes.CLOUD, p.position().add(0, 1, 0), 0.7, 10, 0.1);
        }
    }
}
