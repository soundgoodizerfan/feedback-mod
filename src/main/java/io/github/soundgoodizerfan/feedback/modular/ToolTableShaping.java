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
 *   <li><b>Stock is volume, and divides.</b> A cell is an ingot (144 mB, {@link ModularTuning#CELL_MB}); the pile
 *       must hold at least the shape's cells in volume, all of one material and one form the shape
 *       takes. The shape spends only what it needs and {@link #change} returns the rest as smaller items
 *       (a stone block, nine ingots' worth, gives a part and the cobble left over). A forged variant costs
 *       the standard shape's cells. A part is never made from a part.</li>
 *   <li><b>The hot-working window holds.</b> A material with a window must be inside it, judged on
 *       the coldest piece, and the part comes out at that heat -- hot wherever it goes (philosophy 9).
 *       Heat is whatever the stock items carry ({@link ItemHeat}); a Hearth beside the table
 *       reheats them in the cells ({@code machine.hearth.HearthBlockEntity}).</li>
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
            // A standard shape that is itself a named kind (a receiver's is a handgun) shows its name.
            String name = variant.name().isEmpty() ? shaping.base() : variant.name();
            return name.isEmpty() ? part : Component.translatable("modular.feedback.variant." + name);
        }
    }

    /** Every shape the types can make, in type order, each part once: the standard, the laid, then the forged. */
    public static List<Option> options(Map<ResourceLocation, ModularType> types) {
        List<Option> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map.Entry<ResourceLocation, ModularType> e : types.entrySet()) {
            List<ModularType.Shaping> shapings = new ArrayList<>();
            for (String key : e.getValue().order())
                e.getValue().roles().get(key).part().ifPresent(shapings::add);
            shapings.addAll(e.getValue().shapes());
            for (ModularType.Shaping shaping : shapings) {
                List<ModularType.Variant> all = new ArrayList<>(shaping.laid());
                all.addAll(shaping.forged());
                for (ModularType.Variant v : all)
                    if (seen.add(shaping.item() + "/" + v.name()))
                        out.add(new Option(e.getKey(), shaping, v));
            }
        }
        return out;
    }

    /**
     * Whether stock of {@code what}, in any amount, could make {@code option} at {@code station}: the bench works that
     * kind of stuff, the form is one the shape takes, and the material has the traits it asks for. What the bench's list
     * shows once a material is in the cells.
     */
    public static boolean takes(Station station, Option option, Material.Located what) {
        return (!option.forged() || station.forges()) && station.accepts(what.material())
                && ModularType.fits(option.shaping().stock(), what.form())
                && what.material().traits().containsAll(option.shaping().traits());
    }

    /** What a valid pile of stock is: the stuff, how many pieces, and the coldest piece's heat. */
    public record Stock(Material.Located what, int cells, float coldest, int mb) {

        /** Millibuckets the shape spends. */
        public int spent() {
            return cells * ModularTuning.CELL_MB;
        }

        /** Millibuckets of the pile the shape leaves. */
        public int left() {
            return mb - spent();
        }
    }

    /**
     * Whether {@code contents} is exactly the stock {@code option} takes. Empty cells are ignored.
     * A piece that is already a part, an unknown item, a second material or form, the wrong number,
     * or a material outside its hot-working window all refuse.
     */
    public static Optional<Stock> check(Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level) {
        return check(Station.TOOL, option, contents, materials, level);
    }

    /** As {@link #check(Option, List, Map, Level)}, at a bench that takes only the stock it works ({@link Station}). */
    public static Optional<Stock> check(Station station, Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level) {
        return check(station, option, contents, materials, level, false);
    }

    /** As above; {@code exact} refuses a pile bigger than the shape (a food shape reshapes the whole pile). */
    private static Optional<Stock> check(Station station, Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level,
                                         boolean exact) {
        if (option.forged() && !station.forges())
            return Optional.empty();
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
        int need = option.cells() * ModularTuning.CELL_MB;
        if (what == null || !takes(station, option, what) || (exact ? mb != need : mb < need))
            return Optional.empty();
        Material m = what.material();
        if (m.minTemperature().value() > 0 && (coldest < m.minTemperature().value() || coldest > m.maxTemperature().value()))
            return Optional.empty();
        return Optional.of(new Stock(what, option.cells(), coldest, mb));
    }

    /** The part {@code stock} makes in {@code option}, with {@code flaw} flaws, at the stock's heat; empty if the part item is gone. */
    public static Optional<ItemStack> part(Option option, Stock stock, int flaw, Level level) {
        return part(option, stock, flaw, 1f, level);
    }

    /**
     * As {@link #part(Option, Stock, int, Level)}, and with the work the forge did against the work it asked
     * ({@link ForgeRhythm.Work#ratio}) kept on the part, which the bake reads as thickness and mass.
     * Exactly 1 leaves nothing on it, so a part made without a forge is the drawing.
     */
    public static Optional<ItemStack> part(Option option, Stock stock, int flaw, float work, Level level) {
        return BuiltInRegistries.ITEM.getOptional(option.shaping().item()).map(item -> {
            ItemStack out = new ItemStack(item);
            MadeOf made = new MadeOf(stock.what().id(), stock.what().form(), stock.cells(), option.variant().name(), flaw);
            out.set(FDataComponents.MADE_OF.get(), work == 1f ? made : made.withDim(ForgeRhythm.WORK, work));
            if (stock.coldest() > 0 && stock.what().material().minTemperature().value() > 0)
                ItemHeat.set(out, new Tu(stock.coldest()), level);
            return out;
        });
    }

    /**
     * A food shape ({@link ModularType#shapes}): the stock reshaped, one food per piece, so volume and the food's state
     * (its {@code FoodState}, knead, heat) carry over -- dough rolled thin is the same dough, not a fresh one.
     */
    private static ItemStack food(Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level) {
        if (check(Station.TOOL, option, contents, materials, level, true).isEmpty())
            return ItemStack.EMPTY;
        // shortcut: the first piece's state stands for the pile; pieces of differing age would want the worst kept.
        ItemStack first = contents.stream().filter(s -> !s.isEmpty()).findFirst().orElseThrow();
        int count = contents.stream().mapToInt(ItemStack::getCount).sum();
        ItemStack out = first.transmuteCopy(BuiltInRegistries.ITEM.get(option.shaping().item()), count);
        // A named shape (loaf, flat, roll) rides the dough as a choice, not as another item: the dish reads it as the part's variant.
        String shape = option.variant().name().isEmpty() ? option.shaping().base() : option.variant().name();
        if (shape.isEmpty())
            out.remove(FDataComponents.FOOD_SHAPE.get());
        else
            out.set(FDataComponents.FOOD_SHAPE.get(), shape);
        return out;
    }

    /** The part {@code contents} makes in a shape that is not forged: no flaws, out as soon as the stock is in. */
    public static ItemStack make(Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level) {
        return make(Station.TOOL, option, contents, materials, level);
    }

    public static ItemStack make(Station station, Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level) {
        if (option.forged())
            return ItemStack.EMPTY;
        if (!(BuiltInRegistries.ITEM.get(option.shaping().item()) instanceof ModularPartItem))
            return food(option, contents, materials, level);
        return check(station, option, contents, materials, level).flatMap(s -> part(option, s, 0, level)).orElse(ItemStack.EMPTY);
    }

    /** Forms leftover stock comes back as, largest first: an ingot of metal, a chip of cobble, then a nugget. */
    private static final List<String> CHANGE = List.of("ingot", "cobble", "nugget");

    /**
     * {@code mb} of {@code what} as items smaller than the form it came in: what the bench hands back when a shape spends
     * part of a block. Largest form first, so a stone block less one part comes back as cobble and a metal block as ingots.
     * A remainder no form of the material holds is lost.
     */
    public static List<ItemStack> change(Material.Located what, int mb) {
        List<ItemStack> out = new ArrayList<>();
        int own = FormItems.volume(what.form());
        for (String form : CHANGE) {
            int v = FormItems.volume(form);
            Optional<net.minecraft.world.item.Item> item = what.material().item(what.id(), form);
            // Only smaller than the stock's own form: a leftover is never the thing that was put in.
            if (mb < v || v >= own || item.isEmpty())
                continue;
            int n = mb / v;
            mb -= n * v;
            for (int max = item.get().getDefaultMaxStackSize(); n > 0; n -= Math.min(n, max))
                out.add(new ItemStack(item.get(), Math.min(n, max)));
        }
        return out;
    }
}
