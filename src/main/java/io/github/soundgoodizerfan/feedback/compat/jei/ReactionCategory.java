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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * A reaction card: a thermal-process row that carries a {@code reaction} block (cracking,
 * polymerisation, the soul retort, ...). Item and fluid inputs on the left, the item and fluid
 * outputs on the right, the catalyst bed in between, and the temperature, pressure and hold
 * beneath. Every such row in the table is published here, so a new reaction file appears with no
 * code change.
 */
public class ReactionCategory implements IRecipeCategory<ThermalProcess> {

    static final RecipeType<ThermalProcess> TYPE = RecipeType.create("feedback", "reaction", ThermalProcess.class);

    private static final int WIDTH = 168, HEIGHT = 168, MAX_ROWS = 4;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public ReactionCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.REACTOR.get()));
    }

    @Override
    public RecipeType<ThermalProcess> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.reaction");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ThermalProcess p, IFocusGroup focuses) {
        int in = 0, out = 0;
        for (Ingredient input : p.inputs())
            builder.addInputSlot(1 + 18 * (in / MAX_ROWS), 14 + 18 * (in++ % MAX_ROWS)).setStandardSlotBackground().addIngredients(input);
        var reaction = p.reaction();
        for (var a : reaction.map(ThermalProcess.Reaction::fluidInputs).orElse(List.of()))
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + 18 * (in / MAX_ROWS), 14 + 18 * (in++ % MAX_ROWS)).setStandardSlotBackground()
                    .addFluidStack(BuiltInRegistries.FLUID.get(a.fluid()), a.mb());
        if (!p.result().isEmpty())
            builder.addOutputSlot(WIDTH - 19 - 18 * (out / MAX_ROWS), 14 + 18 * (out++ % MAX_ROWS)).setStandardSlotBackground().addItemStack(p.result());
        for (var a : reaction.map(ThermalProcess.Reaction::fluidOutputs).orElse(List.of()))
            builder.addSlot(RecipeIngredientRole.OUTPUT, WIDTH - 19 - 18 * (out / MAX_ROWS), 14 + 18 * (out++ % MAX_ROWS)).setStandardSlotBackground()
                    .addFluidStack(BuiltInRegistries.FLUID.get(a.fluid()), a.mb());
        if (!p.resultFluid().isEmpty())
            builder.addSlot(RecipeIngredientRole.OUTPUT, WIDTH - 19 - 18 * (out / MAX_ROWS), 14 + 18 * (out % MAX_ROWS)).setStandardSlotBackground()
                    .addFluidStack(p.resultFluid().getFluid(), p.resultFluid().getAmount());
        reaction.flatMap(ThermalProcess.Reaction::catalyst).ifPresent(tag ->
                builder.addSlot(RecipeIngredientRole.CATALYST, WIDTH / 2 - 9, 14).setStandardSlotBackground()
                        .addItemStacks(java.util.stream.StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(tag).spliterator(), false)
                                .map(h -> new ItemStack(h.value())).toList()));
    }

    @Override
    public void draw(ThermalProcess p, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, title(p).toUpperCase(Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        int y = 14 + 18 * MAX_ROWS + 4;
        float max = p.maxTemperature().value();
        y = row(graphics, font, y, "feedback.jei.reaction.temperature", max < Float.MAX_VALUE
                ? Readout.number(p.minTemperature().value()) + "-" + Readout.number(max) + " Tu"
                : ">= " + Readout.number(p.minTemperature().value()) + " Tu");
        y = row(graphics, font, y, "feedback.jei.optimal", Readout.number(p.optimalTemperature().value()) + " Tu");
        if (p.hasHold())
            y = row(graphics, font, y, "feedback.jei.hold", Math.round(p.requiredTpu()) + " t at optimum");
        if (p.maxRateTuPerTick().tuPerTick() < Float.MAX_VALUE)
            y = row(graphics, font, y, "feedback.jei.max_rate", Readout.number(p.maxRateTuPerTick().tuPerTick()) + " Tu/t");
        var r = p.reaction();
        if (r.isPresent() && r.get().minPu().isPresent())
            y = row(graphics, font, y, "feedback.jei.reaction.pressure", Readout.number(r.get().minPu().get().value())
                    + (r.get().optimalPu().isPresent() ? " (best " + Readout.number(r.get().optimalPu().get().value()) + ")" : "") + " Pu");
        if (r.isPresent() && r.get().catalyst().isPresent())
            y = row(graphics, font, y, "feedback.jei.reaction.catalyst", Component.translatable("feedback.jei.reaction.bed").getString());
        if (r.isPresent() && r.get().heat().isPresent())
            y = row(graphics, font, y, "feedback.jei.reaction.heat", Readout.number(r.get().heat().get()) + " Tu/t");
        if (p.spoilTemperature().value() < Float.MAX_VALUE)
            row(graphics, font, y, "feedback.jei.spoils", "> " + Readout.number(p.spoilTemperature().value()) + " Tu");
    }

    /** First output's name: a fluid output before an item, since most reactions end in a fluid. */
    private static String title(ThermalProcess p) {
        var outs = p.reaction().map(ThermalProcess.Reaction::fluidOutputs).orElse(List.of());
        if (!outs.isEmpty())
            return new net.neoforged.neoforge.fluids.FluidStack(BuiltInRegistries.FLUID.get(outs.get(0).fluid()), 1)
                    .getHoverName().getString();
        if (!p.resultFluid().isEmpty())
            return p.resultFluid().getHoverName().getString();
        return p.result().isEmpty() ? Component.translatable("feedback.jei.reaction").getString() : p.result().getHoverName().getString();
    }

    private static int row(GuiGraphics graphics, Font font, int y, String labelKey, String value) {
        graphics.drawString(font, Component.translatable(labelKey), 1, y, LABEL_COLOUR, false);
        graphics.drawString(font, value, 60, y, VALUE_COLOUR, false);
        return y + 10;
    }
}
