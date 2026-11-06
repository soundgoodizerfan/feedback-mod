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
import io.github.soundgoodizerfan.feedback.process.ClientThermalProcesses;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.OreLadder;
import io.github.soundgoodizerfan.feedback.process.ThermalProcess;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
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
 * The ore ladder of one bearing ore (spec: staged ore processing): four tiers from the raw rock down, each with what you do, the machine,
 * and the ingots of metal one piece pays at that stage. Nothing is authored here: the figures are {@link OreLadder}'s and the leach or
 * smelt row is found in the thermal table the client already holds, so a pack's ore appears with no data of its own.
 */
public class OreProcessingCategory implements IRecipeCategory<OreProcessingCategory.Card> {

    public record Card(ResourceLocation id, Material material) {
    }

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "ore_processing", Card.class);

    private static final int WIDTH = 200, HEIGHT = 148, TIER = 32, TOP = 16, TEXT_X = 22, TEXT_W = WIDTH - 22 - 20;

    private final IDrawable icon;

    public OreProcessingCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.ROLLING_MILL.get()));
    }

    /** One card per ore: a material that has a concentrate form is a bearer on the ladder. */
    public static List<Card> cards(Map<ResourceLocation, Material> materials) {
        List<Card> out = new ArrayList<>();
        materials.forEach((id, m) -> {
            if (m.item(id, "concentrate").isPresent())
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
        return Component.translatable("feedback.jei.ore_processing");
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

    private static float share(Material m) {
        return m.bears().values().stream().max(Float::compare).orElse(1f);
    }

    /** The row that takes this ore's concentrate and does chemistry or smelting to it, if any. */
    private static Optional<ThermalProcess> route(Card card) {
        Optional<ItemStack> concentrate = card.material().stack(card.id(), "concentrate");
        if (concentrate.isEmpty())
            return Optional.empty();
        Optional<ThermalProcess> first = takes(concentrate.get());
        // A sulfide roasts to a calcine first: the card shows the row that leaches it.
        if (first.isPresent() && card.material().stack(card.id(), "calcine").map(c -> ItemStack.isSameItem(c, first.get().result())).orElse(false))
            return takes(first.get().result()).or(() -> first);
        return first;
    }

    private static Optional<ThermalProcess> takes(ItemStack stack) {
        return ClientThermalProcesses.get().stream()
                .filter(p -> p.reaction().isPresent() && !p.inputs().isEmpty() && p.inputs().stream().anyMatch(i -> i.test(stack)))
                .findFirst();
    }

    /** Whether that row leaches into a liquor (tier 3) rather than smelting the concentrate (tier 2's ceiling). */
    private static boolean leaches(Optional<ThermalProcess> row) {
        if (row.isEmpty() || row.get().reaction().get().fluidOutputs().isEmpty())
            return false;
        return !row.get().reaction().get().fluidOutputs().get(0).fluid().getPath().startsWith("molten_");
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Card card, IFocusGroup focuses) {
        Material m = card.material();
        ResourceLocation id = card.id();
        String[] rungs = {"lump", "crushed", "concentrate"};
        for (int i = 0; i < rungs.length; i++) {
            String form = i == 0 && m.item(id, "lump").isEmpty() ? "raw" : rungs[i];
            int y = TOP + i * TIER;
            builder.addSlot(i == 0 ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT, 1, y)
                    .addItemStacks(m.stack(id, form).map(List::of).orElse(List.of()));
        }
        Optional<ThermalProcess> row = route(card);
        if (row.isPresent()) {
            var out = row.get().reaction().get().fluidOutputs();
            if (!out.isEmpty())
                builder.addSlot(RecipeIngredientRole.OUTPUT, 1, TOP + 3 * TIER)
                        .addFluidStack(BuiltInRegistries.FLUID.get(out.get(0).fluid()), out.get(0).mb())
                        .setFluidRenderer(Math.max(1, out.get(0).mb()), false, 16, 16);
            if (!row.get().result().isEmpty())
                builder.addSlot(RecipeIngredientRole.OUTPUT, WIDTH - 17, TOP + 3 * TIER).addItemStack(row.get().result());
        }
        m.stack(id, "tailings").ifPresent(t -> builder.addSlot(RecipeIngredientRole.OUTPUT, WIDTH - 17, TOP + 2 * TIER).addItemStack(t));
    }

    /** Draws {@code text} wrapped to the room between the slot column and the right-hand slots; returns the y below it. */
    private static int line(GuiGraphics graphics, Font font, int y, Component text, int colour) {
        for (var part : font.split(text, TEXT_W)) {
            graphics.drawString(font, part, TEXT_X, y, colour, false);
            y += 10;
        }
        return y;
    }

    private static String ingots(float share, float recovery) {
        return Readout.number(share * OreLadder.ORE_CONTENT * recovery);
    }

    @Override
    public void draw(Card card, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("material." + card.id().getNamespace() + "." + card.id().getPath())
                .getString().toUpperCase(Locale.ROOT), 1, 1, 0xFF3F3F3F, false);
        graphics.fill(1, 12, WIDTH - 1, 13, 0x40000000);
        float share = share(card.material());
        Optional<ThermalProcess> row = route(card);
        boolean leach = leaches(row);
        float last = leach ? OreLadder.LEACH : OreLadder.CONCENTRATE;
        float[] recoveries = {OreLadder.MELT, OreLadder.CRUSHED, OreLadder.CONCENTRATE, last};
        String[] keys = {"melt", "crush", "separate", leach ? "leach" : row.isPresent() ? "smelt" : "none"};
        for (int i = 0; i < 4; i++) {
            int y = TOP + i * TIER + 1;
            int below = line(graphics, font, y, Component.translatable("feedback.jei.ore_processing." + keys[i]), 0xFF262626);
            if (i < 3 || row.isPresent())
                line(graphics, font, below, Component.translatable("feedback.jei.ore_processing.pays", ingots(share, recoveries[i]),
                        Readout.number(recoveries[i] * 100f)), 0xFF7A7A7A);
        }
    }
}
