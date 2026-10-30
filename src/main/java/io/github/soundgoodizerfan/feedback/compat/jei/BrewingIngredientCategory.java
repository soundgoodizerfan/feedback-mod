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
import java.util.Map;

import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.process.CompoundTable;
import io.github.soundgoodizerfan.feedback.process.Material;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;

/**
 * What a plant gives the Brewing Stand: its family and the way it heads. Never a node, a position or a route (the table is client side and sees
 * all of it); the shape it walks is on the shape cards. One card per material that carries a {@code potion} block, found by the material's data; the slot is every item made of it.
 */
public class BrewingIngredientCategory implements IRecipeCategory<BrewingIngredientCategory.Card> {

    public record Card(ResourceLocation id, Material material) {
    }

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "brewing_ingredient", Card.class);

    private static final int WIDTH = 168, HEIGHT = 110, ROW_HEIGHT = 9;

    private final IDrawable icon;

    public BrewingIngredientCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.BREWING_STAND.get()));
    }

    public static List<Card> cards(Map<ResourceLocation, Material> materials) {
        List<Card> out = new ArrayList<>();
        materials.forEach((id, m) -> {
            if (m.potion().isPresent() && !m.items(id).isEmpty())
                out.add(new Card(id, m));
        });
        return out;
    }

    /** The compass point a heading in degrees (0 is east, 90 north) falls nearest, as a lang key tail. */
    static String point(float degrees) {
        return POINTS[Math.floorMod(Math.round(degrees / 45f), 8)];
    }

    private static final String[] POINTS = {"east", "north_east", "north", "north_west", "west", "south_west", "south", "south_east"};

    @Override
    public RecipeType<Card> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.brewing_ingredient");
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
        List<Item> items = new ArrayList<>(card.material().items(card.id()).values());
        builder.addInputSlot(WIDTH - 19, 1).setStandardSlotBackground().addItemStacks(items.stream().map(ItemStack::new).toList());
    }

    @Override
    public void draw(Card card, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("material." + card.id().getNamespace() + "." + card.id().getPath())
                .getString().toUpperCase(Locale.ROOT), 1, 1, 0xFF3F3F3F, false);
        graphics.fill(1, 22, WIDTH - 1, 23, 0x40000000);
        Material.Potion plant = card.material().potion().orElseThrow();
        int y = 28;
        graphics.drawString(font, Component.translatable("feedback.jei.brewing_ingredient.family"), 1, y, 0xFF7A7A7A, false);
        Component family = Component.translatable("compound.feedback." + plant.family());
        graphics.drawString(font, family, WIDTH - 1 - font.width(family), y, 0xFF262626, false);
        int dot = CompoundTable.get().find(plant.family()).flatMap(c -> c.color()).orElse(-1);
        if (dot >= 0)
            graphics.fill(WIDTH - 1 - font.width(family) - 8, y + 1, WIDTH - 1 - font.width(family) - 2, y + 7, 0xFF000000 | dot);
        y += ROW_HEIGHT;
        graphics.drawString(font, Component.translatable("feedback.jei.brewing_ingredient.heading", Component.translatable("feedback.jei.heading." + point(plant.heading()))), 1, y, 0xFF262626, false);
        y += ROW_HEIGHT;
        if (plant.pull().isPresent()) {
            graphics.drawString(font, Component.translatable("feedback.jei.brewing_ingredient.steadies." + (plant.pull().get().axisX() ? "x" : "y")), 1, y, 0xFF262626, false);
            y += ROW_HEIGHT;
        }
        y += 4;
        for (FormattedCharSequence line : font.split(Component.translatable("feedback.jei.brewing_ingredient.note"), WIDTH - 2)) {
            graphics.drawString(font, line, 1, y, 0xFF3F3F3F, false);
            y += ROW_HEIGHT;
        }
    }
}
