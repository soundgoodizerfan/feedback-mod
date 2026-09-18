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
 * not, and philosophy 8 is explicit about why: a requirement is published data. The player may
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
    private static final int HEIGHT = 90;

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
        List<Ingredient> inputs = recipe.inputs();
        for (int i = 0; i < inputs.size() && i < 3; i++)
            builder.addInputSlot(1, 13 + i * 18).setStandardSlotBackground().addIngredients(inputs.get(i));

        // A melt's output is a fluid, not an item -- see ThermalProcess#resultFluid. Same slot,
        // whichever ingredient type actually applies; never both.
        if (recipe.hasFluidResult()) {
            FluidStack fluid = recipe.resultFluid();
            builder.addSlot(RecipeIngredientRole.OUTPUT, 1, 69).setOutputSlotBackground()
                    .addFluidStack(fluid.getFluid(), fluid.getAmount());
        } else {
            builder.addOutputSlot(1, 69).setOutputSlotBackground().addItemStack(recipe.result());
        }
    }

    @Override
    public void draw(ThermalProcess recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        String title = recipe.hasFluidResult()
                ? recipe.resultFluid().getHoverName().getString()
                : recipe.result().getHoverName().getString();
        graphics.drawString(font, title.toUpperCase(Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        graphics.fill(1, 11, WIDTH - 1, 12, RULE_COLOUR);

        int y = 18;
        row(graphics, font, y, "feedback.jei.inputs", inputNames(recipe), VALUE_COLOUR);
        y += 12;
        String band = recipe.maxTemperature().value() < Float.MAX_VALUE
                ? Readout.number(recipe.minTemperature().value()) + "-" + Readout.number(recipe.maxTemperature().value()) + " Tu"
                : Readout.number(recipe.minTemperature().value()) + "+ Tu";
        row(graphics, font, y, "feedback.jei.temperature", band, VALUE_COLOUR);
        y += 12;
        // A requirement, not a reading -- philosophy 8's "what a process requires is free" -- so
        // the optimum is printed plainly rather than treated as TPu made player-facing.
        // tpu_spec_doc.md is explicit that TPu itself is never shown; this is a Tu figure, same
        // as the band above it.
        row(graphics, font, y, "feedback.jei.optimal", Readout.number(recipe.optimalTemperature().value()) + " Tu", VALUE_COLOUR);
        y += 12;
        // Both, always: the philosophy's own convention for showing time, because ticks are what
        // the simulation counts and seconds are what the player waits. This is time to complete
        // held exactly at the optimum -- see ThermalProcess#requiredTpu -- not a flat countdown;
        // real time depends on how close the player actually holds it. Left off entirely for a
        // pooled recipe -- see ThermalProcess.NO_HOLD -- rather than print a figure that was
        // never true of it.
        if (recipe.hasHold()) {
            int atOptimum = Math.round(recipe.requiredTpu());
            row(graphics, font, y, "feedback.jei.hold",
                    atOptimum + " t (" + Readout.number(atOptimum / 20f) + " s) at optimum", VALUE_COLOUR);
            y += 12;
        }
        if (recipe.maxRateTuPerTick().tuPerTick() < Float.MAX_VALUE) {
            row(graphics, font, y, "feedback.jei.max_rate",
                    Readout.number(recipe.maxRateTuPerTick().tuPerTick()) + " Tu/t", VALUE_COLOUR);
            y += 12;
        }
        if (recipe.spoilTemperature().value() < Float.MAX_VALUE)
            row(graphics, font, y, "feedback.jei.spoils", "> " + Readout.number(recipe.spoilTemperature().value()) + " Tu",
                    SPOIL_COLOUR);
    }

    private static void row(GuiGraphics graphics, Font font, int y, String labelKey, String value, int colour) {
        graphics.drawString(font, Component.translatable(labelKey), LABEL_X, y, LABEL_COLOUR, false);
        graphics.drawString(font, value, VALUE_X, y, colour, false);
    }

    private static String inputNames(ThermalProcess recipe) {
        StringBuilder names = new StringBuilder();
        for (Ingredient input : recipe.inputs()) {
            ItemStack[] accepted = input.getItems();
            if (accepted.length == 0)
                continue;
            if (names.length() > 0)
                names.append(", ");
            names.append(accepted[0].getHoverName().getString());
        }
        return names.length() == 0 ? "-" : names.toString();
    }
}
