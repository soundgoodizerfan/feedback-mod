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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.vehicle.transmission;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** The design lines a Transmission Blueprint and a Transmission both show, read off the material system. */
public final class TransmissionItems {

    private TransmissionItems() {
    }

    static void tooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltip) {
        TransmissionDesign design = stack.get(FVehicles.TRANSMISSION_DESIGN.get());
        if (design == null)
            return;
        var materials = context.level() == null ? java.util.Map.<net.minecraft.resources.ResourceLocation, io.github.soundgoodizerfan.feedback.process.Material>of()
                : ClientMaterials.on(context.level());
        design.describe(materials.get(design.material())).forEach(line -> tooltip.add(line.copy().withStyle(ChatFormatting.GRAY)));
    }

    /** A transmission on paper, printed by the Car Design Stand; crafted with its ingots into a Transmission, and kept. */
    public static class Blueprint extends Item {
        public Blueprint(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip(stack, context, tooltip);
        }
    }

    /** A Transmission block in hand, carrying its design. */
    public static class Carried extends BlockItem {
        public Carried(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip(stack, context, tooltip);
        }
    }
}
