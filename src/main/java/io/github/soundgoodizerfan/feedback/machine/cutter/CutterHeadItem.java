// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.cutter;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The cutting element of a Power Cutter. What it is made of ({@code MADE_OF}) sets the hardness of stock it can take; wear rides on it as
 * {@code TOOL_CONDITION}, exactly as the derrick bit's does. A saw head cuts cold stock (what the hand stonecutter cuts); a plasma
 * head cuts conducting metal plate.
 */
public class CutterHeadItem extends Item {

    public enum Type { SAW, PLASMA }

    private final Type type;

    public CutterHeadItem(Type type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public Type type() {
        return type;
    }

    /** The head's material, or null if it carries none. */
    public static Material material(ItemStack head) {
        MadeOf made = head.get(FDataComponents.MADE_OF.get());
        return made == null ? null : MaterialTable.get().materials().get(made.material());
    }

    /** The hardest stock the head can take: its material's hardness. */
    public static float hardness(ItemStack head) {
        Material m = material(head);
        return m == null ? FTuning.CUTTER_DEFAULT_HEAD_HARDNESS : m.hardness().value();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made != null)
            tooltip.add(Component.translatable("material." + made.material().getNamespace() + "." + made.material().getPath()));
        tooltip.add(Component.translatable("tooltip.feedback.cutter_head", Math.round(hardness(stack)),
                Math.round(stack.getOrDefault(FDataComponents.TOOL_CONDITION.get(), 1f) * 100f)));
    }
}
