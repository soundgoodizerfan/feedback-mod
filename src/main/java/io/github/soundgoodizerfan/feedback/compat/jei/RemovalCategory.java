// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import io.github.soundgoodizerfan.feedback.process.Removal;
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
 * {@link DeformationCategory}'s own shape, for the removal family -- a separate category rather
 * than a second entry in that one, because turning/drilling/grinding subtract material and
 * deformation moves it, and {@code CLAUDE.md} treats that as a real physical difference, not a
 * cosmetic one. See {@link Removal}'s own class doc for the fuller account of why this category
 * did not exist until now.
 *
 * <h2>The icon is the waste, not a machine</h2>
 * Three machines answer for this category (Drill Press, Lathe, Grinding Wheel -- see
 * {@code FeedbackJeiPlugin#registerRecipeCatalysts}), and none of them is "the" removal machine.
 * Swarf ({@link the generated {@code feedback:steel_swarf}} for now) is what every one of them actually produces, so it names the family
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

    private final IDrawable icon;

    public RemovalCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(io.github.soundgoodizerfan.feedback.Feedback.id("steel_swarf"))));
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
        return WorkCard.WIDTH;
    }

    @Override
    public int getHeight() {
        return WorkCard.HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Removal recipe, IFocusGroup focuses) {
        WorkCard.slots(builder, recipe.input(), recipe.result());
        if (!recipe.swarf().isEmpty())
            builder.addOutputSlot(1, 79).setOutputSlotBackground().addItemStack(recipe.swarf());
    }

    @Override
    public void draw(Removal recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        WorkCard.draw(graphics, recipe.operation(), recipe.input(), recipe.work(),
                "feedback.jei.tool_must_beat", recipe.hardness(), recipe.result(),
                recipe.swarf().isEmpty() ? null : "feedback.jei.sheds", recipe.swarf().getHoverName().getString());
    }
}
