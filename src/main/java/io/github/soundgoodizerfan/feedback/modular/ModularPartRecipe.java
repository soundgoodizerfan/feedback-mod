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
package io.github.soundgoodizerfan.feedback.modular;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.item.HandToolItem;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Shaping a part: a hand tool and stock laid in a part's {@link ModularType.Shaping#shape}, the
 * tool anywhere else in the grid. Out comes the one item for that part with what it was made of
 * on it ({@link MadeOf}) -- three iron ingots in a row are an iron pickaxe head, three of copper a
 * copper one, and both are {@code feedback:pickaxe_head}.
 *
 * <h2>Why a tool, and why every shape has two cells or more</h2>
 * The tool is what makes this shaping rather than a shape: it costs the tool a point
 * ({@link HandToolItem#getCraftingRemainingItem}, the hand tool's accepted wear), and it keeps a
 * part's shape from colliding with vanilla's (three planks in a V is a bowl without one). One
 * tool and one workpiece is already hand deformation ({@code HandDeformationRecipe}), so no shape
 * is a single cell.
 *
 * <h2>The hot-working window holds</h2>
 * Steel below its window does not move under a hammer (philosophy 7's hard gate), so a stock
 * whose material has one must be in it, and the part comes out at the heat the stock had --
 * still hot wherever it goes (philosophy 9). A material with no window (iron, wood, stone) shapes
 * cold. Knapping stone and carving wood with a hammer are the same act as forging here: a
 * shaping operation per kind of material is {@code [OPEN]}, spec 12.
 */
public class ModularPartRecipe extends CustomRecipe {

    /** Set by {@code matches}, as {@code HandDeformationRecipe} does: {@code assemble} has no clock. */
    private static final ThreadLocal<WeakReference<Level>> LEVEL = new ThreadLocal<>();

    public ModularPartRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        LEVEL.set(new WeakReference<>(level));
        return !shape(input, ClientMaterials.on(level), ModularTypes.on(level), level).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        WeakReference<Level> level = LEVEL.get();
        Level l = level == null ? null : level.get();
        return l == null ? ItemStack.EMPTY : shape(input, MaterialTable.get().materials(), ModularTypes.get().types(), l);
    }

    /** The part {@code input} shapes, or empty. */
    public static ItemStack shape(CraftingInput input, Map<ResourceLocation, Material> materials,
                                  Map<ResourceLocation, ModularType> types, Level level) {
        if (materials.isEmpty() || types.isEmpty())
            return ItemStack.EMPTY;
        boolean tool = false;
        Material.Located stock = null;
        Set<Long> cells = new HashSet<>();
        float coldest = Float.MAX_VALUE;
        for (int y = 0; y < input.height(); y++)
            for (int x = 0; x < input.width(); x++) {
                ItemStack s = input.getItem(x, y);
                if (s.isEmpty())
                    continue;
                if (s.getItem() instanceof HandToolItem) {
                    if (tool)
                        return ItemStack.EMPTY;
                    tool = true;
                    continue;
                }
                if (s.has(FDataComponents.MADE_OF.get()))
                    return ItemStack.EMPTY;
                Optional<Material.Located> l = Material.find(materials, s.getItem());
                if (l.isEmpty() || (stock != null && (!stock.id().equals(l.get().id()) || !stock.form().equals(l.get().form()))))
                    return ItemStack.EMPTY;
                stock = l.get();
                cells.add(key(x, y));
                coldest = Math.min(coldest, ItemHeat.get(s, level).value());
            }
        if (!tool || stock == null || cells.size() < 2)
            return ItemStack.EMPTY;
        Material m = stock.material();
        if (m.minTemperature().value() > 0 && (coldest < m.minTemperature().value() || coldest > m.maxTemperature().value()))
            return ItemStack.EMPTY;
        for (ModularType type : types.values())
            for (ModularType.Role role : type.roles().values()) {
                ModularType.Shaping shaping = role.part().orElse(null);
                if (shaping == null || !ModularType.fits(shaping.stock(), stock.form()) || !fills(shaping.shape(), cells))
                    continue;
                Optional<ItemStack> out = BuiltInRegistries.ITEM.getOptional(shaping.item()).map(ItemStack::new);
                if (out.isEmpty())
                    continue;
                out.get().set(FDataComponents.MADE_OF.get(), new MadeOf(stock.id(), stock.form(), cells.size()));
                if (coldest > 0 && m.minTemperature().value() > 0)
                    ItemHeat.set(out.get(), new Tu(coldest), level);
                return out.get();
            }
        return ItemStack.EMPTY;
    }

    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    /** Whether {@code cells}, wherever they sit, are exactly {@code shape} or its mirror image. */
    static boolean fills(java.util.List<String> shape, Set<Long> cells) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (long c : cells) {
            minX = Math.min(minX, (int) (c >> 32));
            minY = Math.min(minY, (int) c);
        }
        Set<Long> at = new HashSet<>();
        for (long c : cells)
            at.add(key((int) (c >> 32) - minX, (int) c - minY));
        int width = shape.stream().mapToInt(String::length).max().orElse(0);
        for (boolean mirrored : new boolean[] {false, true}) {
            Set<Long> want = new HashSet<>();
            int ox = Integer.MAX_VALUE;
            for (int y = 0; y < shape.size(); y++)
                for (int x = 0; x < shape.get(y).length(); x++)
                    if (shape.get(y).charAt(x) != ' ')
                        ox = Math.min(ox, mirrored ? width - 1 - x : x);
            for (int y = 0; y < shape.size(); y++)
                for (int x = 0; x < shape.get(y).length(); x++)
                    if (shape.get(y).charAt(x) != ' ')
                        want.add(key((mirrored ? width - 1 - x : x) - ox, y));
            if (want.equals(at))
                return true;
        }
        return false;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FModular.PART_RECIPE.get();
    }
}
