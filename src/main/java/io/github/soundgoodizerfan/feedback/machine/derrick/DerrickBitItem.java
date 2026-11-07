// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.derrick;

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
 * A boring bit: the part of the derrick that meets the rock. What it is made of ({@code MADE_OF})
 * sets its hardness, so the gate is the material's, not a tier; wear rides on it as {@code
 * TOOL_CONDITION}, exactly as the lathe's bit does.
 */
public class DerrickBitItem extends Item {

    public DerrickBitItem(Properties properties) {
        super(properties);
    }

    /** The bit's hardness: its material's, or steel's if nothing is stamped. */
    public static float hardness(ItemStack bit) {
        MadeOf made = bit.get(FDataComponents.MADE_OF.get());
        Material m = made == null ? null : MaterialTable.get().materials().get(made.material());
        return m == null ? FTuning.DERRICK_BIT_DEFAULT_HARDNESS : m.hardness().value();
    }

    /** The hottest the bit may run before it wears: its material's service temperature, or a default. */
    public static float serviceTemperature(ItemStack bit) {
        MadeOf made = bit.get(FDataComponents.MADE_OF.get());
        Material m = made == null ? null : MaterialTable.get().materials().get(made.material());
        return m == null ? FTuning.MAGMA_BIT_SERVICE_DEFAULT_TU
                : m.thermal().serviceTemperature().map(t -> t.value()).orElse(FTuning.MAGMA_BIT_SERVICE_DEFAULT_TU);
    }

    public static float condition(ItemStack bit) {
        return bit.getOrDefault(FDataComponents.TOOL_CONDITION.get(), 1f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made != null)
            tooltip.add(Component.translatable("material." + made.material().getNamespace() + "." + made.material().getPath()));
        tooltip.add(Component.translatable("tooltip.feedback.derrick_bit", Math.round(hardness(stack)),
                Math.round(condition(stack) * 100f)));
    }
}
