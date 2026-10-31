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
import io.github.soundgoodizerfan.feedback.core.xp.XpStore;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * What a stack holding experience feels like in the hand: a level word and a fill word, never a figure
 * ({@link Readout#xpLevel}, {@link Readout#xpFill}; the numbers are an instrument's).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public class XpTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        XpStore s = event.getItemStack().get(FDataComponents.XP_STORE.get());
        if (s == null)
            return;
        if (Readout.instrumented(Quantity.XP_LEVEL)) {
            event.getToolTip().add(Readout.xpFigures(s.level(), s.points(), s.maxPoints()));
            return;
        }
        event.getToolTip().add(s.points() == 0 ? Readout.xpFill(s)
                : Component.translatable("tooltip.feedback.xp.held", Readout.xpLevel(s), Readout.xpFill(s)).withStyle(ChatFormatting.GRAY));
    }
}
