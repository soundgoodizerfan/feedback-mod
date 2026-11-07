// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A batch of mixed material from the Mixer. The parts on it are the whole of what it is; a machine that acts on it asks the mixture table what it resolves into. */
public class MixtureItem extends Item {

    public MixtureItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var solids = stack.get(FWeapons.MIXTURE.get());
        if (solids == null)
            return;
        var parts = new java.util.ArrayList<>(solids);
        for (var fluid : stack.getOrDefault(FWeapons.MIXTURE_FLUID.get(), List.<io.github.soundgoodizerfan.feedback.process.Mixture.FluidPart>of()))
            parts.add(fluid.asCharge());
        if (parts.isEmpty())
            return;
        int total = parts.stream().mapToInt(Cartridge.Charge::count).sum();
        // Rough shares only: a figure here would be a recipe read off a tooltip.
        var sorted = parts.stream().sorted(java.util.Comparator.comparingInt(Cartridge.Charge::count).reversed()).toList();
        var line = Component.empty();
        for (int k = 0; k < sorted.size(); k++) {
            if (k > 0)
                line.append(", ");
            float share = sorted.get(k).count() / (float) total;
            String key = share >= 0.5f ? "mostly" : share >= 0.2f ? "some" : "little";
            line.append(Component.translatable("tooltip.feedback.mixture." + key, CartridgeItem.name(sorted.get(k).material())));
        }
        tooltip.add(line.withStyle(ChatFormatting.GRAY));
    }
}
