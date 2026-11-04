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
package io.github.soundgoodizerfan.feedback.compat.jei;

import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.process.Electrolysis;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** An electrolysis card, drawn inside the Thermal Process card ({@link ThermalProcessCategory}): the voltage, the heat it needs, and what leaves at each electrode. */
final class ElectrolysisCategory {

    private static final int LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private ElectrolysisCategory() {
    }

    static void layout(IRecipeLayoutBuilder builder, Electrolysis recipe) {
        var feeds = recipe.feed().getStacks();
        if (feeds.length > 0)
            builder.addSlot(RecipeIngredientRole.INPUT, 1, 18).setStandardSlotBackground()
                    .addFluidStack(feeds[0].getFluid(), 1000).setFluidRenderer(1000, false, 16, 16);
        recipe.item().ifPresent(item -> builder.addSlot(RecipeIngredientRole.INPUT, 1, 34).setStandardSlotBackground()
                .addIngredients(item));
        recipe.residue().ifPresent(r -> builder.addSlot(RecipeIngredientRole.OUTPUT, 75, 18).setOutputSlotBackground()
                .addFluidStack(BuiltInRegistries.FLUID.get(r.fluid()), r.mb()).setFluidRenderer(Math.max(1, r.mb()), false, 16, 16));
        int anode = 0, cathode = 0;
        for (Electrolysis.Product p : recipe.products()) {
            boolean isAnode = p.electrode() == Electrolysis.Electrode.ANODE;
            int row = isAnode ? anode++ : cathode++;
            builder.addSlot(RecipeIngredientRole.OUTPUT, isAnode ? 125 : 143, 4 + row * 20).setOutputSlotBackground()
                    .addFluidStack(BuiltInRegistries.FLUID.get(p.fluid()), p.mb()).setFluidRenderer(Math.max(1, p.mb()), false, 16, 16);
        }
    }

    static void drawCard(Electrolysis recipe, GuiGraphics graphics) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("feedback.jei.electrolysis.volts"), 24, 12, LABEL_COLOUR, false);
        graphics.drawString(font, Readout.number(recipe.volts()) + " V", 24, 24, VALUE_COLOUR, false);
        if (recipe.minTemperature() > 0f)
            graphics.drawString(font, Component.translatable("feedback.jei.electrolysis.hot",
                    Readout.number(recipe.minTemperature())), 24, 38, VALUE_COLOUR, false);
        if (recipe.residue().isPresent())
            graphics.drawString(font, Component.translatable("feedback.jei.electrolysis.residue"), 62, 8, LABEL_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.electrolysis.anode"), 100, 40, LABEL_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.electrolysis.cathode"), 130, 40, LABEL_COLOUR, false);
    }
}
