// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import io.github.soundgoodizerfan.feedback.registry.FormItems;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.PartBenchShaping;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
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
import net.minecraft.world.item.ItemStack;

/**
 * The Tool Table's part cards: stock and a hand hammer in, a part out. One card per shape -- the
 * standard one, each laid variant (drawn as its cell diagram) and each forged variant, which says
 * it is forged and shows no rhythm (the pattern is played at the table, not read here). The
 * material rides on the stack, so the stock slot cycles through everything the part can be made of,
 * in the number of cells the shape has, and the part slot cycles beside it.
 */
public class PartBenchPartCategory implements IRecipeCategory<PartBenchShaping.Option> {

    static final RecipeType<PartBenchShaping.Option> TYPE = RecipeType.create("feedback", "tool_table_part", PartBenchShaping.Option.class);

    private static final int WIDTH = 168, HEIGHT = 84;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;
    private static final int CELL = 10;

    private final IDrawable icon;

    public PartBenchPartCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FModular.SMITHING_TABLE_ITEM.get()));
    }

    @Override
    public RecipeType<PartBenchShaping.Option> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.tool_table_part");
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
    public void setRecipe(IRecipeLayoutBuilder builder, PartBenchShaping.Option option, IFocusGroup focuses) {
        List<ItemStack> stock = new ArrayList<>(), parts = new ArrayList<>();
        ClientMaterials.get().forEach((id, m) -> {
            // Same trait gate as PartBenchShaping.check: a food shape takes only dough (kneadable).
            if (m.traits().containsAll(option.shaping().traits()))
                m.items(id).entrySet().stream()
                        .filter(e -> ModularType.fits(option.shaping().stock(), e.getKey()))
                        .filter(e -> option.stockMb(1f) % FormItems.volume(e.getKey()) == 0)
                        .findFirst().ifPresent(e -> {
                            stock.add(new ItemStack(e.getValue(), option.stockMb(1f) / FormItems.volume(e.getKey())));
                            ItemStack part = new ItemStack(BuiltInRegistries.ITEM.get(option.shaping().item()));
                            part.set(FDataComponents.MADE_OF.get(), new MadeOf(id, e.getKey(), option.cells(), option.variant().name()));
                            parts.add(part);
                        });
        });
        builder.addInputSlot(1, 14).setStandardSlotBackground().addItemStacks(stock);
        builder.addInputSlot(1, 34).setStandardSlotBackground().addItemStack(new ItemStack(FModular.HAND_HAMMER.get()));
        builder.addOutputSlot(143, 62).setOutputSlotBackground().addItemStacks(parts);
    }

    @Override
    public void draw(PartBenchShaping.Option option, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, option.label().getString().toUpperCase(Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        graphics.fill(1, 11, WIDTH - 1, 12, 0x40000000);
        graphics.drawString(font, Component.translatable("feedback.jei.tool_table_part.stock", option.cells()), 24, 19, VALUE_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.tool_table_part.hammer"), 24, 39, VALUE_COLOUR, false);
        if (option.forged()) {
            graphics.drawWordWrap(font, Component.translatable("feedback.jei.tool_table_part.forged"), 1, 56, WIDTH - 50, LABEL_COLOUR);
            return;
        }
        // The part's size: one square a cell of stock, rows as even as they go.
        int n = Math.max(1, option.cells());
        int perRow = n <= 3 ? n : (int) Math.ceil(Math.sqrt(n));
        for (int i = 0; i < n; i++) {
            int px = 112 + i % perRow * CELL;
            int py = 16 + i / perRow * CELL;
            graphics.fill(px, py, px + CELL - 1, py + CELL - 1, 0x30000000);
        }
        graphics.drawWordWrap(font, Component.translatable("feedback.jei.tool_table_part.note"), 1, 56, WIDTH - 50, LABEL_COLOUR);
    }
}
