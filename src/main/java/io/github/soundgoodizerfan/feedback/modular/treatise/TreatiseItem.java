// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.treatise;

import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;

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
        // How well its specimen was read: good notes help whoever assembles with the volume held (never a gate).
        Float notes = stack.get(io.github.soundgoodizerfan.feedback.registry.FModular.NOTES.get());
        if (notes != null)
            tooltip.add(Component.translatable("tooltip.feedback.treatise_notes." + (notes < 0.3f ? 0 : notes < 0.55f ? 1 : notes < 0.78f ? 2 : notes < 0.92f ? 3 : 4))
                    .withStyle(ChatFormatting.DARK_GREEN));
        // What it lets you draw, read from the loaded type data: every type of its volume (a hafted head kind is a type too), and each
        // addon role that names the volume (a servo on armour), never a list kept here.
        for (var e : ModularTypes.known().entrySet()) {
            ModularType type = e.getValue();
            String path = e.getKey().getPath();
            if (school == School.of(type.school()) && volume == School.volumeOf(type.school()))
                tooltip.add(Component.literal("- ").append(Component.translatableWithFallback("item." + e.getKey().getNamespace() + "." + path, path.replace('_', ' ')))
                        .withStyle(ChatFormatting.DARK_AQUA));
            for (ModularType.Role role : type.roles().values()) {
                if (role.part().isPresent())
                    for (ModularType.Variant v : role.part().get().variants())
                        if (!v.school().isEmpty() && school == School.of(v.school()) && volume == School.volumeOf(v.school()))
                            tooltip.add(Component.literal("- ").append(Component.translatableWithFallback("modular.feedback.archetype." + v.name(), v.name().replace('_', ' ')))
                                    .append(Component.literal(" (")).append(Component.translatableWithFallback("item." + e.getKey().getNamespace() + "." + path, path.replace('_', ' ')))
                                    .append(Component.literal(")")).withStyle(ChatFormatting.DARK_AQUA));
                if (!role.school().isEmpty() && school == School.of(role.school()) && volume == School.volumeOf(role.school()))
                    tooltip.add(Component.literal("- ").append(Component.translatableWithFallback("modular.feedback.role." + role.name(), role.name()))
                            .append(Component.literal(" (")).append(Component.translatableWithFallback("item." + e.getKey().getNamespace() + "." + path, path.replace('_', ' ')))
                            .append(Component.literal(")")).withStyle(ChatFormatting.DARK_AQUA));
            }
        }
    }
}
