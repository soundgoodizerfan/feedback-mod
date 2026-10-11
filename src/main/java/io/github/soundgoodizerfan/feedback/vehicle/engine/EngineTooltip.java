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
package io.github.soundgoodizerfan.feedback.vehicle.engine;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** The design lines a Blueprint and an Engine both show, read off the material system. */
final class EngineTooltip {

    private EngineTooltip() {
    }

    static void add(ItemStack stack, Item.TooltipContext context, List<Component> tooltip) {
        EngineDesign design = stack.get(FVehicles.ENGINE_DESIGN.get());
        if (design == null)
            return;
        var materials = context.level() == null ? java.util.Map.<net.minecraft.resources.ResourceLocation, io.github.soundgoodizerfan.feedback.process.Material>of()
                : ClientMaterials.on(context.level());
        design.describe(materials.get(design.material())).forEach(line -> tooltip.add(line.copy().withStyle(ChatFormatting.GRAY)));
    }
}
