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

import io.github.soundgoodizerfan.feedback.process.Deformation;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A process card: what mechanical deformation does to one material, stated in exact units.
 *
 * <h2>A spec sheet, not a recipe</h2>
 * Every figure here is a <em>requirement</em>, and philosophy 8 says requirements are free and
 * exact. A player may read that copper wants 14 Fu on their first day, own nothing, and still fail
 * repeatedly -- knowing the target and knowing where you are are different problems, and only the
 * second is for sale. So the card prints figures without apology.
 *
 * <h2>Why the card looks like this</h2>
 * It deliberately does not look like a crafting recipe. There is no arrow implying a machine that
 * performs a transformation on request, because no such machine exists: the hammer applies force to
 * whatever is in front of it and this table describes how the <em>material</em> answers. Labelled
 * rows read as a handbook entry for a material, which is what it is.
 * <p>
 * <b>Corrected: the operation name used to live only on the category tab, on the theory that "the
 * card is about copper; mechanical deformation is the shelf it sits on."</b> That held while
 * {@link FItems#MECHANICAL_HAMMER} was the only catalyst this category had. It stopped holding the
 * moment the Wire Drawer, Rolling Mill and Mechanical Press all started publishing into the same
 * category -- a player looking at a list of cards had no way to tell a hammer's blow recipe from a
 * wire drawer's draw recipe, because both are "mechanical deformation" and neither said which
 * machine wants it. The operation is now a row on every card (see {@link #draw}), and {@link
 * RemovalCategory} was given the identical row from the start for the same reason.
 */
public class DeformationCategory implements IRecipeCategory<Deformation> {

    static final RecipeType<Deformation> TYPE =
            RecipeType.create("feedback", "mechanical_deformation", Deformation.class);

    private final IDrawable icon;

    public DeformationCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.MECHANICAL_HAMMER.get()));
    }

    @Override
    public RecipeType<Deformation> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.mechanical_deformation");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WorkCard.WIDTH;
    }

    @Override
    public int getHeight() {
        return WorkCard.HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Deformation recipe, IFocusGroup focuses) {
        WorkCard.slots(builder, recipe.input(), recipe.result());
    }

    @Override
    public void draw(Deformation recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        WorkCard.draw(graphics, recipe.operation(), recipe.input(), recipe.work(),
                "feedback.jei.hardness", recipe.hardness(), recipe.result());
    }
}
