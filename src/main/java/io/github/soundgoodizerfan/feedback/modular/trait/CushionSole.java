// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;


import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * Cushion sole (armour, band trait): a thick soft sole takes the landing, so fall damage is
 * {@code fall_mult} of what it would be. The {@code stealth} param is carried in data and read by nothing yet.
 * Worn: counts once.
 */
public final class CushionSole implements ModularTrait {

    @Override
    public void onWornFall(LivingFallEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        event.setDamageMultiplier(event.getDamageMultiplier() * params.get("fall_mult", 0.8f));
    }
}
