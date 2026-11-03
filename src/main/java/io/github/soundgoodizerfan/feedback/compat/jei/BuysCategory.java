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

import io.github.soundgoodizerfan.feedback.village.VillageEconomy;
import io.github.soundgoodizerfan.feedback.village.Want;
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
import net.minecraft.world.item.Items;

/** A line of what villagers buy: the item (an apple stands for any food), the emeralds each, and the daily cap. */
public class BuysCategory implements IRecipeCategory<Want> {

    static final RecipeType<Want> TYPE = RecipeType.create("feedback", "buys", Want.class);

    private static final int WIDTH = 168, HEIGHT = 40;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public BuysCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.EMERALD));
    }

    @Override
    public RecipeType<Want> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.buys");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Want want, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 18).setStandardSlotBackground().addItemStack(want.icon());
        builder.addOutputSlot(143, 18).setOutputSlotBackground().addItemStack(new ItemStack(Items.EMERALD, want.price()));
    }

    @Override
    public void draw(Want want, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, buyer(want).getString().toUpperCase(Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        graphics.drawString(font, want.food() ? Component.translatable("feedback.jei.buys.food") : want.icon().getHoverName(), 24, 14, VALUE_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.buys.each", want.price()), 24, 25, LABEL_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.buys.max", want.max()), 100, 25, LABEL_COLOUR, false);
    }

    private static Component buyer(Want want) {
        if (want.profession().equals(Want.ANY))
            return Component.translatable("feedback.jei.buys.any");
        if (want.profession().equals(VillageEconomy.WANDERER))
            return Component.translatable("entity.minecraft.wandering_trader");
        return Component.translatable("entity." + want.profession().getNamespace() + ".villager." + want.profession().getPath());
    }
}
