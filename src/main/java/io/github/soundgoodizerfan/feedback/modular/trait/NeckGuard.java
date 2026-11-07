// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Sallet (spec 5.4, armour): the tail guards the neck, so a blow from behind (more than
 * {@code back_angle} degrees off the wearer's facing, as the dagger reckons it) does only
 * {@code behind_mult}. The cost is the extra cells of the tail: a heavier shell. Worn: counts once.
 */
public final class NeckGuard implements ModularTrait {

    @Override
    public void onWornHurt(LivingIncomingDamageEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        Entity source = event.getSource().getEntity();
        if (source == null)
            return;
        Vec3 facing = Vec3.directionFromRotation(0, wearer.yBodyRot);
        Vec3 toSource = new Vec3(source.getX() - wearer.getX(), 0, source.getZ() - wearer.getZ()).normalize();
        if (facing.dot(toSource) < Math.cos(Math.toRadians(params.get("back_angle", 120f))))
            event.setAmount(event.getAmount() * params.get("behind_mult", 0.8f));
    }
}
