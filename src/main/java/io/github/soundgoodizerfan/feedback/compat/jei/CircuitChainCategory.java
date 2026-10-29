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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;
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
 * The whole way to a first Basic Electronic Circuit on one page: grind, lay traces, mount and paste,
 * solder. Every slot is a real JEI ingredient, so a click on any item opens that item's own recipes
 * (the grinding card, the coherer's craft, the alloy card). The cheapest route is the one drawn: a
 * printed board, a coherer, the narrowest solder. Built from the loaded materials, like
 * {@link SolderingCategory}.
 */
public class CircuitChainCategory implements IRecipeCategory<CircuitChainCategory.Card> {

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "circuit_chain", Card.class);

    public record Card(ItemStack brick, ItemStack substrate, ItemStack dust, ItemStack board, ItemStack coherer,
                       List<ItemStack> solders, ItemStack pasted, ItemStack circuit) {}

    private static final int WIDTH = 168, HEIGHT = 112;
    private static final int LABEL_COLOUR = 0xFF3F3F3F, NOTE_COLOUR = 0xFF7A7A7A;
    private static final int[] ROWS = {2, 26, 50, 74};

    private final IDrawable icon;

    public CircuitChainCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.CIRCUIT.get()));
    }

    /** One card, or none if the table has no substrate, paste or solder to build it from. */
    static List<Card> cards(Map<ResourceLocation, Material> materials) {
        ResourceLocation substrate = null, paste = null;
        List<ResourceLocation> solders = new ArrayList<>();
        for (var e : materials.entrySet()) {
            Material m = e.getValue();
            if (m.item(e.getKey(), "substrate").isPresent() && m.item(e.getKey(), "brick").isPresent()
                    && (substrate == null || e.getKey().equals(Feedback.id("brick"))))
                substrate = e.getKey();
            if (paste == null && m.traits().contains(Material.Trait.PASTE) && m.item(e.getKey(), "dust").isPresent())
                paste = e.getKey();
            if (Soldering.alloy(m).isPresent() && m.item(e.getKey(), "ingot").isPresent())
                solders.add(e.getKey());
        }
        List<Card> out = new ArrayList<>();
        if (substrate == null || paste == null || solders.isEmpty())
            return out;
        // The narrowest window first: the cheapest solder is the one the page shows being pasted.
        solders.sort(Comparator.comparingDouble(s -> Soldering.alloy(materials.get(s)).get().window()));
        Soldering.Alloy alloy = Soldering.alloy(materials.get(solders.get(0))).get();
        ItemStack coherer = new ItemStack(FItems.COHERER.get());
        Board.Part part = new Board.Part(Board.Kind.COHERER, 1f);
        Board bare = new Board(Board.Trace.PRINTED, paste, substrate, Optional.empty(), Board.Joint.NONE, List.of());
        Board pasted = bare.mounted(part).pasted(solders.get(0));
        ItemStack circuit = Soldering.finish(board(pasted), materials, alloy.liquidus() + alloy.window() / 2f, FTuning.SOLDER_DWELL_TICKS);
        List<ItemStack> ingots = new ArrayList<>();
        for (ResourceLocation s : solders)
            ingots.add(materials.get(s).stack(s, "ingot").get());
        out.add(new Card(materials.get(substrate).stack(substrate, "brick").get(), materials.get(substrate).stack(substrate, "substrate").get(),
                materials.get(paste).stack(paste, "dust").get(), board(bare), coherer, ingots, board(pasted), circuit));
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
        return Component.translatable("feedback.jei.circuit_chain");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Card c, IFocusGroup focuses) {
        // 1 grind: brick on a grinding wheel.
        builder.addSlot(RecipeIngredientRole.INPUT, 64, ROWS[0]).setStandardSlotBackground().addItemStack(c.brick());
        builder.addSlot(RecipeIngredientRole.CATALYST, 84, ROWS[0]).setStandardSlotBackground().addItemStack(new ItemStack(FItems.GRINDING_WHEEL.get()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 144, ROWS[0]).setOutputSlotBackground().addItemStack(c.substrate());
        // 2 traces: substrate and redstone dust in a crafting grid.
        builder.addSlot(RecipeIngredientRole.INPUT, 64, ROWS[1]).setStandardSlotBackground().addItemStack(c.substrate());
        builder.addSlot(RecipeIngredientRole.INPUT, 84, ROWS[1]).setStandardSlotBackground().addItemStack(c.dust());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 144, ROWS[1]).setOutputSlotBackground().addItemStack(c.board());
        // 3 mount and paste: a board, a coherer, a solder ingot (two grid crafts).
        builder.addSlot(RecipeIngredientRole.INPUT, 64, ROWS[2]).setStandardSlotBackground().addItemStack(c.board());
        builder.addSlot(RecipeIngredientRole.INPUT, 84, ROWS[2]).setStandardSlotBackground().addItemStack(c.coherer());
        builder.addSlot(RecipeIngredientRole.INPUT, 104, ROWS[2]).setStandardSlotBackground().addItemStacks(c.solders());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 144, ROWS[2]).setOutputSlotBackground().addItemStack(c.pasted());
        // 4 solder: the pasted board under a hot iron, or in an oven.
        builder.addSlot(RecipeIngredientRole.INPUT, 64, ROWS[3]).setStandardSlotBackground().addItemStack(c.pasted());
        builder.addSlot(RecipeIngredientRole.CATALYST, 84, ROWS[3]).setStandardSlotBackground()
                .addItemStacks(List.of(new ItemStack(FItems.SOLDERING_IRON.get()), new ItemStack(FItems.REFLOW_OVEN.get())));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 144, ROWS[3]).setOutputSlotBackground().addItemStack(c.circuit());
    }

    @Override
    public void draw(Card card, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        for (int i = 0; i < ROWS.length; i++) {
            graphics.drawString(font, Component.translatable("feedback.jei.circuit_chain.step" + (i + 1)), 1, ROWS[i] + 5, LABEL_COLOUR, false);
            graphics.drawString(font, "->", 126, ROWS[i] + 5, NOTE_COLOUR, false);
        }
        graphics.drawString(font, Component.translatable("feedback.jei.circuit_chain.note1"), 1, 98, NOTE_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.circuit_chain.note2"), 1, 106, NOTE_COLOUR, false);
    }
}
