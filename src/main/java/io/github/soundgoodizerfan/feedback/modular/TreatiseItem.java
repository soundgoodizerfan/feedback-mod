// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A written body of knowledge of one {@link School}. Holding it (in the inventory, in a {@link TreatiseBinderItem}, or in a shelf beside
 * the Blueprint Table) lets the table draw blueprints of that school's types. It is a tool you keep, never used up; anyone holding it can draw.
 */
public class TreatiseItem extends Item {

    private final School school;

    public TreatiseItem(School school, Properties properties) {
        super(properties);
        this.school = school;
    }

    public School school() {
        return school;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.feedback.treatise").withStyle(ChatFormatting.GRAY));
    }
}
