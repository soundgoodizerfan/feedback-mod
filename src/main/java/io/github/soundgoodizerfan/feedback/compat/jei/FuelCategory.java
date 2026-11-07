// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
public class FuelCategory implements IRecipeCategory<FuelCategory.Card> {

    public record Card(ResourceLocation id, Material material) {
    }

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "fuel", Card.class);

    private static final int WIDTH = 168, HEIGHT = 70, ROW = 11;

    private final IDrawable icon;

    public FuelCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.FIREBOX.get()));
    }

    public static List<Card> cards(Map<ResourceLocation, Material> materials) {
        List<Card> out = new ArrayList<>();
        materials.forEach((id, m) -> {
            if (m.combustion().flatMap(c -> c.flameTemperature()).isPresent() && (!m.items(id).isEmpty() || liquid(id) != null))
                out.add(new Card(id, m));
        });
        return out;
    }

    /** The liquid (not gas) a material is, by its id, or null: a fuel with no item form is poured, not stoked. */
    private static net.minecraft.world.level.material.Fluid liquid(ResourceLocation id) {
        var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(id);
        return fluid == net.minecraft.world.level.material.Fluids.EMPTY || fluid.getFluidType().isLighterThanAir() ? null : fluid;
    }

    @Override
    public RecipeType<Card> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.fuel");
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
        var slot = builder.addInputSlot(WIDTH - 19, 1).setStandardSlotBackground();
        if (!items.isEmpty())
            slot.addItemStacks(items.stream().map(ItemStack::new).toList());
        var fluid = liquid(card.id());
        if (fluid != null)
            slot.addFluidStack(fluid, 1000);
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
        var c = m.combustion().orElseThrow();
        row(graphics, font, y, "feedback.jei.fuel.flame", Readout.number(c.flameTemperature().get().value()) + " Tu");
        y += ROW;
        if (c.burnTicks().isPresent()) {
            row(graphics, font, y, "feedback.jei.fuel.burn", c.burnTicks().get() + " t (" + Readout.number(c.burnTicks().get() / 20f) + " s)");
            y += ROW;
        }
        row(graphics, font, y, "feedback.jei.fuel.spread", Readout.number(c.spread()));
    }
}
