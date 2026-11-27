// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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

    /** Frenzy: with bare hands or a quick weapon, swing eighty percent faster for five seconds; afterwards three seconds of swinging a third slower, winded. */
    static boolean act(ServerPlayer p) {
        if (io.github.soundgoodizerfan.feedback.combat.hit.WeaponProfile.meleeOf(p).tempo() < io.github.soundgoodizerfan.feedback.combat.hit.ProfileTuning.TEMPO_MIN)
            return false;
        long now = p.level().getGameTime();
        var speed = p.getAttribute(Attributes.ATTACK_SPEED);
        speed.removeModifier(WINDED);
        speed.removeModifier(FAST);
        speed.addTransientModifier(new AttributeModifier(FAST, 0.8, Operation.ADD_MULTIPLIED_TOTAL));
        STATE.put(p.getUUID(), new long[]{now + 100, now + 160});
        CastFx.pulse(p.serverLevel(), p.position().add(0, 0.1, 0), Vec3.ZERO, CastFx.CRIMSON, 3.0f, 9);
        CastFx.column(p.serverLevel(), p.position(), CastFx.CRIMSON, 3.0f, 12);
        CastFx.shards(p.serverLevel(), p.position().add(0, 1, 0), CastFx.CRIMSON, 1.8f, 8);
        CastFx.trail(p.serverLevel(), p, new Vec3(0, 1.0, 0), CastFx.CRIMSON, 0.14f, 100);
        CastFx.sound(p.serverLevel(), p.position(), io.github.soundgoodizerfan.feedback.registry.FSounds.CAST_SURGE.get(), 1f, 1.0f);
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
            if (now % 6 == 0) { // the hands crackle
                Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(0, -0.5, 0);
                var r = p.getRandom();
                CastFx.bolt(p.serverLevel(), hand, hand.add(r.nextGaussian() * 0.5, r.nextGaussian() * 0.4, r.nextGaussian() * 0.5), CastFx.CRIMSON, 0.012f, 3);
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
