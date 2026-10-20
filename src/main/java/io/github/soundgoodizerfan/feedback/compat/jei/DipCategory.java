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

import java.util.Locale;

import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.process.Dip;
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
 * A dip card: what is held, what the vessel must hold and how hot it must be, and what comes out.
 * Used on a vessel's tank with the item in hand ({@code machine/dip/Dipping}).
 */
public class DipCategory implements IRecipeCategory<Dip> {

    static final RecipeType<Dip> TYPE = RecipeType.create("feedback", "dip", Dip.class);

    private static final int WIDTH = 168, HEIGHT = 52;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public DipCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.CERAMIC.get()));
    }

    @Override
    public RecipeType<Dip> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.dip");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Dip recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 13).setStandardSlotBackground().addIngredients(recipe.input());
        var fluids = recipe.fluid().getFluids();
        if (fluids.length > 0)
            builder.addSlot(RecipeIngredientRole.INPUT, 1, 31).setStandardSlotBackground()
                    .addFluidStack(fluids[0].getFluid(), fluids[0].getAmount());
        builder.addOutputSlot(143, 22).setOutputSlotBackground().addItemStack(recipe.result());
    }

    @Override
    public void draw(Dip recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, recipe.result().getHoverName().getString().toUpperCase(Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.dip.into"), 24, 18, LABEL_COLOUR, false);
        graphics.drawString(font, recipe.fluid().amount() + " mB", 76, 18, VALUE_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.dip.vessel"), 24, 36, LABEL_COLOUR, false);
        graphics.drawString(font, band(recipe), 76, 36, VALUE_COLOUR, false);
    }

    private static String band(Dip recipe) {
        boolean floor = recipe.minTemperature().value() > -Float.MAX_VALUE;
        boolean ceiling = recipe.maxTemperature().value() < Float.MAX_VALUE;
        String min = Readout.number(recipe.minTemperature().value()), max = Readout.number(recipe.maxTemperature().value());
        if (floor && ceiling)
            return min + "-" + max + " Tu";
        if (floor)
            return ">= " + min + " Tu";
        if (ceiling)
            return "<= " + max + " Tu";
        return Component.translatable("feedback.jei.dip.any").getString();
    }
}
