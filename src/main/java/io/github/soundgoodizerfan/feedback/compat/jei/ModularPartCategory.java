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
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FItems;

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Shaping a part: a hand tool and stock laid in the part's shape in the crafting grid
 * ({@code ModularPartRecipe}). One card per kind of part, not per material -- the material rides
 * on the stack -- so the stock slot cycles through everything the part can be shaped from, in
 * the number of cells the shape has, with the shape drawn beside it.
 */
public class ModularPartCategory implements IRecipeCategory<ModularPartCategory.Card> {

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "modular_part", Card.class);

    /** One shaped part, as a type's role names it. */
    public record Card(ModularType.Shaping shaping) {
    }

    private static final int WIDTH = 168, HEIGHT = 84;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;
    private static final int CELL = 10;

    private final IDrawable icon;

    public ModularPartCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.HAND_HAMMER.get()));
    }

    @Override
    public RecipeType<Card> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.modular_part");
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
        ModularType.Shaping shaping = card.shaping();
        List<ItemStack> stock = new ArrayList<>();
        ClientMaterials.get().forEach((id, m) -> m.items(id).forEach((form, item) -> {
            if (ModularType.fits(shaping.stock(), form))
                stock.add(new ItemStack(item, shaping.cells()));
        }));
        builder.addInputSlot(1, 14).setStandardSlotBackground().addItemStacks(stock);
        builder.addInputSlot(1, 34).setStandardSlotBackground().addItemStack(new ItemStack(FItems.HAND_HAMMER.get()));
        builder.addOutputSlot(143, 62).setOutputSlotBackground()
                .addItemStack(new ItemStack(BuiltInRegistries.ITEM.get(shaping.item())));
    }

    @Override
    public void draw(Card card, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        ModularType.Shaping shaping = card.shaping();
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, BuiltInRegistries.ITEM.get(shaping.item()).getDescription().getString().toUpperCase(Locale.ROOT),
                1, 1, TITLE_COLOUR, false);
        graphics.fill(1, 11, WIDTH - 1, 12, 0x40000000);
        graphics.drawString(font, Component.translatable("feedback.jei.modular_part.stock", shaping.cells()), 24, 19, VALUE_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.modular_part.tool"), 24, 39, VALUE_COLOUR, false);
        for (int y = 0; y < shaping.shape().size(); y++)
            for (int x = 0; x < shaping.shape().get(y).length(); x++)
                if (shaping.shape().get(y).charAt(x) != ' ') {
                    int px = 112 + x * CELL;
                    int py = 16 + y * CELL;
                    graphics.fill(px, py, px + CELL - 1, py + CELL - 1, 0x30000000);
                }
        graphics.drawWordWrap(font, Component.translatable("feedback.jei.modular_part.note"), 1, 56, WIDTH - 50, LABEL_COLOUR);
    }
}
