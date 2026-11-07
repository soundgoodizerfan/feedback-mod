// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.Locale;

import io.github.soundgoodizerfan.feedback.village.Good;
import io.github.soundgoodizerfan.feedback.village.VillageEconomy;
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
import net.minecraft.world.item.Items;

/** A villager's shop line: what it sells, for how many emeralds, from which trade level and standing. */
public class GoodsCategory implements IRecipeCategory<Good> {

    static final RecipeType<Good> TYPE = RecipeType.create("feedback", "goods", Good.class);

    private static final int WIDTH = 168, HEIGHT = 52;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public GoodsCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.EMERALD));
    }

    @Override
    public RecipeType<Good> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.goods");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Good good, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 22).setStandardSlotBackground().addItemStack(new ItemStack(Items.EMERALD, good.price()));
        builder.addOutputSlot(143, 22).setOutputSlotBackground().addItemStack(good.enchantedBook() ? new ItemStack(Items.ENCHANTED_BOOK) : good.stack());
    }

    @Override
    public void draw(Good good, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, seller(good).getString().toUpperCase(Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.goods.price", good.price()), 24, 14, VALUE_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.goods.level", good.level()), 24, 25, LABEL_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.goods.stock", good.stock()), 24, 36, LABEL_COLOUR, false);
        if (good.standing() > 0)
            graphics.drawString(font, Component.translatable("feedback.jei.goods.standing", good.standing()), 76, 36, LABEL_COLOUR, false);
        if (good.blueprint().isPresent())
            graphics.drawString(font, Component.translatable("feedback.jei.goods.blueprint", good.blueprint().get().getPath().replace('_', ' ')), 24, 47, LABEL_COLOUR, false);
        if (good.enchantedBook())
            graphics.drawString(font, Component.translatable("feedback.jei.goods.book"), 24, 47, LABEL_COLOUR, false);
    }

    private static Component seller(Good good) {
        if (good.profession().equals(VillageEconomy.WANDERER))
            return Component.translatable("entity.minecraft.wandering_trader");
        return Component.translatable("entity." + good.profession().getNamespace() + ".villager." + good.profession().getPath());
    }
}
