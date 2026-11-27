// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.Rearms;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Rebuke, a counter-strike reflex (a rider on being struck, not a cast): a melee blow that reaches you inside your own
 * parry window, one the parry did not catch (it came from outside the cone), is answered at once with a short strike at
 * the one who struck. The answer costs stamina and uses your swing, and waits on the clock before the next.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Rebuke {

    private Rebuke() {
    }

    private static boolean returning;

    @SubscribeEvent
    public static void onStruck(LivingDamageEvent.Post event) {
        if (returning || !(event.getEntity() instanceof ServerPlayer p) || !(event.getSource().getEntity() instanceof LivingEntity foe)
                || event.getSource().getDirectEntity() != foe || event.getNewDamage() <= 0f
                || CombatState.of(p).phase() != CombatPhase.PARRY_ACTIVE || !Skills.unlocked(p, "rebuke")
                || foe.distanceToSqr(p) > CastTuning.REBUKE_REACH * CastTuning.REBUKE_REACH || !Rearms.ready(p, "rebuke")
                || !Stamina.spend(p, CastTuning.REBUKE_STAMINA))
            return;
        Rearms.spend(p, "rebuke");
        returning = true;
        try {
            foe.invulnerableTime = 0;
            foe.hurt(p.damageSources().playerAttack(p), (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE));
        } finally {
            returning = false;
        }
        p.swing(InteractionHand.MAIN_HAND, true);
        Vec3 c = foe.getBoundingBox().getCenter();
        CastFx.shards(p.serverLevel(), c, CastFx.STEEL, 1.1f, 6);
        CastFx.streak(p.serverLevel(), p.getEyePosition().add(0, -0.3, 0), c, CastFx.STEEL, 0.03f, 4);
        CastFx.sound(p.serverLevel(), c, FSounds.CAST_PUNCH.get(), 0.9f, 0.9f);
    }
}
