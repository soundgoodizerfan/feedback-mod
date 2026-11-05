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

import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.GunOption;
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

/** A gun fitting: its slot, part and metal, and only the figures it changes. */
public class GunOptionCategory implements IRecipeCategory<GunOption> {

    static final RecipeType<GunOption> TYPE = RecipeType.create("feedback", "gun_option", GunOption.class);

    private static final int WIDTH = 168, HEIGHT = 80;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public GunOptionCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FWeapons.GUN_TABLE_ITEM.get()));
    }

    @Override
    public RecipeType<GunOption> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.gun_option");
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
    public void setRecipe(IRecipeLayoutBuilder builder, GunOption option, IFocusGroup focuses) {
        // text only: a fitting has no item of its own
    }

    @Override
    public void draw(GunOption o, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable(o.langKey()).getString().toUpperCase(Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("feedback.jei.gun_option.slot", o.slot()));
        lines.add(Component.translatable("feedback.jei.gun_option.part", o.part()));
        lines.add(Component.translatable("feedback.jei.gun_option.metal", fmt(o.metalMb())));
        o.reliefAt().ifPresent(r -> lines.add(Component.translatable("feedback.jei.gun_option.relief", pct(r))));
        if (o.bleed() != 0) lines.add(Component.translatable("feedback.jei.gun_option.bleed", pct(o.bleed())));
        if (o.gasCut() != 0) lines.add(Component.translatable("feedback.jei.gun_option.gas_cut", pct(o.gasCut())));
        if (o.blast() != 1) lines.add(Component.translatable("feedback.jei.gun_option.blast", fmt(o.blast())));
        if (o.heat() != 1) lines.add(Component.translatable("feedback.jei.gun_option.heat", fmt(o.heat())));
        if (o.shed() != 1) lines.add(Component.translatable("feedback.jei.gun_option.shed", fmt(o.shed())));
        if (o.eddy() != 0) lines.add(Component.translatable("feedback.jei.gun_option.eddy", pct(o.eddy())));
        if (!o.trait().isEmpty()) lines.add(Component.translatable("feedback.jei.gun_option.trait", o.trait().replace('_', ' ')));
        for (int i = 0; i < lines.size(); i++)
            graphics.drawString(font, lines.get(i), 1 + (i / 6) * 84, 14 + (i % 6) * 11, VALUE_COLOUR, false);
    }

    private static String pct(float f) {
        return String.format(Locale.ROOT, "%.0f%%", f * 100);
    }

    private static String fmt(float f) {
        return String.format(Locale.ROOT, "%.2f", f);
    }
}
