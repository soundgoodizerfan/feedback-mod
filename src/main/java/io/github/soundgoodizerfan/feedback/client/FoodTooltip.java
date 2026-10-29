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

import java.util.Locale;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.FoodState;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Says how far a food has been cooked and whether anything live is left in it. The state is synced, so the client reads it itself. */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public class FoodTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        FoodState state = event.getItemStack().get(FDataComponents.FOOD_STATE.get());
        if (Minecraft.getInstance().level == null)
            return;
        var level = Minecraft.getInstance().level;
        // Which part of a dish it can be, read off the dish types' own roles: by its traits, never by what it is called.
        var located = io.github.soundgoodizerfan.feedback.process.Material.find(
                io.github.soundgoodizerfan.feedback.process.ClientMaterials.get(), event.getItemStack().getItem());
        if (located.isPresent()) {
            java.util.Set<String> roles = new java.util.LinkedHashSet<>();
            for (var type : io.github.soundgoodizerfan.feedback.modular.ModularTypes.known().values())
                if (type.family() == io.github.soundgoodizerfan.feedback.modular.ModularType.Family.DISH)
                    for (var role : type.roles().values())
                        if (!role.name().equals("container") && role.accepts(located.get()))
                            roles.add(role.name());
            if (!roles.isEmpty())
                event.getToolTip().add(Component.translatable("tooltip.feedback.food.dish_part", roles.stream()
                        .map(r -> Component.translatable("modular.feedback.role." + r)).reduce((a, b) -> a.copy().append(", ").append(b)).orElse(Component.empty()))
                        .withStyle(ChatFormatting.DARK_GREEN));
        }
        Cooking.findPerishable(level, event.getItemStack()).ifPresent(entry -> {
            if (!entry.food().actives().isEmpty())
                event.getToolTip().add(Component.translatable("tooltip.feedback.food.carries", entry.food().actives().keySet().stream()
                        .sorted().map(c -> c.startsWith("perk:") ? Component.translatable("perk.feedback." + c.substring(5)) : Component.translatable("compound.feedback." + c)).reduce((a, b) -> a.copy().append(", ").append(b)).orElse(Component.empty()))
                        .withStyle(ChatFormatting.DARK_AQUA));
            // Shift says what each perk does, a line apiece.
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown())
                entry.food().actives().keySet().stream().filter(c -> c.startsWith("perk:")).sorted().forEach(c ->
                        event.getToolTip().add(Readout.gloss(Component.translatable("perk.feedback." + c.substring(5)).withStyle(ChatFormatting.DARK_AQUA),
                                "perk.feedback." + c.substring(5) + ".gloss")));
            var cut = event.getItemStack().get(FDataComponents.FOOD_CUT.get());
            if (cut != null)
                event.getToolTip().add(io.github.soundgoodizerfan.feedback.modular.ToolTableMenu.cutName(cut)
                        .withStyle(cut.ragged() ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            var knead = event.getItemStack().get(FDataComponents.FOOD_KNEAD.get());
            if (knead != null)
                event.getToolTip().add(Component.translatable("prep.feedback.knead." + knead.grade())
                        .withStyle(knead.grade() == io.github.soundgoodizerfan.feedback.process.FoodKnead.GLOSSY ? ChatFormatting.GOLD
                                : knead.grade() == io.github.soundgoodizerfan.feedback.process.FoodKnead.GOOD ? ChatFormatting.GRAY : ChatFormatting.RED));
            String shape = event.getItemStack().get(FDataComponents.FOOD_SHAPE.get());
            if (shape != null)
                event.getToolTip().add(Component.translatable("prep.feedback.shape." + shape).withStyle(ChatFormatting.GRAY));
            if (state == null)
                return;
            FoodState now = Cooking.current(event.getItemStack(), entry, level, io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable.at(level).value());
            int spoil = Cooking.spoilage(now);
            if (spoil > 0)
                event.getToolTip().add(Component.translatable("tooltip.feedback.food.spoil." + spoil)
                        .withStyle(spoil == 2 ? ChatFormatting.DARK_RED : ChatFormatting.GOLD));
            event.getToolTip().add(Component.translatable(
                    "tooltip.feedback.food." + Cooking.doneness(entry, now).name().toLowerCase(Locale.ROOT))
                    .withStyle(ChatFormatting.GRAY));
            if (now.smoke() > FTuning.SMOKE_SHOWN_AT)
                event.getToolTip().add(Component.translatable("tooltip.feedback.food.smoked").withStyle(ChatFormatting.GRAY));
            if (now.salt() > 0f)
                event.getToolTip().add(Component.translatable("tooltip.feedback.food.salted").withStyle(ChatFormatting.GRAY));
            if (now.pathogen() > FTuning.FOOD_HAZARD_TOLERANCE)
                event.getToolTip().add(Component.translatable("tooltip.feedback.food.pathogen").withStyle(ChatFormatting.RED));
        });
        event.getToolTip().addAll(readings(event.getItemStack(), level));
    }

    /**
     * The figures a carried instrument reads off a food's present state: water, live load, toxin. Each is absent without an
     * instrument for it, so the adjectives and hazard lines above stay the only sense; the tooltip and the item-info HUD share this.
     */
    public static java.util.List<Component> readings(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level) {
        java.util.List<Component> out = new java.util.ArrayList<>();
        if (!stack.has(FDataComponents.FOOD_STATE.get()))
            return out;
        Cooking.findPerishable(level, stack).ifPresent(entry -> {
            FoodState now = Cooking.current(stack, entry, level, io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable.at(level).value());
            if (Readout.instrumented(Quantity.MOISTURE))
                out.add(Readout.reading(Quantity.MOISTURE, "feedback.readout.moisture", Readout.figure(Quantity.MOISTURE, now.moisture())));
            if (Readout.instrumented(Quantity.PATHOGEN))
                out.add(Readout.reading(Quantity.PATHOGEN, "feedback.readout.pathogen", Readout.figure(Quantity.PATHOGEN, io.github.soundgoodizerfan.feedback.process.Material.Food.qu(now.pathogen()).value())));
            if (Readout.instrumented(Quantity.TOXIN))
                out.add(Readout.reading(Quantity.TOXIN, "feedback.readout.toxin", Readout.figure(Quantity.TOXIN, io.github.soundgoodizerfan.feedback.process.Material.Food.qu(now.toxin()).value())));
        });
        return out;
    }
}
