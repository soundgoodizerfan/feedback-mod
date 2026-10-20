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

import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.process.AlloyRange;
import io.github.soundgoodizerfan.feedback.process.AlloyRecipe;
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
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * An alloy card: each metal and the share of the whole melt it must make up, and what the mix
 * becomes. A composition window is a requirement, so it is printed exactly (philosophy 8); the
 * player pours known amounts and the ratio falls out ({@code feedback_mechanics.md} §4.2).
 */
public class AlloyCategory implements IRecipeCategory<AlloyRecipe> {

    static final RecipeType<AlloyRecipe> TYPE = RecipeType.create("feedback", "alloy", AlloyRecipe.class);

    private static final int ROWS = 3;
    private static final int WIDTH = 168, HEIGHT = 14 + ROWS * 18 + 12;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public AlloyCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.SMALL_CRUCIBLE.get()));
    }

    @Override
    public RecipeType<AlloyRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.alloy");
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
    public void setRecipe(IRecipeLayoutBuilder builder, AlloyRecipe recipe, IFocusGroup focuses) {
        List<AlloyRange> metals = recipe.contents();
        for (int i = 0; i < metals.size() && i < ROWS; i++)
            builder.addSlot(RecipeIngredientRole.INPUT, 1, 14 + i * 18).setStandardSlotBackground()
                    .addFluidStack(metals.get(i).fluid());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 143, 14).setOutputSlotBackground().addFluidStack(recipe.result());
    }

    @Override
    public void draw(AlloyRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, new FluidStack(recipe.result(), 1).getHoverName().getString().toUpperCase(Locale.ROOT),
                1, 1, TITLE_COLOUR, false);
        List<AlloyRange> metals = recipe.contents();
        for (int i = 0; i < metals.size() && i < ROWS; i++) {
            AlloyRange m = metals.get(i);
            graphics.drawString(font, percent(m.min()) + "-" + percent(m.max()) + "%", 24, 19 + i * 18, VALUE_COLOUR, false);
        }
        graphics.drawString(font, Component.translatable("feedback.jei.alloy.note"), 1, HEIGHT - 10, LABEL_COLOUR, false);
    }

    private static String percent(float fraction) {
        return Integer.toString(Math.round(fraction * 100f));
    }
}
