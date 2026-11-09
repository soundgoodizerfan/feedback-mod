// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.hit.WeaponProfile;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

/**
 * Observation's tipping point: a blow from a precise weapon that lands at the tip of the reach goes past a quarter of the
 * target's armour and does not knock it back. The weapon's precision comes from {@link WeaponProfile}, never its class.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Tipping {

    private Tipping() {
    }

    /** STAND-INS ([OPEN]): precision share needed, the share of the armour a tip blow goes past. */
    static final float MIN_PRECISION = 0.5f, IGNORED = 0.25f;

    private static final Map<UUID, Long> TIPPED = new HashMap<>();

    /** Share of a blow that gets through {@code armor} and {@code toughness} (vanilla's formula). */
    private static float through(float damage, float armor, float toughness) {
        float a = Math.min(20f, Math.max(armor / 5f, armor - damage / (2f + toughness / 4f)));
        return 1f - a / 25f;
    }

    @SubscribeEvent
    public static void onBlow(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getSource().getDirectEntity() != p
                || !event.getSource().is(DamageTypes.PLAYER_ATTACK) || !Skills.unlocked(p, "tipping_point"))
            return;
        LivingEntity t = event.getEntity();
        double reach = p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE);
        if (p.distanceTo(t) < reach * SkillTuning.REACH_TIP || WeaponProfile.meleeOf(p).precision() < MIN_PRECISION)
            return;
        float armor = t.getArmorValue(), tough = (float) t.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
        float d = event.getAmount();
        float base = through(d, armor, tough);
        if (base > 0f)
            event.setAmount(d * through(d, armor * (1f - IGNORED), tough) / base);
        TIPPED.put(t.getUUID(), t.level().getGameTime());
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        Long at = TIPPED.get(event.getEntity().getUUID());
        if (at != null && at == event.getEntity().level().getGameTime())
            event.setCanceled(true);
    }
}
