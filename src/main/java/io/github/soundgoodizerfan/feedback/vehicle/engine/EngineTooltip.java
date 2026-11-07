// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.engine;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** The design lines a Blueprint and an Engine both show, read off the material system. */
final class EngineTooltip {

    private EngineTooltip() {
    }

    static void add(ItemStack stack, Item.TooltipContext context, List<Component> tooltip) {
        EngineDesign design = stack.get(FVehicles.ENGINE_DESIGN.get());
        if (design == null)
            return;
        var materials = context.level() == null ? java.util.Map.<net.minecraft.resources.ResourceLocation, io.github.soundgoodizerfan.feedback.process.Material>of()
                : ClientMaterials.on(context.level());
        design.describe(materials.get(design.material())).forEach(line -> tooltip.add(line.copy().withStyle(ChatFormatting.GRAY)));
    }
}
