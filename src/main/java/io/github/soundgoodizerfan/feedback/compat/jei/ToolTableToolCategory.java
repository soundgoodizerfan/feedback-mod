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
import io.github.soundgoodizerfan.feedback.modular.ToolDesigns;
import io.github.soundgoodizerfan.feedback.modular.ToolTableShaping;
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
 * The Tool Table's tool cards: one per form of each type -- which roles it has and how many of
 * each -- as a slot per role, cycling through everything that fits it (a part of every material
 * for a shaped role, every item of a form it takes for stock). There is no grid: the table has six
 * slots and the roles may sit in any of them, so the card says what goes in, not where.
 * Below, what the pattern leaves out: the set-in tip or studs, a binding, regrinding the edge at a
 * grindstone, and plating.
 * <p>
 * Built from the client's copy of the {@link ModularType}s and the material table, so a pack's own
 * type or material shows with no edit, and no item is named (philosophy 11).
 */
public class ToolTableToolCategory implements IRecipeCategory<ToolTableToolCategory.Card> {

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "tool_table_tool", Card.class);

    /** One form of one type. */
    public record Card(ResourceLocation type, int form) {
    }

    private static final int WIDTH = 168, HEIGHT = 184;
    /** Rows of slots in a column; a type with more roles than this (a gun) takes a second column. */
    private static final int ROWS = 8;

    private static int column(Map<String, Integer> roles, int i) {
        return roles.size() > ROWS ? i / ROWS * 84 : 0;
    }
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public ToolTableToolCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(net.minecraft.world.item.Items.CRAFTING_TABLE));
    }

    @Override
    public RecipeType<Card> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.tool_table_tool");
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

    /** The roles of a form with every optional part offered, in the type's order, and how many of each: {@link ToolDesigns}' own answer. */
    private static Map<String, Integer> roles(ModularType type, int form) {
        return ToolDesigns.counts(type, form, new java.util.LinkedHashSet<>(type.optionals()), java.util.Map.of());
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Card card, IFocusGroup focuses) {
        ModularType type = ModularTypes.known().get(card.type());
        if (type == null)
            return;
        Map<ResourceLocation, Material> materials = ClientMaterials.get();
        int i = 0;
        Map<String, Integer> roles = roles(type, card.form());
        for (Map.Entry<String, Integer> r : roles.entrySet()) {
            builder.addInputSlot(1 + column(roles, i), 14 + 20 * (i % ROWS)).setStandardSlotBackground()
                    .addItemStacks(stacks(type.roles().get(r.getKey()), r.getValue(), materials));
            i++;
        }
        builder.addOutputSlot(143, roles.size() > ROWS ? 14 : 62).setOutputSlotBackground()
                .addItemStack(new ItemStack(BuiltInRegistries.ITEM.get(card.type())));
    }

    /**
     * What fits a role, from the shared model: the standard part of every material for a shaped
     * role (every kind of head for a twin's), {@code count} items of each fitting stock for a raw one.
     */
    static List<ItemStack> stacks(ModularType.Role role, int count, Map<ResourceLocation, Material> materials) {
        List<ItemStack> out = new ArrayList<>();
        if (ToolDesigns.shaped(role)) {
            for (ToolTableShaping.Option o : ToolDesigns.shapes(role, ModularTypes.known()))
                if (o.variant().name().isEmpty())
                    for (Material.Located l : ToolDesigns.materials(role, o, materials)) {
                        ItemStack part = new ItemStack(BuiltInRegistries.ITEM.get(o.shaping().item()));
                        part.set(FDataComponents.MADE_OF.get(), new MadeOf(l.id(), l.form(), o.cells()));
                        out.add(part);
                    }
            return out;
        }
        for (Material.Located l : ToolDesigns.materials(role, null, materials))
            l.material().item(l.id(), l.form()).ifPresent(item -> out.add(new ItemStack(item, Math.min(count, item.getDefaultMaxStackSize()))));
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
        Map<String, Integer> roles = roles(type, card.form());
        int i = 0;
        for (Map.Entry<String, Integer> r : roles.entrySet()) {
            ModularType.Role role = type.roles().get(r.getKey());
            String line = Component.translatable("modular.feedback.role." + role.name()).getString()
                    + (r.getValue() > 1 && !ToolDesigns.shaped(role) ? " x" + r.getValue() : "")
                    + (type.optionals().contains(r.getKey()) ? " (" + Component.translatable("feedback.jei.tool_table_tool.optional").getString() + ")" : "");
            if (roles.size() > ROWS)
                line = font.plainSubstrByWidth(line, 56);
            graphics.drawString(font, line, 24 + column(roles, i), 19 + 20 * (i % ROWS), VALUE_COLOUR, false);
            i++;
        }
        int y = 20 + 20 * Math.min(ROWS, roles.size()) - 2;
        boolean dish = type.family() == ModularType.Family.DISH;
        y = note(graphics, font, y, dish ? "feedback.jei.tool_table_tool.dish" : "feedback.jei.tool_table_tool.slots");
        if (dish)
            return;
        if (roles.keySet().stream().anyMatch(r -> type.roles().get(r).set()))
            y = note(graphics, font, y, "feedback.jei.modular.studs");
        else if (type.family() != ModularType.Family.ARMOUR && type.roles().values().stream().anyMatch(ModularType.Role::set))
            y = note(graphics, font, y, "feedback.jei.modular.tip");
        if (type.roles().values().stream().anyMatch(r -> r.name().equals("binding")))
            y = note(graphics, font, y, "feedback.jei.modular.binding");
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
