// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.arc;

import java.util.List;

import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * An arc furnace electrode: a rod of any material that can carry the arc ({@link Material#electrodeLimit}).
 * What it is made of ({@code MADE_OF}) sets how hot its arc may burn; wear rides on it as {@code TOOL_CONDITION}.
 * It is consumed by nature, like a fuse or a grinding wheel's grit: the hotter and longer the bath it works, the faster it sublimes.
 */
public class ElectrodeItem extends Item {

    public ElectrodeItem(Properties properties) {
        super(properties);
    }

    /** One new electrode of {@code material}. */
    public static ItemStack of(ResourceLocation material) {
        ItemStack stack = new ItemStack(FItems.ARC_ELECTRODE.get());
        stack.set(FDataComponents.MADE_OF.get(), new MadeOf(material, "rod", 1));
        return stack;
    }

    /** The material this electrode is made of, or null if it carries none. */
    public static Material material(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        return made == null ? null : MaterialTable.get().materials().get(made.material());
    }

    /** One new electrode of {@code material} with a tip of {@code tip} (another material, usually a harder one) on it. */
    public static ItemStack tipped(ResourceLocation material, ResourceLocation tip) {
        ItemStack stack = new ItemStack(FItems.ARC_ELECTRODE.get());
        stack.set(FDataComponents.MADE_OF.get(), new MadeOf(material, "rod", 1, tip.toString()));
        return stack;
    }

    /** The material of the tip, or null for a plain electrode. */
    public static Material tip(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made == null || made.variant().isEmpty())
            return null;
        ResourceLocation id = ResourceLocation.tryParse(made.variant());
        return id == null ? null : MaterialTable.get().materials().get(id);
    }

    /**
     * The temperature its arc cannot pass; zero for an electrode that carries no usable material. A tip that can take more than the rod lifts it:
     * the arc stands on the tip, so the rod behind it wears as if it ran cooler.
     */
    public static float limitTu(ItemStack stack) {
        Material material = material(stack);
        float limit = material == null ? 0f : material.electrodeLimit().map(tu -> tu.value()).orElse(0f);
        Material tip = tip(stack);
        return tip == null ? limit : Math.max(limit, tip.electrodeLimit().map(tu -> tu.value()).orElse(0f));
    }

    public static float condition(ItemStack stack) {
        return stack.getOrDefault(FDataComponents.TOOL_CONDITION.get(), 1f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made != null)
            tooltip.add(Component.translatable("material." + made.material().getNamespace() + "." + made.material().getPath()));
        MadeOf tipMade = made != null && !made.variant().isEmpty() ? made : null;
        if (tipMade != null) {
            ResourceLocation tipId = ResourceLocation.tryParse(tipMade.variant());
            if (tipId != null)
                tooltip.add(Component.translatable("tooltip.feedback.arc_electrode.tip",
                        Component.translatable("material." + tipId.getNamespace() + "." + tipId.getPath())));
        }
        tooltip.add(Component.translatable("tooltip.feedback.arc_electrode", Math.round(limitTu(stack)), Math.round(condition(stack) * 100f)));
    }
}
