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

/**
 * A liquid coming apart under current in an electrolyser: the feed on the left, the voltage it needs, and what
 * leaves at the anode (front) and the cathode (back) per millibucket of feed.
 */
public class ElectrolysisCategory implements IRecipeCategory<Electrolysis> {

    static final RecipeType<Electrolysis> TYPE = RecipeType.create("feedback", "electrolysis", Electrolysis.class);

    private static final int WIDTH = 168, HEIGHT = 52;
    private static final int LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public ElectrolysisCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.ELECTROLYSER.get()));
    }

    @Override
    public RecipeType<Electrolysis> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.electrolysis");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Electrolysis recipe, IFocusGroup focuses) {
        var feeds = recipe.feed().getStacks();
        if (feeds.length > 0)
            builder.addSlot(RecipeIngredientRole.INPUT, 1, 18).setStandardSlotBackground()
                    .addFluidStack(feeds[0].getFluid(), 1000);
        recipe.item().ifPresent(item -> builder.addSlot(RecipeIngredientRole.INPUT, 1, 34).setStandardSlotBackground()
                .addIngredients(item));
        recipe.residue().ifPresent(r -> builder.addSlot(RecipeIngredientRole.OUTPUT, 75, 18).setOutputSlotBackground()
                .addFluidStack(BuiltInRegistries.FLUID.get(r.fluid()), r.mb()));
        int anode = 0, cathode = 0;
        for (Electrolysis.Product p : recipe.products()) {
            boolean isAnode = p.electrode() == Electrolysis.Electrode.ANODE;
            int row = isAnode ? anode++ : cathode++;
            builder.addSlot(RecipeIngredientRole.OUTPUT, isAnode ? 125 : 143, 4 + row * 20).setOutputSlotBackground()
                    .addFluidStack(BuiltInRegistries.FLUID.get(p.fluid()), p.mb());
        }
    }

    @Override
    public void draw(Electrolysis recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
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
