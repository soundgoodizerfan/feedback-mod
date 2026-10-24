/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.item.FormItem;
import io.github.soundgoodizerfan.feedback.item.MassProbeItem;
import io.github.soundgoodizerfan.feedback.process.Cut;
import io.github.soundgoodizerfan.feedback.process.Plating;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * A coat's metal and how it came out, in words. A coat is visible -- anybody can see a piece is
 * copper-plated and whether the copper is bright, patchy or burnt -- so this is a free sense
 * (philosophy 8). Its thickness is a figure no instrument reads yet, so only the truth tier shows it.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public class PlatingTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Plating plating = event.getItemStack().get(FDataComponents.PLATING.get());
        net.minecraft.world.item.ItemStack stack = event.getItemStack();
        // The cut is plain to see on a plate, and what it reads is printed on it: a free sense.
        if (stack.getItem() instanceof MassProbeItem probe || stack.getItem() instanceof FormItem plate && plate.form().equals("crystal_plate")) {
            Cut cut = Cut.of(stack);
            event.getToolTip().add(Readout.gloss(Component.translatable(cut.translationKey()).withStyle(ChatFormatting.GRAY),
                    "feedback.gloss.cut", Instruments.sig(cut.range, 3), Instruments.sig(cut.step, 3)));
            Component deposit = deposit(stack, plating);
            if (deposit != null)
                event.getToolTip().add(deposit);
        }
        if (plating == null)
            return;
        String quality = plating.quality().getSerializedName();
        event.getToolTip().add(Readout.gloss(Component.translatable("plating.feedback." + quality,
                Component.translatable("material." + plating.metal().getNamespace() + "." + plating.metal().getPath()))
                .withStyle(ChatFormatting.GRAY), "feedback.gloss.plating." + quality,
                Readout.number(FTuning.PLATING_POROUS_BELOW_QU), Readout.number(FTuning.PLATING_SPALLS_ABOVE_QU),
                Readout.number(FTuning.ELECTROLYTIC_PURE_FRACTION * 100), Readout.number(FTuning.PLATING_BURNT_SHARE * 100)));
        Component thickness = thickness(plating);
        if (thickness != null)
            event.getToolTip().add(thickness);
    }

    /** What a held mass probe shows for its own coat, or null for any other stack. Shared with {@link ItemInfoHud}. */
    static Component deposit(net.minecraft.world.item.ItemStack stack, Plating plating) {
        if (!(stack.getItem() instanceof MassProbeItem p))
            return null;
        // Seeded by the coat, so looking again does not average the noise away.
        float coat = plating == null ? 0f : plating.thickness();
        return p.signed(stack, p.read(stack, coat, RandomSource.create(Float.floatToIntBits(coat))));
    }

    /** A coat's thickness, truth tier only (no instrument reads it yet), else null. Shared with {@link ItemInfoHud}. */
    static Component thickness(Plating plating) {
        return plating != null && Readout.exact(Quantity.FIGURE)
                ? Readout.reading(Quantity.FIGURE, "feedback.readout.plating", Readout.figure(Quantity.FIGURE, plating.thickness()))
                : null;
    }
}
