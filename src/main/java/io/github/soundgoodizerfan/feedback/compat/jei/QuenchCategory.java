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
import io.github.soundgoodizerfan.feedback.process.Quench;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A quench card: what goes into water, how hot it must still be when it lands, and what comes out.
 * The floor is a requirement, so it is printed exactly (philosophy 8); the race against the
 * piece's own cooling is the player's.
 */
public class QuenchCategory implements IRecipeCategory<Quench> {

    static final RecipeType<Quench> TYPE = RecipeType.create("feedback", "quench", Quench.class);

    private static final int WIDTH = 168, HEIGHT = 40;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public QuenchCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.WATER_BUCKET));
    }

    @Override
    public RecipeType<Quench> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.quench");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Quench recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 18).setStandardSlotBackground().addIngredients(recipe.input());
        builder.addOutputSlot(143, 18).setOutputSlotBackground().addItemStack(recipe.result());
    }

    @Override
    public void draw(Quench recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, recipe.result().getHoverName().getString().toUpperCase(java.util.Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.quench_into"), 24, 16, LABEL_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.quench_water"), 76, 16, VALUE_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.quench_min"), 24, 28, LABEL_COLOUR, false);
        graphics.drawString(font, ">= " + Readout.number(recipe.minTemperature().value()) + " Tu", 76, 28, VALUE_COLOUR, false);
    }
}
