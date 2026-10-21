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

import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Says what a workpiece needs, and describes how far along it is.
 *
 * <h2>The two halves are not the same kind of fact</h2>
 * Philosophy 8, <em>what you need is free, what you have is a cost</em>, cuts straight through the
 * middle of this tooltip. {@code Needs 14 Fu} is published data -- a handbook figure, true before
 * the player owns anything, and stating it gates nothing. {@code Has 9 Fu} is the state of the
 * object in your hand, and that is what calipers are for.
 * <p>
 * So the requirement prints as a figure and the progress prints as a word, on adjacent lines. That
 * juxtaposition is the lesson: the player can see precisely what they are aiming at and only
 * roughly where they are, which is the entire shape of the mod in two lines of tooltip.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public class WorkpieceTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        // A machined barrel's shape is state, so a figure is the calipers' to give (philosophy 8).
        var made = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get());
        var barrel = made != null && made.dims().containsKey(io.github.soundgoodizerfan.feedback.weapon.BarrelGeometry.BORE)
                ? io.github.soundgoodizerfan.feedback.process.Machining.geometry(made) : null;
        if (barrel != null)
            event.getToolTip().add(Readout.instrumented(Quantity.LENGTH)
                    ? Readout.reading(Quantity.LENGTH, "feedback.readout.barrel",
                            Readout.figure(Quantity.LENGTH, barrel.boreMm()),
                            Readout.figure(Quantity.LENGTH, barrel.wallMm()),
                            Readout.exact(Quantity.LENGTH) ? Instruments.format(barrel.lengthM(), Instrument.TRUTH)
                                    : Instruments.format(barrel.lengthM(), 0.01f))
                    : Component.translatable("feedback.workpiece.barrel_unmeasured").withStyle(ChatFormatting.DARK_GRAY));
        int worked = stack.getOrDefault(FDataComponents.WORK.get(), 0);
        int required = stack.getOrDefault(FDataComponents.WORK_REQUIRED.get(), 0);
        if (worked <= 0 || required <= 0)
            return;

        if (Readout.instrumented(Quantity.WORK)) {
            event.getToolTip().add(Readout.reading(Quantity.WORK,
                    "feedback.readout.work", worked, required));
            return;
        }

        event.getToolTip().add(Component.translatable("feedback.workpiece.needs", required)
                .withStyle(ChatFormatting.DARK_GRAY));
        event.getToolTip().add(Readout.progress(worked, required));
    }
}
