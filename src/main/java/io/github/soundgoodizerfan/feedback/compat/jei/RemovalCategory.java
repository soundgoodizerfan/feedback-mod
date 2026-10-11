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

import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.process.Removal;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * {@link DeformationCategory}'s own shape, for the removal family -- a separate category rather
 * than a second entry in that one, because turning/drilling/grinding subtract material and
 * deformation moves it, and {@code CLAUDE.md} treats that as a real physical difference, not a
 * cosmetic one. See {@link Removal}'s own class doc for the fuller account of why this category
 * did not exist until now.
 *
 * <h2>The icon is the waste, not a machine</h2>
 * Three machines answer for this category (Drill Press, Lathe, Grinding Wheel -- see
 * {@code FeedbackJeiPlugin#registerRecipeCatalysts}), and none of them is "the" removal machine.
 * {@link FItems#METAL_SWARF} is what every one of them actually produces, so it names the family
 * without picking a favourite -- the same reasoning that keeps {@code Operation}'s values named
 * after the action rather than the block.
 *
 * <h2>Operation is a row now, not just a category name</h2>
 * {@link DeformationCategory}'s own doc used to argue the operation name belongs on the category
 * tab and not the card, since "the card is about copper." That was wrong the moment two machines
 * could answer the same category (as they now both can) -- a player looking at a list of removal
 * cards has no way to tell a Lathe recipe from a Grinding Wheel one without this row. Both
 * categories print it now.
 */
public class RemovalCategory implements IRecipeCategory<Removal> {

    static final RecipeType<Removal> TYPE = RecipeType.create("feedback", "material_removal", Removal.class);

    private static final int WIDTH = 168;
    private static final int HEIGHT = 78;

    private static final int LABEL_X = 24;
    private static final int VALUE_X = 76;

    private static final int TITLE_COLOUR = 0xFF3F3F3F;
    private static final int LABEL_COLOUR = 0xFF7A7A7A;
    private static final int VALUE_COLOUR = 0xFF262626;
    private static final int RULE_COLOUR = 0x40000000;

    private final IDrawable icon;

    public RemovalCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.METAL_SWARF.get()));
    }

    @Override
    public RecipeType<Removal> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.material_removal");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Removal recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 13).setStandardSlotBackground().addIngredients(recipe.input());
        builder.addOutputSlot(1, 57).setOutputSlotBackground().addItemStack(recipe.result());
    }

    @Override
    public void draw(Removal recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, recipe.result().getHoverName().getString().toUpperCase(java.util.Locale.ROOT),
                1, 1, TITLE_COLOUR, false);
        graphics.fill(1, 11, WIDTH - 1, 12, RULE_COLOUR);

        ItemStack[] accepted = recipe.input().getItems();
        row(graphics, font, 18, "feedback.jei.operation", Component.translatable(recipe.operation().translationKey()).getString());
        row(graphics, font, 30, "feedback.jei.input", quantity(accepted.length == 0 ? ItemStack.EMPTY : accepted[0]));
        row(graphics, font, 42, "feedback.jei.work", recipe.work() + " Fu");
        row(graphics, font, 54, "feedback.jei.hardness", Readout.number(recipe.hardness().value()));
        row(graphics, font, 66, "feedback.jei.output", quantity(recipe.result()));
    }

    private static void row(GuiGraphics graphics, Font font, int y, String labelKey, String value) {
        graphics.drawString(font, Component.translatable(labelKey), LABEL_X, y, LABEL_COLOUR, false);
        graphics.drawString(font, value, VALUE_X, y, VALUE_COLOUR, false);
    }

    private static String quantity(ItemStack stack) {
        return stack.isEmpty() ? "-" : stack.getCount() + " × " + stack.getHoverName().getString();
    }
}
