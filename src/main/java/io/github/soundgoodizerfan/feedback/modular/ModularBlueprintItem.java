// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A modular design on paper, printed at the Blueprint Table: the design and the figures it bakes to, as drawn. A blueprint <i>predicts</i> -- it
 * gives the figures every part would make if it came out exactly to drawing, for free, because it
 * is paper. What an item actually has is measured on the item, with an instrument.
 *
 * <p>A type that says {@code blueprint} (the crossbow) takes one of its own in the Tool Table's plan slot to
 * make it; the table keeps it, as the gun's craft does. Everything else is its parts and needs none.
 */
public class ModularBlueprintItem extends Item {

    public ModularBlueprintItem(Properties properties) {
        super(properties);
    }

    /** A blueprint that holds food is a recipe, in every word the player reads. */
    @Override
    public Component getName(ItemStack stack) {
        ModularDesign design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.known(design.type());
        return type != null && type.family() == ModularType.Family.DISH ? Component.translatable("item.feedback.recipe") : super.getName(stack);
    }

    /** What the tooltip draws turning: the design, as it would look built. */
    public record Preview(ModularDesign design) implements net.minecraft.world.inventory.tooltip.TooltipComponent {
    }

    @Override
    public java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(ItemStack stack) {
        ModularDesign design = ModularStacks.design(stack);
        return design == null ? java.util.Optional.empty() : java.util.Optional.of(new Preview(design));
    }

    /** Using a blueprint opens its recipe card: what to make, read off the design the sheet carries. */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
                                                                       net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ModularStacks.design(stack) == null)
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        if (level.isClientSide)
            io.github.soundgoodizerfan.feedback.client.BlueprintCardScreen.open(stack);
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModularDesign design = ModularStacks.design(stack);
        if (design == null)
            return;
        ModularType named = ModularTypes.known(design.type());
        Component typeName = Component.translatable("item." + design.type().getNamespace() + "." + design.type().getPath());
        tooltip.add((named != null && named.family() == ModularType.Family.HAFTED ? ModularStacks.haftedName(design, named, typeName) : typeName)
                .copy().withStyle(ChatFormatting.WHITE));
        BlueprintCredit credit = BlueprintCredit.of(stack);
        if (credit != null && credit.titleLine() != null)
            tooltip.add(credit.titleLine());
        if (credit != null)
            tooltip.add(credit.line());
        ModularType type = ModularTypes.known(design.type());
        if (type != null)
            tooltip.add(ModularTooltip.parts(design, type).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.feedback.modular.edge." + design.edge().getSerializedName())
                .withStyle(ChatFormatting.GRAY));
        ModularFigures figures = ModularStacks.figures(stack);
        if (figures != null) {
            net.minecraft.network.chat.MutableComponent mods = ModularModifiers.line(ModularModifiers.of(design, type, figures));
            if (mods != null)
                tooltip.add(mods);
        }
        if (figures != null)
            figures(figures).forEach(line -> tooltip.add(line.copy().withStyle(ChatFormatting.DARK_AQUA)));
    }

    /** One line per figure the design has: a figure a type does not use bakes to zero and is left out. */
    public static List<Component> figures(ModularFigures figures) {
        return figures(figures, false);
    }

    /** The figures {@link #figures} has a line for, in the same order. */
    public static List<Fig> present(ModularFigures figures) {
        return java.util.Arrays.stream(Fig.values()).filter(fig -> figures.get(fig) != 0).toList();
    }

    /** What a figure is: its full label, then what it does to the item. */
    public static List<Component> explain(Fig fig) {
        String key = "modular.feedback.fig." + fig.name().toLowerCase(Locale.ROOT);
        return List.of(Component.translatable(key), Component.translatable(key + ".tip").withStyle(ChatFormatting.GRAY));
    }

    /** As {@link #figures(ModularFigures)}; {@code brief} cuts each label at its comma (the unit) to fit a column. */
    public static List<Component> figures(ModularFigures figures, boolean brief) {
        List<Component> out = new ArrayList<>();
        for (Fig fig : Fig.values()) {
            float v = figures.get(fig);
            if (v == 0)
                continue;
            Component label = Component.translatable("modular.feedback.fig." + fig.name().toLowerCase(Locale.ROOT));
            if (brief)
                label = Component.literal(label.getString().split(",", 2)[0]);
            out.add(Component.empty().append(label).append(": " + String.format(Locale.ROOT,
                    v >= 100 ? "%.0f" : v >= 10 ? "%.1f" : "%.2f", v)));
        }
        return out;
    }
}
