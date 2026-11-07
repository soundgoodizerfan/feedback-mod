// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Brigandine (spec 5.4, armour): the rivets bite whoever strikes the wearer in melee, for
 * {@code per_stud} damage for each stud set in the piece, at most {@code max}; no studs, no bite.
 * Costs a point of the piece's wear a reflection ({@code wear}), and the shell is the light, short
 * one (fewer cells, so less cover). Not for projectiles or for the reflection itself. Worn: counts once.
 */
public final class Studded implements ModularTrait {

    private static boolean reflecting;

    @Override
    public void onWornHurt(LivingIncomingDamageEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        if (reflecting || event.getSource().is(DamageTypeTags.IS_PROJECTILE) || event.getSource().is(DamageTypeTags.IS_FALL)
                || !(event.getSource().getDirectEntity() instanceof LivingEntity attacker) || attacker == wearer)
            return;
        ModularDesign design = ModularStacks.design(piece);
        ModularDesign.Part studs = design == null ? null : design.part("T");
        if (studs == null)
            return;
        float damage = Math.min(params.get("max", 3f), params.get("per_stud", 1f) * studs.count());
        reflecting = true;
        try {
            attacker.hurt(wearer.damageSources().thorns(wearer), damage);
        } finally {
            reflecting = false;
        }
        piece.hurtAndBreak(ModularStacks.cap(piece, (int) params.get("wear", 1f)), wearer,
                ((net.minecraft.world.item.ArmorItem) piece.getItem()).getEquipmentSlot());
    }
}
