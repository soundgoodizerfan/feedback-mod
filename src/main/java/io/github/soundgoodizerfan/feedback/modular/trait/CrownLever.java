// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;


import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

/**
 * Crown lever (armour, band trait): a tall or heavy crown is a lever on the neck, so knockback does
 * {@code knock_mult} of its strength to the wearer. Only a piece in the head slot works it. Worn: counts once.
 */
public final class CrownLever implements ModularTrait {

    @Override
    public void onWornKnockback(LivingKnockBackEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        if (piece.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.HEAD)
            event.setStrength(event.getStrength() * params.get("knock_mult", 1.3f));
    }
}
