// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;


import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/**
 * A metal boss on a shield face: a blow the shield turns is answered by the boss, which hurts the melee attacker for {@code bash}
 * and shoves them back by {@code shove}. The boss costs mass and a weak spot (its role's {@code capacity_mult}, read by the bake).
 */
public final class Boss implements ModularTrait {

    @Override
    public void onShieldBlock(LivingShieldBlockEvent event, LivingEntity blocker, ItemStack shield, Params params) {
        if (event.getBlocked() && event.getDamageSource().getDirectEntity() instanceof LivingEntity attacker && attacker != blocker) {
            attacker.hurt(blocker.damageSources().thorns(blocker), params.get("bash", 3f));
            attacker.knockback(params.get("shove", 0.5f), blocker.getX() - attacker.getX(), blocker.getZ() - attacker.getZ());
        }
    }
}
