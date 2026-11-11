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
 * One numbered volume of a {@link School}'s treatise. Holding it (in the inventory, in a {@link TreatiseBinderItem}, or in a shelf beside
 * the Blueprint Table) lets the table draw blueprints of that school's types. It is a tool you keep, never used up; anyone holding it can draw.
 */
public class TreatiseItem extends Item {

    private final School school;
    private final int volume;

    public TreatiseItem(School school, int volume, Properties properties) {
        super(properties);
        this.school = school;
        this.volume = volume;
    }

    /** Which volume of the school's treatise this is, from 1. */
    public int volume() {
        return volume;
    }

    public School school() {
        return school;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.feedback.treatise").withStyle(ChatFormatting.GRAY));
        // what it lets you draw: every type of its volume, by name
        for (var e : ModularTypes.known().entrySet())
            if (school == School.of(e.getValue().school()) && volume == School.volumeOf(e.getValue().school()))
                tooltip.add(Component.literal("- ").append(Component.translatable("item." + e.getKey().getNamespace() + "." + e.getKey().getPath()))
                        .withStyle(ChatFormatting.DARK_AQUA));
    }
}
