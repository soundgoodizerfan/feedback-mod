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
import java.util.Map;

import io.github.soundgoodizerfan.feedback.client.GuiStyle;
import io.github.soundgoodizerfan.feedback.potion.PotionShape;
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
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The curves the Brewing Stand's plants walk (potions spec 15): one card per shape in the catalogue, with a dot tracing the path from its start to
 * its end and leaving a fading trail, and the net displacement marked. Drawn from {@link PotionShape#atArc}, the function the stand walks, so what the card
 * shows is what stirring does. Client only (JEI); the slot lists every item whose material carries that shape, so a plant's uses open its card.
 */
public class PotionShapeCategory implements IRecipeCategory<PotionShapeCategory.Card> {

    public record Card(PotionShape.Type type, List<Item> plants) {
    }

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "potion_shape", Card.class);

    private static final int WIDTH = 168, HEIGHT = 110, BX0 = 8, BY0 = 52, BX1 = 160, BY1 = 106, SAMPLES = 64;
    /** Ticks of the loop in milliseconds: the walk, then a hold on the end before it starts again. */
    private static final long WALK_MS = 2600, HOLD_MS = 900;

    private final IDrawable icon;

    public PotionShapeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.BREWING_STAND.get()));
    }

    /** One card per type, each with the plants that carry it (a plant with no shape is a line). */
    public static List<Card> cards(Map<ResourceLocation, Material> materials) {
        List<Card> out = new ArrayList<>();
        for (PotionShape.Type type : PotionShape.Type.values()) {
            List<Item> plants = new ArrayList<>();
            materials.forEach((id, m) -> {
                if (m.potion().isPresent() && m.potion().get().shape().type() == type)
                    plants.addAll(m.items(id).values());
            });
            out.add(new Card(type, plants));
        }
        return out;
    }

    @Override
    public RecipeType<Card> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.potion_shape");
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
        builder.addInputSlot(WIDTH - 19, 1).setStandardSlotBackground().addItemStacks(card.plants().stream().map(ItemStack::new).toList());
    }

    @Override
    public void draw(Card card, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        String name = card.type().getSerializedName();
        g.drawString(font, Component.translatable("feedback.jei.shape." + name).getString().toUpperCase(java.util.Locale.ROOT), 1, 1, 0xFF3F3F3F, false);
        g.fill(1, 22, WIDTH - 1, 23, 0x40000000);
        int y = 26;
        for (FormattedCharSequence line : font.split(Component.translatable("feedback.jei.shape." + name + ".tip"), WIDTH - 2)) {
            g.drawString(font, line, 1, y, 0xFF3F3F3F, false);
            y += 9;
        }
        PotionShape shape = PotionShape.of(card.type());
        // Fit the whole curve in the box, keeping its proportions.
        float[] p = new float[2];
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i <= SAMPLES; i++) {
            shape.atArc((float) i / SAMPLES, p);
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
        }
        float w = Math.max(maxX - minX, 0.05f), h = Math.max(maxY - minY, 0.05f);
        float k = Math.min((BX1 - BX0 - 8) / w, (BY1 - BY0 - 8) / h);
        float ox = (BX0 + BX1) / 2f - (minX + maxX) / 2f * k, oy = (BY0 + BY1) / 2f + (minY + maxY) / 2f * k;
        int[] xs = new int[SAMPLES + 1], ys = new int[SAMPLES + 1];
        for (int i = 0; i <= SAMPLES; i++) {
            shape.atArc((float) i / SAMPLES, p);
            xs[i] = Math.round(ox + p[0] * k);
            ys[i] = Math.round(oy - p[1] * k);
        }
        // The whole path, faint; the net displacement as a dashed chord from the start to the end.
        for (int i = 0; i < SAMPLES; i++)
            GuiStyle.line(g, xs[i], ys[i], xs[i + 1], ys[i + 1], 0x30000000);
        for (int i = 0; i < SAMPLES; i += 4)
            GuiStyle.line(g, lerpInt(xs[0], xs[SAMPLES], i / (float) SAMPLES), lerpInt(ys[0], ys[SAMPLES], i / (float) SAMPLES),
                    lerpInt(xs[0], xs[SAMPLES], (i + 2) / (float) SAMPLES), lerpInt(ys[0], ys[SAMPLES], (i + 2) / (float) SAMPLES), 0x80A83828);
        ring(g, xs[0], ys[0], 0xFF3F3F3F);
        int ex = xs[SAMPLES], ey = ys[SAMPLES];
        g.fill(ex - 2, ey - 2, ex + 3, ey + 3, 0xFFA83828);
        Component end = Component.translatable("feedback.jei.shape.net");
        g.drawString(font, end, Math.min(ex + 5, WIDTH - 1 - font.width(end)), Math.max(BY0 - 2, ey - 10), 0xFFA83828, false);
        // The dot: it walks the arc at an even pace, with the last stretch behind it fading out.
        long now = System.currentTimeMillis() % (WALK_MS + HOLD_MS);
        float f = Math.min(1f, now / (float) WALK_MS);
        int at = Math.round(f * SAMPLES), tail = SAMPLES / 4;
        for (int i = Math.max(0, at - tail); i < at; i++) {
            int alpha = 40 + Math.round(200f * (i - (at - tail)) / tail);
            GuiStyle.line(g, xs[i], ys[i], xs[i + 1], ys[i + 1], alpha << 24 | 0x2A6AA0);
        }
        g.fill(xs[at] - 1, ys[at] - 1, xs[at] + 2, ys[at] + 2, 0xFF1A4A80);
    }

    private static int lerpInt(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    private static void ring(GuiGraphics g, int x, int y, int colour) {
        g.renderOutline(x - 2, y - 2, 5, 5, colour);
    }
}
