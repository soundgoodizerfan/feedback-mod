// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.List;

import io.github.soundgoodizerfan.feedback.process.Board;
import io.github.soundgoodizerfan.feedback.process.Circuit;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Basic Electronic Circuit. Everything about it is the {@link Circuit}
 * component; the item only carries it and shows it. The tooltip says only how many elements it
 * has; reading its quality takes a component tester, as for any part. A carried
 * circuit that gets hotter than its traces take lifts them (dead part).
 */
public class CircuitItem extends Item {

    public CircuitItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide)
            Circuit.scorch(stack, level);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Circuit c = Circuit.of(stack);
        tooltip.add(c.live() ? Component.translatable("feedback.circuit.elements", c.elements())
                : Component.translatable("feedback.circuit.dead"));
        long burnt = c.board().map(b -> b.parts().stream().filter(Board.Part::dead).count()).orElse(0L);
        if (c.live() && burnt > 0)
            tooltip.add(Component.translatable("tooltip.feedback.circuit.burnt", burnt).withStyle(net.minecraft.ChatFormatting.GRAY));
        if (c.live() && c.stability() > 0f)
            tooltip.add(Component.translatable("tooltip.feedback.circuit.has_resonator").withStyle(net.minecraft.ChatFormatting.GRAY));
        if (c.live() && c.memory() > 0)
            tooltip.add(Component.translatable(c.line().holds() ? "tooltip.feedback.circuit.has_lattice_holds" : "tooltip.feedback.circuit.has_lattice").withStyle(net.minecraft.ChatFormatting.GRAY));
        if (c.live() && c.tubed()) {
            // A tubed circuit says how warm its bulb is: cold it is slow and noisy, warm it works at its best.
            net.minecraft.world.level.Level level = context.level();
            float w = level == null ? 0f : c.warmth(stack, level);
            String word = w >= 0.95f ? "warm" : w >= 0.4f ? "warming" : "cold";
            tooltip.add(io.github.soundgoodizerfan.feedback.client.tooltip.Readout.gloss(Component.translatable("tooltip.feedback.circuit." + word),
                    "tooltip.feedback.circuit.gloss." + word, Math.round(0.95f * 100f), Math.round(0.4f * 100f)).copy().withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.feedback.role.circuit").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
