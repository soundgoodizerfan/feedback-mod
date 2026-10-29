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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Board;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.Soldering;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Soldering is an operation, not a recipe, so this card is the rule written out per solder alloy:
 * a pasted board in, the same board graded out, and the figures {@link Soldering#grade} compares
 * a joint against. Exact, because they are requirements (philosophy 8). The iron and the oven are
 * the catalysts: they differ in what reaching the window costs, not in the window.
 */
public class SolderingCategory implements IRecipeCategory<SolderingCategory.Card> {

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "soldering", Card.class);

    /** One solder alloy and the boards it joins. {@code lowestSubstrateTu} is the lowest service temperature of any substrate loaded. */
    public record Card(ItemStack solder, ItemStack pasted, ItemStack joined, ItemStack circuit, float liquidus, float window,
                       float lowestSubstrateTu) {}

    private static final int WIDTH = 168, HEIGHT = 104;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public SolderingCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.SOLDERING_IRON.get()));
    }

    /** One card per solder alloy that has a melting point, on a printed board over the first substrate loaded. */
    static List<Card> cards(Map<ResourceLocation, Material> materials) {
        ResourceLocation substrate = null, paste = null;
        float lowest = Float.MAX_VALUE;
        for (var e : materials.entrySet()) {
            Material m = e.getValue();
            if (m.item(e.getKey(), "substrate").isPresent()) {
                substrate = substrate == null ? e.getKey() : substrate;
                lowest = Math.min(lowest, m.thermal().serviceTemperature().map(t -> t.value()).orElse(Float.MAX_VALUE));
            }
            if (paste == null && m.traits().contains(Material.Trait.PASTE))
                paste = e.getKey();
        }
        List<Card> out = new ArrayList<>();
        if (substrate == null || paste == null)
            return out;
        for (var e : materials.entrySet()) {
            Optional<Soldering.Alloy> alloy = Soldering.alloy(e.getValue());
            Optional<ItemStack> ingot = e.getValue().stack(e.getKey(), "ingot");
            if (alloy.isEmpty() || ingot.isEmpty())
                continue;
            Board pasted = new Board(Board.Trace.PRINTED, paste, substrate, Optional.of(e.getKey()), Board.Joint.NONE, List.of());
            // A board with a part mounted comes out of the same joint as a circuit.
            ItemStack circuit = Soldering.finish(board(pasted.mounted(new Board.Part(Board.Kind.RELAY, 1f))), materials,
                    alloy.get().liquidus() + alloy.get().window() / 2f, FTuning.SOLDER_DWELL_TICKS);
            out.add(new Card(ingot.get(), board(pasted), board(pasted.withJoint(Board.Joint.GOOD)), circuit,
                    alloy.get().liquidus(), alloy.get().window(), lowest));
        }
        return out;
    }

    private static ItemStack board(Board board) {
        ItemStack stack = new ItemStack(FItems.TRACE_BOARD.get());
        stack.set(FDataComponents.BOARD.get(), board);
        return stack;
    }

    @Override
    public RecipeType<Card> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.soldering");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Card card, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 14).setStandardSlotBackground().addItemStack(card.pasted());
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 34).setStandardSlotBackground().addItemStack(card.solder());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 1, 62).setOutputSlotBackground().addItemStack(card.joined());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 1, 82).setOutputSlotBackground().addItemStack(card.circuit());
    }

    @Override
    public void draw(Card card, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, card.solder().getHoverName().getString().toUpperCase(java.util.Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        row(graphics, font, 14, "feedback.jei.soldering.liquidus", tu(card.liquidus()));
        row(graphics, font, 26, "feedback.jei.soldering.window", tu(card.liquidus()) + " - " + tu(card.liquidus() + card.window()));
        row(graphics, font, 38, "feedback.jei.soldering.dwell", FTuning.SOLDER_DWELL_TICKS + " t (" + FTuning.SOLDER_DWELL_TICKS / 20 + " s)");
        row(graphics, font, 50, "feedback.jei.soldering.lift_printed", tu(FTuning.PRINTED_TRACE_LIMIT_TU));
        if (card.lowestSubstrateTu() < Float.MAX_VALUE)
            row(graphics, font, 62, "feedback.jei.soldering.lift_substrate", tu(card.lowestSubstrateTu()));
        graphics.drawString(font, Component.translatable("feedback.jei.soldering.note"), 1, HEIGHT - 10, LABEL_COLOUR, false);
    }

    private static void row(GuiGraphics graphics, Font font, int y, String key, String value) {
        graphics.drawString(font, Component.translatable(key), 24, y, LABEL_COLOUR, false);
        graphics.drawString(font, value, 96, y, VALUE_COLOUR, false);
    }

    private static String tu(float value) {
        return Math.round(value) + " Tu";
    }
}
