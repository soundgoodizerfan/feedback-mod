// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.cable;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.item.FormBlock;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * A length of cable in some metal: the material rides on the stack ({@link MadeOf}) and passes to
 * the block entity when it is placed (with the {@code INSULATION} band it was crafted with, if any), so the name is a pattern over the material's
 * ({@code form.feedback.cable} = {@code %s Cable}) and the sprite is tinted by it.
 */
public class CableItem extends BlockItem {

    public CableItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** {@code count} bare cable of {@code material}. */
    public static ItemStack of(ResourceLocation material, int count) {
        return of(material, count, null);
    }

    /** {@code count} cable of {@code material} coated to hold off {@code insulation}, or bare when null. */
    public static ItemStack of(ResourceLocation material, int count, @Nullable VoltageBand insulation) {
        return of(material, count, insulation, CableGauge.STANDARD);
    }

    /** {@code count} cable of {@code material} and {@code gauge}; standard carries no component, so it stacks with every plain cable. */
    public static ItemStack of(ResourceLocation material, int count, @Nullable VoltageBand insulation, CableGauge gauge) {
        ItemStack stack = new ItemStack(io.github.soundgoodizerfan.feedback.registry.FItems.CABLE.get(), count);
        stack.set(FDataComponents.MADE_OF.get(), new MadeOf(material, "wire", 1));
        if (insulation != null)
            stack.set(FDataComponents.INSULATION.get(), insulation);
        if (gauge != CableGauge.STANDARD)
            stack.set(FDataComponents.CABLE_GAUGE.get(), gauge);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CableGauge gauge = stack.get(FDataComponents.CABLE_GAUGE.get());
        if (gauge != null)
            tooltip.add(Component.translatable("feedback.tooltip.cable.gauge", Component.translatable(gauge.key()),
                    Component.literal(String.valueOf(gauge.area()))).withStyle(ChatFormatting.GRAY));
        if (getBlock() instanceof FenceBlock)
            return;
        VoltageBand band = stack.get(FDataComponents.INSULATION.get());
        tooltip.add(band == null ? Component.translatable("feedback.tooltip.cable.bare").withStyle(ChatFormatting.GRAY)
                : Component.translatable("feedback.tooltip.cable.insulated", Component.translatable(band.key()))
                        .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public Component getName(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made == null)
            return super.getName(stack);
        boolean bus = stack.get(FDataComponents.CABLE_GAUGE.get()) == CableGauge.BUS;
        return FormBlock.name(made.material(), getBlock() instanceof FenceBlock ? "fence" : bus ? "bus_bar" : "cable");
    }
}
