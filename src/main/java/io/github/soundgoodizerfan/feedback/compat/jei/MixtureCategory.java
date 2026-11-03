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
import io.github.soundgoodizerfan.feedback.process.Mixture;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.CartridgeItem;
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

/** A mixture card: the shares a mixture must hold (within tolerance), how hot it may be, and what it resolves into. Machines that act on a mixture (the Reloading Press) ask this table. */
public class MixtureCategory implements IRecipeCategory<Mixture> {

    static final RecipeType<Mixture> TYPE = RecipeType.create("feedback", "mixture", Mixture.class);

    private static final int WIDTH = 168, HEIGHT = 60;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public MixtureCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FWeapons.MIXER_ITEM.get()));
    }

    @Override
    public RecipeType<Mixture> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.mixture");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Mixture recipe, IFocusGroup focuses) {
        if (recipe.hasFluidResult())
            builder.addOutputSlot(143, 22).setOutputSlotBackground()
                    .addFluidStack(recipe.resultFluid().getFluid(), recipe.resultFluid().getAmount()).setFluidRenderer(Math.max(1, recipe.resultFluid().getAmount()), false, 16, 16);
        else
            builder.addOutputSlot(143, 22).setOutputSlotBackground().addItemStack(recipe.result());
    }

    @Override
    public void draw(Mixture recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, (recipe.hasFluidResult() ? recipe.resultFluid().getHoverName() : recipe.result().getHoverName()).getString().toUpperCase(Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        int y = 13;
        for (var share : recipe.shares().entrySet()) {
            graphics.drawString(font, Math.round(share.getValue() * 100) + "% ", 1, y, VALUE_COLOUR, false);
            graphics.drawString(font, CartridgeItem.name(share.getKey()), 30, y, VALUE_COLOUR, false);
            y += 9;
        }
        graphics.drawString(font, Component.translatable("feedback.jei.mixture.tolerance", Math.round(recipe.tolerance() * 100)), 1, 49, LABEL_COLOUR, false);
        graphics.drawString(font, band(recipe), 90, 49, LABEL_COLOUR, false);
    }

    private static String band(Mixture recipe) {
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
