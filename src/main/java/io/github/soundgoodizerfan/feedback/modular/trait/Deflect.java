// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import io.github.soundgoodizerfan.feedback.modular.RangedDispatch;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/**
 * The kite shield (spec 5.4). This trait is the data that says the shield deflects: on the parry key
 * {@code combat/parry/Parrying} turns a projectile back along the look at {@code return_speed} of its
 * speed (the one parry system; a kite does not parry a melee blow). Cost, on the passive raise: the
 * long face is narrow to a thrust, so a melee blow is held to {@code melee_mult} of what the face
 * could take.
 */
public final class Deflect implements ModularTrait {

    @Override
    public void onShieldBlock(LivingShieldBlockEvent event, LivingEntity blocker, ItemStack shield, Params params) {
        if (!event.getBlocked() || ModularStacks.broken(shield))
            return;
        if (!(event.getDamageSource().getDirectEntity() instanceof Projectile))
            event.setBlockedDamage(Math.min(event.getBlockedDamage(), RangedDispatch.capacity(blocker, shield) * params.get("melee_mult", 0.75f)));
    }
}
