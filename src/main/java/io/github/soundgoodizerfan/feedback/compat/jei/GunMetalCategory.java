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
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
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
 * The Gun Stand's spec sheet (spec 11, step 2): what each gun metal makes of one reference design.
 * Every design is its own, so there is no list of guns to show; the decision the Stand asks first
 * is the barrel's metal, and that is what a card answers -- a 9 mm, medium-barrelled,
 * standard-walled pistol baked in this metal, its chamber rating against a nominal charge of
 * gunpowder, the work it delivers and what it weighs. Requirements are free and exact (philosophy
 * 8); nothing here is a reading of anything the player owns.
 *
 * @param forms every item that is this metal: any one is a sample at the Stand.
 */
public class GunMetalCategory implements IRecipeCategory<GunMetalCategory.Card> {

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "gun_metal", Card.class);

    public record Card(ResourceLocation metal, List<Item> forms, GunFigures figures) {
    }

    private static final int WIDTH = 168, HEIGHT = 74;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626,
            OVER_COLOUR = 0xFFAA2222;

    private final IDrawable icon;

    public GunMetalCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FWeapons.GUN_STAND_ITEM.get()));
    }

    @Override
    public RecipeType<Card> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.gun_metal");
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
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 31).setStandardSlotBackground()
                .addItemStacks(card.forms().stream().map(ItemStack::new).toList());
    }

    @Override
    public void draw(Card card, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        GunFigures f = card.figures();
        graphics.drawString(font, Component.translatable("material." + card.metal().getNamespace() + "."
                + card.metal().getPath()).getString().toUpperCase(java.util.Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        graphics.fill(1, 11, WIDTH - 1, 12, 0x40000000);
        boolean over = f.peakPressure().value() > f.chamberRating().value();
        row(graphics, font, 16, "feedback.jei.gun_metal.rating", Readout.number(f.chamberRating().value()) + " / "
                + Readout.number(f.peakPressure().value()) + " Pu", over ? OVER_COLOUR : VALUE_COLOUR);
        row(graphics, font, 28, "feedback.jei.gun_metal.work", Readout.number(f.muzzleEnergy()) + " Work", VALUE_COLOUR);
        row(graphics, font, 40, "feedback.jei.gun_metal.mass", String.format(java.util.Locale.ROOT, "%.2f Mu",
                f.gunMass().value()), VALUE_COLOUR);
        graphics.drawString(font, Component.translatable("feedback.jei.gun_metal.note", FTuning.GUN_CHARGE_NOMINAL), 24, 58,
                LABEL_COLOUR, false);
    }

    private static void row(GuiGraphics graphics, Font font, int y, String key, String value, int colour) {
        graphics.drawString(font, Component.translatable(key), 24, y, LABEL_COLOUR, false);
        graphics.drawString(font, value, 96, y, colour, false);
    }
}
