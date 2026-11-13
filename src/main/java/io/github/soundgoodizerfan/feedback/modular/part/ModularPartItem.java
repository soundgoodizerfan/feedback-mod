// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.part;

import io.github.soundgoodizerfan.feedback.item.FormBlock;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.modular.attunement.Attunements;
import io.github.soundgoodizerfan.feedback.modular.bench.ForgeRhythm;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One kind of shaped part -- a pickaxe head, a sword blade, a helmet's plate -- in any material:
 * the material rides on the stack ({@link MadeOf}), so the name is a pattern over the material's
 * ({@code form.feedback.pickaxe_head} = {@code %s Pickaxe Head}) and the sprite is tinted by it.
 * No behaviour: what it does is decided when it is set into a tool.
 */
public class ModularPartItem extends Item {

    public ModularPartItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made == null)
            return super.getName(stack);
        Component name = made.variant().isEmpty()
                ? FormBlock.name(made.material(), BuiltInRegistries.ITEM.getKey(this).getPath())
                : Component.translatable("modular.feedback.variant_of",
                        Component.translatable("material." + made.material().getNamespace() + "." + made.material().getPath()),
                        Component.translatable("modular.feedback.variant." + made.variant()));
        if (made.flaw() > 0)
            name = Component.translatable("modular.feedback.flaw." + made.flaw(), name);
        return name;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made != null && made.flaw() > 0)
            tooltip.add(Component.translatable("tooltip.feedback.part.flaw", made.flaw(), ForgeRhythm.MAX_FLAW)
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        if (made != null)
            made.measures().slots().forEach((name, slot) -> tooltip.add(Component.translatable("tooltip.feedback.part.slot",
                    Component.translatable("modular.feedback.slot." + name),
                    Component.translatable("material." + slot.material().getNamespace() + "." + slot.material().getPath()))
                    .withStyle(net.minecraft.ChatFormatting.GRAY)));
        if (made != null)
            attuned(made, tooltip);
        if (made != null && made.measures().work() != 1f
                && io.github.soundgoodizerfan.feedback.client.tooltip.Readout.exact(io.github.soundgoodizerfan.feedback.instrument.Quantity.FIGURE))
            tooltip.add(Component.translatable("tooltip.feedback.part.worked",
                    io.github.soundgoodizerfan.feedback.client.tooltip.Readout.figure(
                            io.github.soundgoodizerfan.feedback.instrument.Quantity.FIGURE, made.measures().work() * 100f))
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    /** The attunement lines of a part, shaped or raw stock the enchanting table stamped. */
    public static void attuned(MadeOf made, java.util.List<Component> tooltip) {
        made.measures().attuned().forEach((key, level) -> {
            Attunements.Effect effect = Attunements.byKey(key);
            if (effect != null)
                tooltip.add(Component.translatable("tooltip.feedback.part.attuned",
                        Component.translatable("attunement.feedback." + effect.id().getPath() + (level < 0f ? ".mirror" : "")),
                        Math.abs(Math.round(level)))
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        });
    }
}
