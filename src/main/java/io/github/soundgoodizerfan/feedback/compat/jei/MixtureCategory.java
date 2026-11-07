// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.Locale;

import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.process.Mixture;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.CartridgeItem;
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

/** A mixture card: the window each part must sit in, how hot it may be, and what it resolves into. The Mixer, the Reloading Press and a crucible's melt ledger ask this table. */
public class MixtureCategory implements IRecipeCategory<Mixture> {

    static final RecipeType<Mixture> TYPE = RecipeType.create("feedback", "mixture", Mixture.class);

    private static final int WIDTH = 168, HEIGHT = 84;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public MixtureCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FWeapons.MIXER_ITEM.get()));
    }

    @Override
    public RecipeType<Mixture> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.mixture");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Mixture recipe, IFocusGroup focuses) {
        // Parts are material amounts, not items: each part's slot cycles every item of that material, so a lookup on any of them lists the mix under Uses.
        var materials = io.github.soundgoodizerfan.feedback.process.ClientMaterials.get();
        int x = 1;
        for (var id : recipe.parts().keySet()) {
            var material = materials.get(id);
            if (material == null || material.items(id).isEmpty())
                continue;
            builder.addInputSlot(x, 62).setStandardSlotBackground()
                    .addItemStacks(material.items(id).values().stream().map(ItemStack::new).toList());
            x += 18;
        }
        if (recipe.hasFluidResult())
            builder.addOutputSlot(143, 22).setOutputSlotBackground()
                    .addFluidStack(recipe.resultFluid().getFluid(), recipe.resultFluid().getAmount()).setFluidRenderer(Math.max(1, recipe.resultFluid().getAmount()), false, 16, 16);
        else
            builder.addOutputSlot(143, 22).setOutputSlotBackground().addItemStack(recipe.result());
    }

    @Override
    public void draw(Mixture recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, (recipe.hasFluidResult() ? recipe.resultFluid().getHoverName() : recipe.result().getHoverName()).getString().toUpperCase(Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        int y = 13;
        for (var part : recipe.parts().entrySet()) {
            graphics.drawString(font, Math.round(part.getValue().min() * 100) + "-" + Math.round(part.getValue().max() * 100) + "%", 1, y, VALUE_COLOUR, false);
            graphics.drawString(font, CartridgeItem.name(part.getKey()), 40, y, VALUE_COLOUR, false);
            y += 9;
        }
        graphics.drawString(font, recipe.stray() > 0 ? Component.translatable("feedback.jei.mixture.stray", Math.round(recipe.stray() * 100)) : Component.translatable("feedback.jei.mixture.pure"), 1, 49, LABEL_COLOUR, false);
        graphics.drawString(font, band(recipe), 90, 49, LABEL_COLOUR, false);
    }

    private static String band(Mixture recipe) {
        boolean floor = recipe.minTemperature().value() > -Float.MAX_VALUE;
        boolean ceiling = recipe.maxTemperature().value() < Float.MAX_VALUE;
        String min = Readout.number(recipe.minTemperature().value()), max = Readout.number(recipe.maxTemperature().value());
        if (floor && ceiling)
            return min + "-" + max + " Tu";
        if (floor)
            return ">= " + min + " Tu";
        if (ceiling)
            return "<= " + max + " Tu";
        return Component.translatable("feedback.jei.dip.any").getString();
    }
}
