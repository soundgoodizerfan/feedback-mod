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

/** Tints each layer of a modular item by the material of the part it draws (layer n is the type's nth role), and a shaped part by its own. */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModularClient {

    private ModularClient() {
    }

    @SubscribeEvent
    public static void registerColors(RegisterColorHandlersEvent.Item event) {
        event.register(ModularClient::tint, java.util.stream.Stream.concat(FModular.items().stream(),
                java.util.stream.Stream.of((net.minecraft.world.item.Item) io.github.soundgoodizerfan.feedback.registry.FWeapons.GUN.get()))
                .toArray(net.minecraft.world.item.Item[]::new));
        event.register((stack, layer) -> {
            io.github.soundgoodizerfan.feedback.process.MadeOf made =
                    stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get());
            return made == null || layer != 0 ? 0xFFFFFFFF : 0xFF000000 | MaterialColors.of(made.material());
        }, FModular.PARTS.stream().map(p -> (net.minecraft.world.item.Item) p.get()).toArray(net.minecraft.world.item.Item[]::new));
    }

    /** A dud draught is tinted by the class it leans toward; a real potion keeps its look. */
    @SubscribeEvent
    public static void registerPotionColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> {
            var state = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.POTION_STATE.get());
            return state != null && state.dud() && state.tint() >= 0 && layer == 0 ? 0xFF000000 | state.tint() : 0xFFFFFFFF;
        }, io.github.soundgoodizerfan.feedback.registry.FItems.POTION.get());
    }

    @SubscribeEvent
    public static void registerExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new SpearClient.Extension(), FModular.SPEAR.get());
    }

    /**
     * {@code feedback:variant}: 1 + the index of the first layer's part shape among its role's variants, 0 for the
     * standard shape. Models read it as a threshold, so the highest listed override at or under it wins.
     */
    @SubscribeEvent
    public static void registerProperties(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> FModular.items().forEach(item -> net.minecraft.client.renderer.item.ItemProperties.register(
                item, Feedback.id("variant"), (stack, level, entity, seed) -> variant(stack))));
    }

    private static float variant(ItemStack stack) {
        ModularDesign design = stack.getItem() instanceof io.github.soundgoodizerfan.feedback.modular.Modular
                ? ModularStacks.design(stack) : null;
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
        if (type == null)
            return 0;
        String role = type.order().get(0);
        ModularDesign.Part part = design.part(role);
        if (part == null || part.variant().isEmpty())
            return 0;
        List<ModularType.Variant> variants = type.roles().get(role).part().map(ModularType.Shaping::variants).orElse(List.of());
        for (int i = 0; i < variants.size(); i++)
            if (variants.get(i).name().equals(part.variant()))
                return i + 1;
        return 0;
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
