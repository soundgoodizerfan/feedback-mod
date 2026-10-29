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

import java.util.Locale;

import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.geology.Basin;
import io.github.soundgoodizerfan.feedback.registry.FFluids;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A fluid vein card: a basin kind (petrochemistry spec 2.3) -- the fluid it holds, its seep on the
 * surface, and where, how big, how full and how hard it drives. Output slots, so looking up crude
 * oil shows where it is found.
 */
public class BasinCategory implements IRecipeCategory<Basin> {

    static final RecipeType<Basin> TYPE = RecipeType.create("feedback", "basin", Basin.class);

    private static final int WIDTH = 168, HEIGHT = 108;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public BasinCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.COPPER_BUCKET.get()));
    }

    @Override
    public RecipeType<Basin> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.basin");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Basin basin, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, 1, 14).setOutputSlotBackground()
                .addFluidStack(FFluids.CRUDE_OIL.get(), 1000);
        basin.indicator().ifPresent(id -> builder.addOutputSlot(WIDTH - 19, 14).setOutputSlotBackground()
                .addItemStack(new ItemStack(BuiltInRegistries.ITEM.get(id))));
    }

    @Override
    public void draw(Basin basin, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, basin.kind().replace('_', ' ').toUpperCase(Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        int y = 36;
        row(graphics, font, y, "feedback.jei.basin.depth", "Y " + basin.minY() + " .. " + basin.maxY());
        y += 10;
        row(graphics, font, y, "feedback.jei.basin.size", basin.radius() + " x " + basin.thickness() + " blocks");
        y += 10;
        row(graphics, font, y, "feedback.jei.basin.weight", Integer.toString(basin.weight()));
        y += 10;
        row(graphics, font, y, "feedback.jei.basin.volume", Readout.number(basin.volumeMb()) + " mB");
        y += 10;
        row(graphics, font, y, "feedback.jei.basin.drive", Readout.number(basin.drivePu()) + " Pu");
        y += 10;
        row(graphics, font, y, "feedback.jei.basin.density", Readout.number(basin.crude().density()) + " kg/L");
        y += 10;
        row(graphics, font, y, "feedback.jei.basin.sulfur", Readout.number(basin.crude().sulfur() * 100f) + "%");
        y += 10;
        row(graphics, font, y, "feedback.jei.basin.light", Readout.number(basin.crude().lightFraction() * 100f) + "%");
        y += 10;
        row(graphics, font, y, "feedback.jei.basin.dimension", String.join(", ",
                basin.dimensions().stream().map(ResourceLocation::getPath).toList()));
        if (basin.indicator().isPresent()) {
            Component seep = Component.translatable("feedback.jei.basin.seep");
            graphics.drawString(font, seep, WIDTH - 23 - font.width(seep), 19, LABEL_COLOUR, false);
        }
    }

    private static void row(GuiGraphics graphics, Font font, int y, String labelKey, String value) {
        graphics.drawString(font, Component.translatable(labelKey), 1, y, LABEL_COLOUR, false);
        graphics.drawString(font, value, 60, y, VALUE_COLOUR, false);
    }
}
