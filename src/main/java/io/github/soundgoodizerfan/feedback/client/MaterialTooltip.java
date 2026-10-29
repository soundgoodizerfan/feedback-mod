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
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The few general figures of an item's material that a player acts on every day: how hard it is, what it
 * melts at, what it burns at. Held back until Shift so a plain tooltip stays short. The full sheet is still
 * the Material Probe's; this is only what the author's playtest could not find without it.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public class MaterialTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!net.minecraft.client.gui.screens.Screen.hasShiftDown())
            return;
        Material.find(ClientMaterials.get(), event.getItemStack()).ifPresent(at -> {
            Material m = at.material();
            var tip = event.getToolTip();
            if (m.hardness().value() > 0)
                tip.add(line("tooltip.feedback.material.hardness", "hardness", m.hardness().value(), ""));
            m.thermal().meltingPoint().ifPresent(t -> tip.add(line("tooltip.feedback.material.melts", "melting_point", t.value(), "Tu")));
            m.combustion().flatMap(c -> c.flameTemperature())
                    .ifPresent(t -> tip.add(line("tooltip.feedback.material.burns", "flame", t.value(), "Tu")));
        });
    }

    private static Component line(String key, String stat, float value, String unit) {
        Component line = Component.translatable(key, Readout.figure(Quantity.FIGURE, value)).withStyle(ChatFormatting.GRAY);
        Component band = Readout.rating(stat, value, unit);
        return band == null ? line : line.copy().append(Component.literal(" (")).append(band).append(Component.literal(")"));
    }
}
