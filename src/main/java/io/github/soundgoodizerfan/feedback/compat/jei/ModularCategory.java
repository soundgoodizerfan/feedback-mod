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

import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * How a modular item is put together: one card per type per pattern, since the pattern is the
 * design (a longer handle, an optional guard, a tip are different patterns, spec 3). Down the
 * left, a slot for each role the pattern has, cycling through everything that fits it; on the
 * right, the pattern as the grid lays it out. Below, the options that are not part of the
 * pattern: the set-in tip or studs, regrinding the edge at a grindstone, and plating.
 * <p>
 * Built from the client's copy of the {@link ModularType}s and the material table, so a pack's
 * own type or material shows with no edit, and no item is named (philosophy 11).
 */
public class ModularCategory implements IRecipeCategory<ModularCategory.Card> {

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "modular_assembly", Card.class);

    /** One pattern of one type; the figures are read from the type's file when drawn. */
    public record Card(ResourceLocation type, int pattern) {
    }

    private static final int WIDTH = 168, HEIGHT = 184;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;
    private static final int CELL = 10;

    private final IDrawable icon;

    public ModularCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FModular.BLUEPRINT_TABLE_ITEM.get()));
    }

    @Override
    public RecipeType<Card> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.modular_assembly");
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

    /** The roles a pattern has, in the type's order. */
    private static List<String> roles(ModularType type, int pattern) {
        Map<String, Integer> counts = type.counts(pattern);
        return type.order().stream().filter(counts::containsKey).toList();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Card card, IFocusGroup focuses) {
        ModularType type = ModularTypes.known().get(card.type());
        if (type == null)
            return;
        Map<ResourceLocation, Material> materials = ClientMaterials.get();
        List<String> roles = roles(type, card.pattern());
        for (int i = 0; i < roles.size(); i++)
            builder.addInputSlot(1, 14 + 20 * i).setStandardSlotBackground()
                    .addItemStacks(stacks(type.roles().get(roles.get(i)), materials));
        builder.addOutputSlot(143, 62).setOutputSlotBackground()
                .addItemStack(new ItemStack(BuiltInRegistries.ITEM.get(card.type())));
    }

    /** What fits a role: the parts of every material for a shaped one, every item of a form it takes for stock. */
    static List<ItemStack> stacks(ModularType.Role role, Map<ResourceLocation, Material> materials) {
        List<ItemStack> out = new ArrayList<>();
        materials.forEach((id, m) -> {
            if (role.part().isPresent()) {
                ModularType.Shaping shaping = role.part().get();
                m.items(id).keySet().stream().filter(form -> ModularType.fits(shaping.stock(), form)).findFirst().ifPresent(form -> {
                    ItemStack part = new ItemStack(BuiltInRegistries.ITEM.get(shaping.item()));
                    part.set(FDataComponents.MADE_OF.get(), new MadeOf(id, form, shaping.cells()));
                    out.add(part);
                });
                return;
            }
            m.items(id).forEach((form, item) -> {
                if (role.accepts(new Material.Located(id, m, form)))
                    out.add(new ItemStack(item));
            });
        });
        return out;
    }

    @Override
    public void draw(Card card, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        ModularType type = ModularTypes.known().get(card.type());
        if (type == null)
            return;
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, BuiltInRegistries.ITEM.get(card.type()).getDescription().getString().toUpperCase(Locale.ROOT),
                1, 1, TITLE_COLOUR, false);
        graphics.fill(1, 11, WIDTH - 1, 12, 0x40000000);
        Map<String, Integer> counts = type.counts(card.pattern());
        List<String> roles = roles(type, card.pattern());
        for (int i = 0; i < roles.size(); i++) {
            ModularType.Role role = type.roles().get(roles.get(i));
            int count = counts.get(roles.get(i));
            String line = roles.get(i) + "  " + Component.translatable("modular.feedback.role." + role.name()).getString()
                    + (count > 1 && role.part().isEmpty() ? " x" + count : "")
                    + (role.set() ? " (" + Component.translatable("feedback.jei.modular.set").getString() + ")" : "");
            graphics.drawString(font, line, 24, 19 + 20 * i, VALUE_COLOUR, false);
        }
        List<String> pattern = type.patterns().get(card.pattern());
        for (int y = 0; y < pattern.size(); y++)
            for (int x = 0; x < pattern.get(y).length(); x++) {
                char c = pattern.get(y).charAt(x);
                if (c == ' ')
                    continue;
                int px = 112 + x * CELL;
                int py = 16 + y * CELL;
                graphics.fill(px, py, px + CELL - 1, py + CELL - 1, 0x30000000);
                graphics.drawString(font, String.valueOf(c), px + 2, py + 1, VALUE_COLOUR, false);
            }
        int y = 20 + 20 * roles.size() - 2;
        if (roles.stream().anyMatch(r -> type.roles().get(r).set()))
            y = note(graphics, font, y, type.family() == ModularType.Family.ARMOUR ? "feedback.jei.modular.studs" : "feedback.jei.modular.tip");
        if (type.edged())
            y = note(graphics, font, y, "feedback.jei.modular.grind");
        note(graphics, font, y, "feedback.jei.modular.coat");
    }

    private static int note(GuiGraphics graphics, Font font, int y, String key) {
        Component text = Component.translatable(key);
        graphics.drawWordWrap(font, text, 1, y, WIDTH - 2, LABEL_COLOUR);
        return y + 10 * font.split(text, WIDTH - 2).size() + 2;
    }
}
