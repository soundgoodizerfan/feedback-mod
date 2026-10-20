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

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Tints each layer of a modular item by the material of the part it draws: layer n is the type's nth role. */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModularClient {

    private ModularClient() {
    }

    @SubscribeEvent
    public static void registerColors(RegisterColorHandlersEvent.Item event) {
        event.register(ModularClient::tint, FModular.items().toArray(net.minecraft.world.item.Item[]::new));
    }

    private static int tint(ItemStack stack, int layer) {
        ModularDesign design = stack.getItem() instanceof io.github.soundgoodizerfan.feedback.modular.Modular
                ? ModularStacks.design(stack) : null; // a blueprint carries a design and is paper, not parts
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
        if (type == null)
            return 0xFFFFFFFF;
        List<String> order = type.order();
        ModularDesign.Part part = layer < order.size() ? design.part(order.get(layer)) : null;
        return part == null ? 0xFFFFFFFF : 0xFF000000 | MaterialColors.of(part.material());
    }
}
