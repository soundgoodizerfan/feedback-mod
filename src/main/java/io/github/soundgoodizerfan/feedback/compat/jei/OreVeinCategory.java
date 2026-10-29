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

import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.worldgen.OreVein;
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
import net.minecraft.world.item.Items;

/**
 * An ore vein card (mechanics 9.2): the ores a deposit holds, laid out core, cap, rim and scatter,
 * and where and how often it occurs. Output slots, so looking up an ore shows where it is found.
 */
public class OreVeinCategory implements IRecipeCategory<OreVein> {

    static final RecipeType<OreVein> TYPE = RecipeType.create("feedback", "ore_vein", OreVein.class);

    private static final int WIDTH = 168, HEIGHT = 96;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public OreVeinCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.IRON_PICKAXE));
    }

    @Override
    public RecipeType<OreVein> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.ore_vein");
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

    private static ItemStack stack(ResourceLocation id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(id));
    }

    /** Core first, then cap, rim and each scatter ore, in the order the slots are drawn. */
    private static List<ResourceLocation> ores(OreVein v) {
        List<ResourceLocation> out = new ArrayList<>();
        out.add(v.ore());
        v.layers().cap().ifPresent(z -> out.add(z.ore()));
        v.layers().rim().ifPresent(z -> out.add(z.ore()));
        v.layers().scatter().forEach(z -> out.add(z.ore()));
        return out;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, OreVein vein, IFocusGroup focuses) {
        List<ResourceLocation> ores = ores(vein);
        for (int i = 0; i < ores.size(); i++)
            builder.addOutputSlot(1 + i * 18, 14).setOutputSlotBackground().addItemStack(stack(ores.get(i)));
        List<ResourceLocation> signs = vein.indicators();
        for (int i = 0; i < signs.size(); i++)
            builder.addOutputSlot(WIDTH - 19 - (signs.size() - 1 - i) * 18, 14).setOutputSlotBackground().addItemStack(stack(signs.get(i)));
    }

    @Override
    public void draw(OreVein vein, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, stack(vein.ore()).getHoverName().getString().toUpperCase(Locale.ROOT), 1, 1, TITLE_COLOUR, false);
        List<String> layers = new ArrayList<>();
        layers.add(Component.translatable("feedback.jei.ore_vein.core").getString());
        vein.layers().cap().ifPresent(z -> layers.add(Component.translatable("feedback.jei.ore_vein.cap").getString()));
        vein.layers().rim().ifPresent(z -> layers.add(Component.translatable("feedback.jei.ore_vein.rim").getString()));
        vein.layers().scatter().forEach(z -> layers.add(Component.translatable("feedback.jei.ore_vein.scatter").getString()));
        int y = 36;
        row(graphics, font, y, "feedback.jei.ore_vein.layers", String.join(", ", layers));
        y += 10;
        row(graphics, font, y, "feedback.jei.ore_vein.depth", "Y " + vein.minY() + " .. " + vein.maxY()
                + (vein.triangle() ? " (mid)" : ""));
        y += 10;
        row(graphics, font, y, "feedback.jei.ore_vein.size", vein.radius() + " x " + vein.thickness() + " blocks");
        y += 10;
        row(graphics, font, y, "feedback.jei.ore_vein.density", Readout.number(vein.density() * 100f) + "%");
        y += 10;
        row(graphics, font, y, "feedback.jei.ore_vein.weight", Integer.toString(vein.weight()));
        y += 10;
        row(graphics, font, y, "feedback.jei.ore_vein.dimension", String.join(", ",
                vein.dimensions().stream().map(ResourceLocation::getPath).toList()));
        if (!vein.indicators().isEmpty()) {
            Component outcrop = Component.translatable("feedback.jei.ore_vein.outcrop");
            graphics.drawString(font, outcrop, WIDTH - 23 - (vein.indicators().size() - 1) * 18 - font.width(outcrop), 19, LABEL_COLOUR, false);
        }
    }

    private static void row(GuiGraphics graphics, Font font, int y, String labelKey, String value) {
        graphics.drawString(font, Component.translatable(labelKey), 1, y, LABEL_COLOUR, false);
        graphics.drawString(font, value, 60, y, VALUE_COLOUR, false);
    }
}
