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
package io.github.soundgoodizerfan.feedback.compat.jei;

import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.process.Casting;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A melt freezing in a mold, drawn inside the Thermal Process card ({@link ThermalProcessCategory}): what a mold of a given shape holds,
 * the figure it must cool to, and what comes out. The mold is the shape; the melt names nothing.
 */
final class CastingCategory {

    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private CastingCategory() {
    }

    static void layout(IRecipeLayoutBuilder builder, Casting recipe) {
        var fluids = recipe.fluid().getFluids();
        if (fluids.length > 0)
            builder.addInputSlot(1, 24).setStandardSlotBackground().addFluidStack(fluids[0].getFluid(), fluids[0].getAmount()).setFluidRenderer(Math.max(1, fluids[0].getAmount()), false, 16, 16);
        builder.addOutputSlot(143, 24).setOutputSlotBackground().addItemStack(recipe.result());
    }

    static void drawCard(Casting recipe, GuiGraphics graphics) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, recipe.result().getHoverName().getString().toUpperCase(java.util.Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.mold"), 24, 16, LABEL_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.mold." + recipe.mold()), 76, 16, VALUE_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.solid_at"), 24, 28, LABEL_COLOUR, false);
        graphics.drawString(font, "<= " + Readout.number(recipe.solidifyTemperature().value()) + " Tu", 76, 28, VALUE_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.amount"), 24, 40, LABEL_COLOUR, false);
        graphics.drawString(font, recipe.fluid().amount() + " mB", 76, 40, VALUE_COLOUR, false);
    }
}
