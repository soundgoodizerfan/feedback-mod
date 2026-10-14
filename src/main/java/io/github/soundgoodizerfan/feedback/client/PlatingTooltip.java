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
import io.github.soundgoodizerfan.feedback.process.Plating;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * A coat's metal and how it came out, in words. A coat is visible -- anybody can see a piece is
 * copper-plated and whether the copper is bright, patchy or burnt -- so this is a free sense
 * (philosophy 8). Its thickness is a figure, and nothing reads it yet.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public class PlatingTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Plating plating = event.getItemStack().get(FDataComponents.PLATING.get());
        if (plating == null)
            return;
        event.getToolTip().add(Component.translatable("plating.feedback." + plating.quality().getSerializedName(),
                Component.translatable("material." + plating.metal().getNamespace() + "." + plating.metal().getPath()))
                .withStyle(ChatFormatting.GRAY));
    }
}
