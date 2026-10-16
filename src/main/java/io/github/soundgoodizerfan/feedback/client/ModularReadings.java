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
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The figures behind a modular item's adjectives, for a player who carries the instrument that
 * reads them (spec 6): mass from a balance scale, an edge's hardness as a rung of a file set's
 * ladder. What was made is what costs an instrument; the blueprint's target is free paper.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class ModularReadings {

    private ModularReadings() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ModularFigures f = ModularStacks.figures(stack);
        if (f == null)
            return;
        if (Readout.instrumented(Quantity.MASS))
            event.getToolTip().add(Readout.reading(Quantity.MASS, "feedback.readout.mass",
                    String.format("%.1f", Instruments.quantise(f.mass().value(), ModularTuning.BALANCE_RESOLUTION))));
        if (Readout.instrumented(Quantity.HARDNESS))
            event.getToolTip().add(Readout.reading(Quantity.HARDNESS, "feedback.readout.hardness", ladder(f.get(ModularFigures.Fig.HARDNESS))));
    }

    /** Which two files the edge falls between: the hardest it is not scratched by, and the softest that scratches it. */
    private static Component ladder(float hardness) {
        float[] files = ModularTuning.SCRATCH_FILES;
        if (hardness < files[0])
            return Component.translatable("feedback.readout.hardness.softer", Integer.toString(1));
        for (int i = files.length - 1; i >= 0; i--)
            if (hardness >= files[i])
                return i == files.length - 1
                        ? Component.translatable("feedback.readout.hardness.harder", i + 1)
                        : Component.translatable("feedback.readout.hardness.between", i + 1, i + 2);
        return Component.empty();
    }
}
