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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.cable.CableBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FormItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Every material in a form is one greyscale sprite per form ({@code textures/item/form/<form>.png})
 * tinted by {@link MaterialColors}: generated forms, storage blocks, and the hand-registered items a
 * material's {@code forms} names (copper plate, steel ingot). Vanilla's own items (iron ingot) keep
 * vanilla's sprite; their models are not ours to change.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FormTint {

    /** The forms that have a sprite under {@code textures/item/form/}. A new form type needs a file and an entry here. */
    private static final Set<String> SPRITE_FORMS = Set.of("ingot", "ground_ingot", "nugget", "bullet", "case",
            "plate", "grip", "rod", "turned_rod", "wire", "fine_wire", "foil", "scrap", "swarf", "drilled_plate");

    private FormTint() {
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        Map<Item, Integer> colors = new HashMap<>();
        for (FormItems.Generated g : FormItems.all())
            colors.put(g.item().get(), 0xFF000000 | MaterialColors.of(g.material()));
        FormItems.materials().forEach((id, material) -> material.forms().forEach((form, item) -> {
            if (SPRITE_FORMS.contains(form) && item.getNamespace().equals(Feedback.MOD_ID))
                BuiltInRegistries.ITEM.getOptional(item)
                        .ifPresent(i -> colors.put(i, 0xFF000000 | MaterialColors.of(id)));
        }));
        colors.forEach((item, color) -> event.register((stack, layer) -> layer == 0 ? color : 0xFFFFFFFF, item));
        // A cable's metal rides on the stack, so its tint cannot be fixed per item.
        event.register((stack, layer) -> {
            MadeOf made = stack.get(FDataComponents.MADE_OF.get());
            return layer == 0 && made != null ? 0xFF000000 | MaterialColors.of(made.material()) : 0xFFFFFFFF;
        }, FItems.CABLE.get());
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        for (FormItems.Generated g : FormItems.all())
            if (g.block() != null) {
                int color = 0xFF000000 | MaterialColors.of(g.material());
                event.register((state, level, pos, tintIndex) -> color, (Block) g.block().get());
            }
        event.register((state, level, pos, tintIndex) -> level != null && pos != null
                && level.getBlockEntity(pos) instanceof CableBlockEntity cable && cable.material() != null
                ? 0xFF000000 | MaterialColors.of(cable.material()) : 0xFFFFFFFF, FBlocks.CABLE.get());
    }
}
