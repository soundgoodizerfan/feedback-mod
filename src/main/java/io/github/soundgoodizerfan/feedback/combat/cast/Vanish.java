// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Vanish: unseen until you strike; what you wear is what gives you away. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Vanish {

    private Vanish() {
    }

    /** Vanish: invisible for about five seconds, less the more armour you wear (it clanks); landing a blow gives you away. */
    static boolean act(ServerPlayer p) {
        int ticks = 100 + (Skills.unlocked(p, "long_shadow") ? 60 : 0) - (int) (BodyMass.armour(p) * 1.5f);
        ticks = Math.max(30, ticks);
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false));
        Vec3 c = p.position().add(0, 1, 0);
        p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 24, 0.3, 0.6, 0.3, 0.04);
        CastFx.spray(p.serverLevel(), CastFx.fade(CastFx.SHADOW, 0x000000, 1.5f), c, 30, 0.4);
        CastFx.sound(p.serverLevel(), c, SoundEvents.ENDERMAN_TELEPORT, 0.7f, 1.8f);
        return true;
    }

    @SubscribeEvent
    public static void onStrike(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof ServerPlayer p && event.getSource().getDirectEntity() == p
                && p.hasEffect(MobEffects.INVISIBILITY) && Skills.owned(p, "vanish")) {
            p.removeEffect(MobEffects.INVISIBILITY);
            Vec3 c = p.position().add(0, 1, 0);
            p.serverLevel().sendParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 14, 0.3, 0.6, 0.3, 0.03);
        }
    }
}
