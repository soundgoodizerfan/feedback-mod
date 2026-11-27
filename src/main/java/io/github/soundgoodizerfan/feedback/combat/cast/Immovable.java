// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

/**
 * Immovable (Anchor): a rider on the knockback a player takes. With the skill, knockback does not move you; it costs stamina instead, the more
 * the lighter you are: the knockback's strength times {@link #STAMINA_PER_STRENGTH}, scaled by the reference mass over your own
 * ({@link BodyMass} plus armour). With too little stamina left to pay it, you are moved as anyone is.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Immovable {

    private Immovable() {
    }

    /** STAND-INS: stamina per unit of knockback strength at the reference mass, and that mass (Mu). */
    private static final float STAMINA_PER_STRENGTH = 30f, REFERENCE_MASS = 90f;

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !Skills.unlocked(p, "immovable"))
            return;
        float mass = Math.max(1f, BodyMass.of(p) + BodyMass.armour(p));
        float cost = event.getStrength() * STAMINA_PER_STRENGTH * REFERENCE_MASS / mass;
        if (Stamina.spend(p, cost)) {
            event.setCanceled(true);
            net.minecraft.world.phys.Vec3 at = p.position().add(0, 0.1, 0);
            CastFx.pulse(p.serverLevel(), at, net.minecraft.world.phys.Vec3.ZERO, CastFx.STEEL, 1.2f, 7);
            CastFx.sound(p.serverLevel(), at, io.github.soundgoodizerfan.feedback.registry.FSounds.CAST_GUARD.get(), 0.7f, 0.9f);
        }
    }
}
