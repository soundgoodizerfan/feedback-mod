// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import io.github.soundgoodizerfan.feedback.weapon.GunBlueprintPool;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import io.github.soundgoodizerfan.feedback.registry.FModular;

/** A gun blueprint that turns up in chests, and in skeletons' hands if the gun is an early one. */
public class GunBlueprintPoolCategory implements IRecipeCategory<GunBlueprintPool.Listing> {

    static final RecipeType<GunBlueprintPool.Listing> TYPE =
            RecipeType.create("feedback", "gun_blueprint_pool", GunBlueprintPool.Listing.class);

    private static final int WIDTH = 168, HEIGHT = 30;

    private final IDrawable icon;

    public GunBlueprintPoolCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FModular.BLUEPRINT.get()));
    }

    @Override
    public RecipeType<GunBlueprintPool.Listing> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.gun_blueprint_pool");
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
    public void setRecipe(IRecipeLayoutBuilder builder, GunBlueprintPool.Listing entry, IFocusGroup focuses) {
        builder.addOutputSlot(143, 6).setOutputSlotBackground().addItemStack(GunBlueprintPool.blueprint(entry.design()));
    }

    @Override
    public void draw(GunBlueprintPool.Listing entry, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("feedback.jei.gun_blueprint_pool.chests"), 1, 5, 0xFF262626, false);
        if (entry.early())
            graphics.drawString(font, Component.translatable("feedback.jei.gun_blueprint_pool.early"), 1, 17, 0xFF7A7A7A, false);
    }
}
