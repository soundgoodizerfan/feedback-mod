// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
