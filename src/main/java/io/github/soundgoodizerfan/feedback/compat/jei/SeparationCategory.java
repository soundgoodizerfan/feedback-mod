// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.client.tooltip.Readout;
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

    private static final int WIDTH = 168, HEIGHT = 160, ROW = 11;

    private final IDrawable icon;

    public SeparationCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(io.github.soundgoodizerfan.feedback.registry.FModular.ELECTROMAGNET.get()));
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

    /** Lines the last row used: a row whose value will not sit beside its label puts it on a second line. */
    private static int lastLines = 1;

    private static void row(GuiGraphics graphics, Font font, int y, String labelKey, String value) {
        lastLines = CardText.row(graphics, font, WIDTH, 1, y, Component.translatable(labelKey), value, 0xFF7A7A7A, 0xFF262626);
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
        y += ROW * lastLines;
        if (m.separation().magnetic() && m.separation().curieTemperature().isPresent()) {
            row(graphics, font, y, "feedback.jei.separation.curie", Readout.number(m.separation().curieTemperature().get().value()) + " Tu");
            y += ROW * lastLines;
        }
        if (m.separation().reflectivity().isPresent()) {
            row(graphics, font, y, "feedback.jei.separation.reflectivity", Readout.number(m.separation().reflectivity().get()));
            y += ROW * lastLines;
        }
        row(graphics, font, y, "feedback.jei.separation.density", Readout.number(m.density().muPerMb()) + " Mu/mB");
        y += ROW * lastLines;
        io.github.soundgoodizerfan.feedback.logistics.sort.Reading r = io.github.soundgoodizerfan.feedback.logistics.sort.SortReader.of(m);
        StringBuilder bands = new StringBuilder();
        for (io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty p : new io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty[]{
                io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty.REFLECTIVITY, io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty.DENSITY,
                io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty.HARDNESS, io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty.KIND})
            bands.append(bands.length() == 0 ? "" : ", ").append(Component.translatable("feedback.sort.band." + io.github.soundgoodizerfan.feedback.logistics.sort.SortReader.bandOf(r, p).key()).getString());
        row(graphics, font, y, "feedback.jei.separation.sorts_as", bands.toString());
        // An ore: what each separator reports to the concentrate per pass, from its contrast with its host rock.
        if (m.item(card.id(), "concentrate").isPresent()) {
            y += ROW * lastLines;
            var materials = io.github.soundgoodizerfan.feedback.process.MaterialIngredient.materialsNow();
            var gangue = io.github.soundgoodizerfan.feedback.process.OreLadder.gangueOf(m, materials);
            ResourceLocation host = m.separation().gangue().orElse(io.github.soundgoodizerfan.feedback.process.OreLadder.DEFAULT_GANGUE);
            row(graphics, font, y, "feedback.jei.separation.gangue", Component.translatable("material." + host.getNamespace() + "." + host.getPath()).getString());
            for (var mode : io.github.soundgoodizerfan.feedback.process.OreLadder.Mode.values()) {
                y += ROW * lastLines;
                float dust = gangue.map(g -> io.github.soundgoodizerfan.feedback.process.OreLadder.share(mode, m, g, "dust")).orElse(0f);
                float crushed = gangue.map(g -> io.github.soundgoodizerfan.feedback.process.OreLadder.share(mode, m, g, "crushed")).orElse(0f);
                row(graphics, font, y, "feedback.jei.separation.ore." + mode.key(), dust <= 0f
                        ? Component.translatable("feedback.jei.separation.ore.none").getString()
                        : Component.translatable("feedback.jei.separation.ore.share", Math.round(dust * 100f), Math.round(crushed * 100f)).getString());
            }
        }
    }
}
