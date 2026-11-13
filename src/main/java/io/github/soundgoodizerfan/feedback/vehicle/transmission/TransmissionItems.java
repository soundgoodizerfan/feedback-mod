// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.transmission;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** The design lines a Transmission shows, read off the material system. */
public final class TransmissionItems {

    private TransmissionItems() {
    }

    static void tooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltip) {
        TransmissionDesign design = stack.get(FVehicles.TRANSMISSION_DESIGN.get());
        if (design == null)
            return;
        var materials = context.level() == null ? java.util.Map.<net.minecraft.resources.ResourceLocation, io.github.soundgoodizerfan.feedback.process.Material>of()
                : ClientMaterials.on(context.level());
        design.describe(materials.get(design.material())).forEach(line -> tooltip.add(line.copy().withStyle(ChatFormatting.GRAY)));
    }

    /** A Transmission block in hand: a modular build (the {@code transmission} type) carrying the design it bakes. */
    public static class Carried extends BlockItem implements io.github.soundgoodizerfan.feedback.modular.type.Modular {
        public Carried(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public Component getName(ItemStack stack) {
            return io.github.soundgoodizerfan.feedback.modular.item.ModularStacks.name(stack, super.getName(stack));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            io.github.soundgoodizerfan.feedback.modular.bake.ModularTooltip.add(stack, tooltip);
            tooltip(stack, context, tooltip);
        }
    }
}
