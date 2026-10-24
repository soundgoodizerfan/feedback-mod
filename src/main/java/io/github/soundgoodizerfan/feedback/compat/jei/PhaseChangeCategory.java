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
import io.github.soundgoodizerfan.feedback.process.PhaseChange;
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
 * A fluid changing phase: the liquid boils to the gas above its boiling point (a boiler) and the
 * gas condenses back to it below (a condenser), the latent Work per mB spent or given back.
 */
public class PhaseChangeCategory implements IRecipeCategory<PhaseChange> {

    static final RecipeType<PhaseChange> TYPE = RecipeType.create("feedback", "phase_change", PhaseChange.class);

    private static final int WIDTH = 168, HEIGHT = 52;
    private static final int LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public PhaseChangeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.BOILER.get()));
    }

    @Override
    public RecipeType<PhaseChange> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.phase_change");
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
    public void setRecipe(IRecipeLayoutBuilder builder, PhaseChange recipe, IFocusGroup focuses) {
        var liquids = recipe.liquid().getStacks();
        if (liquids.length > 0)
            builder.addSlot(RecipeIngredientRole.INPUT, 1, 18).setStandardSlotBackground()
                    .addFluidStack(liquids[0].getFluid(), 1000);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 143, 18).setOutputSlotBackground()
                .addFluidStack(BuiltInRegistries.FLUID.get(recipe.gasFluid()), 1000);
    }

    @Override
    public void draw(PhaseChange recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("feedback.jei.phase_change.boil"), 24, 12, LABEL_COLOUR, false);
        graphics.drawString(font, Readout.number(recipe.boil().value()) + " Tu", 84, 12, VALUE_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.phase_change.latent"), 24, 30, LABEL_COLOUR, false);
        graphics.drawString(font, Readout.number(recipe.latent()) + " Work/mB", 84, 30, VALUE_COLOUR, false);
    }
}
