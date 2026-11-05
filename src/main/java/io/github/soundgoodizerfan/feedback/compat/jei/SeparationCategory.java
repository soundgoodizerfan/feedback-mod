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

import io.github.soundgoodizerfan.feedback.client.Readout;
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

/**
 * str(object='') -> str
str(bytes_or_buffer[, encoding[, errors]]) -> str

Create a new string object from the given object. If encoding or
errors is specified, then the object must expose a data buffer
that will be decoded using the given encoding and error handler.
Otherwise, returns the result of object.__str__() (if defined)
or repr(object).
encoding defaults to 'utf-8'.
errors defaults to 'strict'.
 */
public class SeparationCategory implements IRecipeCategory<SeparationCategory.Card> {

    public record Card(ResourceLocation id, Material material) {
    }

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "separation", Card.class);

    private static final int WIDTH = 168, HEIGHT = 82, ROW = 11;

    private final IDrawable icon;

    public SeparationCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.ELECTROMAGNET.get()));
    }

    public static List<Card> cards(Map<ResourceLocation, Material> materials) {
        List<Card> out = new ArrayList<>();
        materials.forEach((id, m) -> {
            if (m.separation().magnetic() || m.separation().reflectivity().isPresent() && !m.items(id).isEmpty())
                out.add(new Card(id, m));
        });
        return out;
    }

    @Override
    public RecipeType<Card> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.separation");
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

    private static void row(GuiGraphics graphics, Font font, int y, String labelKey, String value) {
        graphics.drawString(font, Component.translatable(labelKey), 1, y, 0xFF7A7A7A, false);
        graphics.drawString(font, value, 84, y, 0xFF262626, false);
    }

    @Override
    public void draw(Card card, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("material." + card.id().getNamespace() + "." + card.id().getPath())
                .getString().toUpperCase(Locale.ROOT), 1, 1, 0xFF3F3F3F, false);
        graphics.fill(1, 22, WIDTH - 1, 23, 0x40000000);
        Material m = card.material();
        int y = 28;
        row(graphics, font, y, "feedback.jei.separation.magnet", Component.translatable(m.separation().magnetic()
                ? "feedback.jei.separation.pulled" : "feedback.jei.separation.ignored").getString());
        y += ROW;
        if (m.separation().magnetic() && m.separation().curieTemperature().isPresent()) {
            row(graphics, font, y, "feedback.jei.separation.curie", Readout.number(m.separation().curieTemperature().get().value()) + " Tu");
            y += ROW;
        }
        if (m.separation().reflectivity().isPresent()) {
            row(graphics, font, y, "feedback.jei.separation.reflectivity", Readout.number(m.separation().reflectivity().get()));
            y += ROW;
        }
        row(graphics, font, y, "feedback.jei.separation.density", Readout.number(m.density().muPerMb()) + " Mu/mB");
        y += ROW;
        io.github.soundgoodizerfan.feedback.logistics.sort.Reading r = io.github.soundgoodizerfan.feedback.logistics.sort.SortReader.of(m);
        StringBuilder bands = new StringBuilder();
        for (io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty p : new io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty[]{
                io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty.REFLECTIVITY, io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty.DENSITY,
                io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty.HARDNESS, io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty.KIND})
            bands.append(bands.length() == 0 ? "" : ", ").append(Component.translatable("feedback.sort.band." + io.github.soundgoodizerfan.feedback.logistics.sort.SortReader.bandOf(r, p).key()).getString());
        row(graphics, font, y, "feedback.jei.separation.sorts_as", bands.toString());
    }
}
