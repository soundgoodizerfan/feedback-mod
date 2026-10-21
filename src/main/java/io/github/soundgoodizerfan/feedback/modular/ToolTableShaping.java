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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FormItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Making a part at the Tool Table: the shapes it can make, and what stock makes each. This is what
 * {@code ModularPartRecipe} was, as plain functions of a list of stacks rather than a crafting grid
 * -- the rules are the same ones.
 *
 * <ul>
 *   <li><b>Every shape of every type is on the list</b>: a shaped role's standard shape, each
 *       variant that lays cells ({@link ModularType.Shaping#laid}) and each variant that is forged
 *       ({@link ModularType.Shaping#forged}). The list is the player's choice; nothing infers it,
 *       because one stock makes any of several shapes.</li>
 *   <li><b>Stock is volume.</b> A cell is an ingot (144 mB, {@link ModularTuning#CELL_MB}); the pile
 *       must hold exactly the shape's cells in volume, all of one material and one form the shape
 *       takes, so nine ingots and one block fill the same nine cells (a block never fills fewer: it
 *       cannot be split). A forged variant costs the standard shape's cells. A part is never made from
 *       a part.</li>
 *   <li><b>The hot-working window holds.</b> A material with a window must be inside it, judged on
 *       the coldest piece, and the part comes out at that heat -- hot wherever it goes (philosophy 9).
 *       Heat in v1 is whatever the stock items carry ({@link ItemHeat}); a hearth beside the table
 *       that reheats the workpiece is {@code [OPEN]} (TODO).</li>
 *   <li><b>A forged variant plays its rhythm</b> ({@link ForgeRhythm}) in the table's screen; the
 *       flaws it earns are on the part. Any other comes out as soon as the stock is in.</li>
 * </ul>
 */
public final class ToolTableShaping {

    private ToolTableShaping() {
    }

    /** One entry in the shape list: a shape of a part of some type. */
    public record Option(ResourceLocation type, ModularType.Shaping shaping, ModularType.Variant variant) {

        /** Whether it is played on the anvil rather than made as soon as the stock is in. */
        public boolean forged() {
            return variant.shape().isEmpty() && !variant.rhythm().isEmpty();
        }

        /** Items of stock it takes: its own cells if laid, the standard shape's if forged. */
        public int cells() {
            return forged() || variant.name().isEmpty() ? shaping.cells() : variant.cells();
        }

        /** The name the list shows: the part, and the shape if it is not the standard one. */
        public Component label() {
            Component part = Component.translatable("item." + shaping.item().getNamespace() + "." + shaping.item().getPath());
            return variant.name().isEmpty() ? part : Component.translatable("modular.feedback.variant." + variant.name());
        }
    }

    /** Every shape the types can make, in type order, each part once: the standard, the laid, then the forged. */
    public static List<Option> options(Map<ResourceLocation, ModularType> types) {
        List<Option> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map.Entry<ResourceLocation, ModularType> e : types.entrySet())
            for (String key : e.getValue().order()) {
                ModularType.Shaping shaping = e.getValue().roles().get(key).part().orElse(null);
                if (shaping == null)
                    continue;
                List<ModularType.Variant> all = new ArrayList<>(shaping.laid());
                all.addAll(shaping.forged());
                for (ModularType.Variant v : all)
                    if (seen.add(shaping.item() + "/" + v.name()))
                        out.add(new Option(e.getKey(), shaping, v));
            }
        return out;
    }

    /** What a valid pile of stock is: the stuff, how many pieces, and the coldest piece's heat. */
    public record Stock(Material.Located what, int cells, float coldest) {}

    /**
     * Whether {@code contents} is exactly the stock {@code option} takes. Empty cells are ignored.
     * A piece that is already a part, an unknown item, a second material or form, the wrong number,
     * or a material outside its hot-working window all refuse.
     */
    public static Optional<Stock> check(Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level) {
        Material.Located what = null;
        int mb = 0;
        float coldest = Float.MAX_VALUE;
        for (ItemStack s : contents) {
            if (s.isEmpty())
                continue;
            if (s.has(FDataComponents.MADE_OF.get()))
                return Optional.empty();
            Optional<Material.Located> l = Material.find(materials, s.getItem());
            if (l.isEmpty() || (what != null && (!what.id().equals(l.get().id()) || !what.form().equals(l.get().form()))))
                return Optional.empty();
            what = l.get();
            mb += s.getCount() * FormItems.volume(l.get().form());
            coldest = Math.min(coldest, ItemHeat.get(s, level).value());
        }
        if (what == null || mb != option.cells() * ModularTuning.CELL_MB || !ModularType.fits(option.shaping().stock(), what.form()))
            return Optional.empty();
        Material m = what.material();
        if (m.minTemperature().value() > 0 && (coldest < m.minTemperature().value() || coldest > m.maxTemperature().value()))
            return Optional.empty();
        return Optional.of(new Stock(what, option.cells(), coldest));
    }

    /** The part {@code stock} makes in {@code option}, with {@code flaw} flaws, at the stock's heat; empty if the part item is gone. */
    public static Optional<ItemStack> part(Option option, Stock stock, int flaw, Level level) {
        return BuiltInRegistries.ITEM.getOptional(option.shaping().item()).map(item -> {
            ItemStack out = new ItemStack(item);
            out.set(FDataComponents.MADE_OF.get(), new MadeOf(stock.what().id(), stock.what().form(), stock.cells(), option.variant().name(), flaw));
            if (stock.coldest() > 0 && stock.what().material().minTemperature().value() > 0)
                ItemHeat.set(out, new Tu(stock.coldest()), level);
            return out;
        });
    }

    /** The part {@code contents} makes in a shape that is not forged: no flaws, out as soon as the stock is in. */
    public static ItemStack make(Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level) {
        if (option.forged())
            return ItemStack.EMPTY;
        return check(option, contents, materials, level).flatMap(s -> part(option, s, 0, level)).orElse(ItemStack.EMPTY);
    }
}
