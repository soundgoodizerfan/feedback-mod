// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;


import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Open helm (spec 5.4, armour): an arrow, bolt or thrown weapon does {@code projectile_mult} of its
 * damage to the wearer, because the face is bare. The gain is in the shape (fewer cells, so a
 * lighter shell that never slows). Worn: one open helm counts once.
 */
public final class OpenFace implements ModularTrait {

    @Override
    public void onWornHurt(LivingIncomingDamageEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        if (event.getSource().is(DamageTypeTags.IS_PROJECTILE))
            event.setAmount(event.getAmount() * params.get("projectile_mult", 1.3f));
    }
}
