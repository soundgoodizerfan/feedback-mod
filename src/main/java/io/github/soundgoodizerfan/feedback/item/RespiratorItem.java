// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * A mask with a charcoal filter, worn in the head slot. It cuts the dose of any fume or fume cloud
 * ({@code Skills.fumeShare}); it does not give air, so it does nothing against a place with none. It never wears
 * out: wear comes from misuse, not from being worn.
 */
public class RespiratorItem extends Item {

    public RespiratorItem(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.HEAD;
    }

    /** Whether {@code who} has one on. */
    public static boolean wearing(LivingEntity who) {
        return who.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof RespiratorItem;
    }
}
