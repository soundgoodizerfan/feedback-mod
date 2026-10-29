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
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * One line saying what a part of the first-circuit chain is for, picked by the material's form and
 * properties (a substrate, a solder, a rectifying crystal), never by id, so another mod's stuff reads
 * the same. The board and the circuit say their own in their items.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public class ElectronicsTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.is(io.github.soundgoodizerfan.feedback.registry.FTags.CIRCUIT_FITTABLE)) {
            var circuit = io.github.soundgoodizerfan.feedback.process.Circuit.of(stack);
            event.getToolTip().add(circuit.live()
                    ? Component.translatable("tooltip.feedback.instrument.fitted", Math.round(circuit.stepFactor() * 100f)).withStyle(ChatFormatting.AQUA)
                    : Component.translatable("tooltip.feedback.instrument.fittable").withStyle(ChatFormatting.GRAY));
        }
        Material.find(ClientMaterials.get(), event.getItemStack()).ifPresent(at -> {
            Material m = at.material();
            String role = null;
            if (at.form().equals("substrate"))
                role = "substrate";
            else if (at.form().equals("ingot") && m.traits().contains(Material.Trait.SOLDER))
                role = "solder";
            else if (!at.form().equals("crystal_plate") && m.electrical().rectifying().orElse(0f) > 0f)
                role = "rectifier";
            if (role != null)
                event.getToolTip().add(Component.translatable("tooltip.feedback.role." + role).withStyle(ChatFormatting.GRAY));
        });
    }
}
