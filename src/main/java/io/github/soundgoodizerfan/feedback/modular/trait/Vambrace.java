// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;


import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

/**
 * Vambrace (spec 5.3, armour addon on a glove): a cuff on the forearm turns a melee blow aside when the main hand
 * is empty or the blow is light (at most {@code light_damage}): it does {@code parry_mult} of its damage and shoves
 * {@code stagger_mult} as hard. A disarming shot cannot pull what the wearer holds ({@link #resistsDisarm}). Worn: counts once.
 */
public final class Vambrace implements ModularTrait {

    /** The wearer and the tick of the last blow the cuff turned, so the shove that follows is softened. */
    private static LivingEntity parried;
    private static long parriedAt;

    @Override
    public void onWornHurt(LivingIncomingDamageEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        if (!(event.getSource().getDirectEntity() instanceof LivingEntity) || event.getSource().is(DamageTypeTags.IS_PROJECTILE))
            return;
        if (!wearer.getMainHandItem().isEmpty() && event.getAmount() > params.get("light_damage", 4f))
            return;
        event.setAmount(event.getAmount() * params.get("parry_mult", 0.6f));
        parried = wearer;
        parriedAt = wearer.level().getGameTime();
    }

    @Override
    public void onWornKnockback(LivingKnockBackEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        if (parried == wearer && parriedAt == wearer.level().getGameTime()) {
            event.setStrength(event.getStrength() * params.get("stagger_mult", 0.5f));
            parried = null;
        }
    }

    /** Whether {@code target} wears a vambrace, which keeps a disarming shot from taking what it holds. */
    public static boolean resistsDisarm(LivingEntity target) {
        // A lanyard on what the target holds is a line to the hand: it cannot be stripped away.
        if (ModularTraits.addons(target.getMainHandItem()).stream().anyMatch(a -> a.id().equals("lanyard")))
            return true;
        for (ModularTraits.Worn w : ModularTraits.worn(target))
            if (w.trait() instanceof Vambrace)
                return true;
        return false;
    }
}
