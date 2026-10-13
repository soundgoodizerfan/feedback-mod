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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.compat.jei;

import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * What the Reloading Press does with a propellant. The press names no cartridge -- it seats
 * whatever is in it -- so there is no recipe to list; what a player can look up is what each
 * charge is: the pressure a nominal charge makes and how fast it burns. One card per propellant,
 * the case and bullet slots cycling through every one the material table has, the output a
 * reference round of that charge.
 *
 * @param charge  the items that are this propellant, from its material's forms.
 * @param cases   every case form, every bullet form: what the other two slots accept.
 */
public class PressCategory implements IRecipeCategory<PressCategory.Card> {

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "reloading_press", Card.class);

    public record Card(ResourceLocation propellant, List<Item> charge, List<Item> cases, List<Item> bullets,
                       float peakPressure, float burnRate) {
    }

    private static final int WIDTH = 168, HEIGHT = 74;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public PressCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FWeapons.RELOADING_PRESS_ITEM.get()));
    }

    @Override
    public RecipeType<Card> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.reloading_press");
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
        builder.addInputSlot(1, 13).setStandardSlotBackground().addItemStacks(card.cases().stream().map(ItemStack::new).toList());
        builder.addInputSlot(1, 31).setStandardSlotBackground()
                .addItemStacks(card.charge().stream().map(i -> new ItemStack(i, FTuning.GUN_CHARGE_NOMINAL)).toList());
        builder.addInputSlot(1, 49).setStandardSlotBackground().addItemStacks(card.bullets().stream().map(ItemStack::new).toList());
        ItemStack round = new ItemStack(FWeapons.CARTRIDGE_ITEM.get());
        Cartridge reference = Cartridge.reference(9);
        round.set(FWeapons.CARTRIDGE.get(), new Cartridge(9, reference.caseMaterial(), card.propellant(),
                FTuning.GUN_CHARGE_NOMINAL, reference.projectile()));
        builder.addOutputSlot(143, 31).setOutputSlotBackground().addItemStack(round);
    }

    @Override
    public void draw(Card card, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("material." + card.propellant().getNamespace() + "."
                + card.propellant().getPath()).getString().toUpperCase(java.util.Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        graphics.fill(1, 11, WIDTH - 1, 12, 0x40000000);
        row(graphics, font, 18, "feedback.jei.press.peak", Readout.number(card.peakPressure()) + " Pu");
        row(graphics, font, 30, "feedback.jei.press.burn", Readout.number(card.burnRate()) + " per m");
        row(graphics, font, 42, "feedback.jei.press.charge", FTuning.GUN_CHARGE_NOMINAL + " nominal, " + FTuning.GUN_CHARGE_MAX + " most");
        graphics.drawString(font, Component.translatable("feedback.jei.press.note"), 24, 58, LABEL_COLOUR, false);
    }

    private static void row(GuiGraphics graphics, Font font, int y, String key, String value) {
        graphics.drawString(font, Component.translatable(key), 24, y, LABEL_COLOUR, false);
        graphics.drawString(font, value, 84, y, VALUE_COLOUR, false);
    }
}
