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
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.core.unit.Unit;
import io.github.soundgoodizerfan.feedback.core.unit.Units;
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
 * A material's own stats, straight off its {@code material/*.json}: nothing derived, nothing
 * rolled up. The slot is every item made of it (a "uses" lookup on any of them lands here); a
 * stat the file leaves out is left off the card.
 */
public class MaterialStatsCategory implements IRecipeCategory<MaterialStatsCategory.Card> {

    /** One material; equality is the record's, so a republish of an unchanged table keeps its cards. */
    public record Card(ResourceLocation id, Material material) {
    }

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "material_stats", Card.class);

    private static final int WIDTH = 168, HEIGHT = 190;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public MaterialStatsCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.CALIPERS.get()));
    }

    /** Only materials with an item to point at get a card. */
    public static List<Card> cards(Map<ResourceLocation, Material> materials) {
        List<Card> out = new ArrayList<>();
        materials.forEach((id, m) -> {
            if (!m.items(id).isEmpty())
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
        return Component.translatable("feedback.jei.material_stats");
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
        builder.addInputSlot(WIDTH - 19, 1).setStandardSlotBackground()
                .addItemStacks(items.stream().map(ItemStack::new).toList());
    }

    private static void add(List<String[]> rows, String key, Optional<? extends Unit> unit) {
        unit.ifPresent(u -> rows.add(new String[] {key, Units.figure(u).getString()}));
    }

    private static void number(List<String[]> rows, String key, Optional<? extends Number> value) {
        value.ifPresent(v -> rows.add(new String[] {key, Readout.number(v.floatValue())}));
    }

    /** Label key suffix and printed value, in display order. */
    static List<String[]> rows(Material m) {
        List<String[]> rows = new ArrayList<>();
        m.kind().ifPresent(k -> rows.add(new String[] {"kind", k.getSerializedName()}));
        rows.add(new String[] {"hardness", Units.figure(m.hardness()).getString()});
        rows.add(new String[] {"density", Units.figure(m.density()).getString()});
        var t = m.thermal();
        add(rows, "melting_point", t.meltingPoint());
        add(rows, "boiling_point", t.boilingPoint());
        add(rows, "anneal", t.annealTemperature());
        add(rows, "service", t.serviceTemperature());
        add(rows, "oxidation", t.oxidationTemperature());
        add(rows, "thermal_conductivity", t.conductivity());
        add(rows, "cooling_rate", t.coolingRate());
        var mech = m.mechanical();
        add(rows, "toughness", mech.toughness());
        number(rows, "ductility", mech.ductility());
        add(rows, "yield", mech.yieldStrength());
        number(rows, "friction", mech.friction());
        number(rows, "abrasiveness", mech.abrasiveness());
        if (mech.hardenable())
            rows.add(new String[] {"hardenable", Component.translatable("feedback.jei.material.yes").getString()});
        number(rows, "electrical_conductivity", m.electrical().conductivity());
        number(rows, "corrosion", m.corrosionResistance());
        m.combustion().ifPresent(c -> {
            add(rows, "flame", c.flameTemperature());
            add(rows, "flash_point", c.flashPoint());
            number(rows, "burn_ticks", c.burnTicks());
        });
        m.propellant().ifPresent(p -> add(rows, "ignition", Optional.of(p.ignitionTemperature())));
        m.catalyst().ifPresent(c -> number(rows, "catalyst_activity", Optional.of(c.activity())));
        return rows;
    }

    @Override
    public void draw(Card card, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("material." + card.id().getNamespace() + "." + card.id().getPath())
                .getString().toUpperCase(Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        graphics.fill(1, 22, WIDTH - 1, 23, 0x40000000);
        int y = 28;
        // shortcut: 18 rows fit the card; a material with more stats loses the tail, widen HEIGHT if one does.
        for (String[] row : rows(card.material()).stream().limit(18).toList()) {
            graphics.drawString(font, Component.translatable("feedback.jei.material." + row[0]), 1, y, LABEL_COLOUR, false);
            graphics.drawString(font, row[1], 90, y, VALUE_COLOUR, false);
            y += 9;
        }
    }
}
