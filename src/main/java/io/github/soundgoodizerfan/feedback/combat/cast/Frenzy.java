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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Frenzy: hands fast for five seconds, then three seconds of gasping. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Frenzy {

    private Frenzy() {
    }

    private static final ResourceLocation FAST = Feedback.id("frenzy"), WINDED = Feedback.id("frenzy_winded");

    /** Per fighter: the game tick the frenzy ends and the tick the gasping after it ends. */
    private static final Map<UUID, long[]> STATE = new HashMap<>();

    /** Frenzy: with empty hands, swing eighty percent faster for five seconds; afterwards three seconds of swinging a third slower, winded. */
    static boolean act(ServerPlayer p) {
        if (!p.getMainHandItem().isEmpty())
            return false;
        long now = p.level().getGameTime();
        var speed = p.getAttribute(Attributes.ATTACK_SPEED);
        speed.removeModifier(WINDED);
        speed.removeModifier(FAST);
        speed.addTransientModifier(new AttributeModifier(FAST, 0.8, Operation.ADD_MULTIPLIED_TOTAL));
        STATE.put(p.getUUID(), new long[]{now + 100, now + 160});
        CastFx.ring(p.serverLevel(), CastFx.dust(CastFx.CRIMSON, 1.4f), p.position().add(0, 1, 0), 0.9, 16, 0.2);
        CastFx.sound(p.serverLevel(), p.position(), SoundEvents.RAVAGER_ROAR, 0.8f, 1.4f);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !STATE.containsKey(p.getUUID()))
            return;
        long now = p.level().getGameTime();
        long[] s = STATE.get(p.getUUID());
        var speed = p.getAttribute(Attributes.ATTACK_SPEED);
        if (now < s[0]) {
            if (now % 4 == 0) { // the hands are lit
                Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(0, -0.5, 0);
                p.serverLevel().sendParticles(CastFx.dust(CastFx.CRIMSON, 0.9f), hand.x, hand.y, hand.z, 3, 0.2, 0.1, 0.2, 0);
            }
        } else if (speed.hasModifier(FAST)) {
            speed.removeModifier(FAST);
            speed.addTransientModifier(new AttributeModifier(WINDED, -0.35, Operation.ADD_MULTIPLIED_TOTAL));
            CastFx.sound(p.serverLevel(), p.position(), SoundEvents.PLAYER_BREATH, 1f, 0.8f);
        } else if (now >= s[1]) {
            speed.removeModifier(WINDED);
            STATE.remove(p.getUUID());
        }
    }
}
