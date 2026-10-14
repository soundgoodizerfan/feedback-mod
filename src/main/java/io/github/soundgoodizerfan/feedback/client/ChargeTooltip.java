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
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * How charged a jar round is: a word, or a figure with an electroscope. Its volts are a stamp and
 * a tick like a workpiece's heat, so the client leaks them itself with no packet ({@link HeatTooltip}).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public class ChargeTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Cartridge round = event.getItemStack().get(FWeapons.CARTRIDGE.get());
        var level = Minecraft.getInstance().level;
        if (round == null || !round.charged() || level == null)
            return;
        event.getToolTip().add(Component.translatable("cartridge.feedback.jar",
                Readout.chargeReading(round.at(level.getGameTime()).volts())).withStyle(ChatFormatting.GRAY));
    }
}
