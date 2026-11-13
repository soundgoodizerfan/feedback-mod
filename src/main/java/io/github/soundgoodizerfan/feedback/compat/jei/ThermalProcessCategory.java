// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.process.ThermalProcess;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A process card: what a fire does to a set of materials, stated in exact units.
 *
 * <h2>Publishing the window is not giving the game away</h2>
 * It is tempting to hide "1420 to 1480 Tu" on the grounds that discovering it is the beat. It is
 * not, and philosophy 10 is explicit about why: a requirement is published data. The player may
 * read the window on their first day, own nothing, and still burn six stacks of iron -- because
 * knowing the target and knowing <em>where you are</em> are different problems, and only the second
 * is for sale. A hidden number would make the beat about a wiki lookup instead.
 *
 * <h2>The spoil line is the most useful thing on the card</h2>
 * Beat 2's failure modes are asymmetric on purpose: undershooting does nothing, overshooting
 * destroys. Printing the spoil temperature tells the player which way to err long before they can
 * measure anything, which is exactly the lesson §6 wants them to carry into every later process.
 *
 * <h2>One card for hand-authored and pooled recipes alike</h2>
 * A pooled vanilla/modded cooking recipe (see {@code FeedbackJeiPlugin.pooled}) is wrapped as a
 * {@link ThermalProcess} too, so it draws through this same card rather than a second category and
 * a second tab. It carries less: {@link ThermalProcess#hasHold()} is false for one, so the Hold
 * row is left off instead of printing a number nobody measured, and its temperature band is often
 * open-ended ({@link ThermalProcess#maxTemperature()} of {@code Float.MAX_VALUE}, drawn as
 * {@code "800+"}) because a bare floor is all §15's two constants ever promised.

 *
 * <h2>Melting is this card too, not a new one</h2>
 * A melt is a {@link ThermalProcess} whose {@link ThermalProcess#resultFluid()} is set instead of
 * {@link ThermalProcess#result()} -- min temperature is the melt point, everything else at its
 * ordinary defaults. It was tempting to give melting its own category the way the first cut of
 * the pooled-recipe merge did: wrong for the same reason that was. Band, hold and spoil are
 * per-entry fields already, not a type distinction, and the only thing that actually differs is
 * the output slot's ingredient type -- so that is the only thing {@link #setRecipe} branches on.
 */
public class ThermalProcessCategory implements IRecipeCategory<ThermalProcess> {

    static final RecipeType<ThermalProcess> TYPE =
            RecipeType.create("feedback", "thermal_process", ThermalProcess.class);

    private static final int WIDTH = 168;
    private static final int HEIGHT = ThermalCard.HEIGHT;

    private static final int LABEL_X = 24;
    private static final int VALUE_X = 76;

    // JEI's recipe background is light grey. See DeformationCategory for the time this was got
    // wrong in the other direction.
    private static final int TITLE_COLOUR = 0xFF3F3F3F;
    private static final int LABEL_COLOUR = 0xFF7A7A7A;
    private static final int VALUE_COLOUR = 0xFF262626;
    private static final int SPOIL_COLOUR = 0xFF8A2B12;
    private static final int RULE_COLOUR = 0x40000000;

    private final IDrawable icon;

    public ThermalProcessCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.SMALL_CRUCIBLE.get()));
    }

    @Override
    public RecipeType<ThermalProcess> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.thermal_process");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ThermalProcess recipe, IFocusGroup focuses) {
        ThermalCard.layout(builder, recipe);
    }

    @Override
    public void draw(ThermalProcess recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        ThermalCard.draw(recipe, slots, graphics);
    }
}
