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
import java.util.WeakHashMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * What the woodworking table's Saw tab makes of a plank or a log: every crafting
 * recipe whose ingredients are all planks or sticks, so stairs, slabs, doors, fences, signs, boats and bowls are
 * made at the bench and not in the grid, plus a log stripped and a log sawn to planks. A log counts as four
 * planks; a stick as half a plank (two planks make four sticks). The recipe's own result is what comes out.
 *
 * <p>Read off the recipe manager, so a pack's recipes join without a line here; the list depends only on the
 * item put in, and is cached per recipe manager (a reload makes a new one).
 */
public final class WoodCuts {

    /** Planks one log is worth. */
    public static final int LOG_PLANKS = 4;

    /**
     * One thing the saw can make. {@code cost} is in planks (one log, for a strip); {@code strip} marks the log-for-log
     * case, where the input is spent one for one and nothing is left over.
     */
    public record Cut(ItemStack result, int cost, boolean strip) {

        /** Planks one item of {@code in} is worth here. */
        public int per(ItemStack in) {
            return strip || !in.is(ItemTags.LOGS) ? 1 : LOG_PLANKS;
        }

        /** Items of {@code in} one cut spends: whole logs when a log is put in. */
        public int inputs(ItemStack in) {
            int per = per(in);
            return (cost + per - 1) / per;
        }

        /** Planks left over from the last log, returned to the player. */
        public int leftover(ItemStack in) {
            return inputs(in) * per(in) - cost;
        }
    }

    private static final Map<RecipeManager, Map<Item, List<Cut>>> CACHE = new WeakHashMap<>();

    private WoodCuts() {
    }

    /** The cuts of {@code in}: empty unless it is a plank or a log. */
    public static List<Cut> of(Level level, ItemStack in) {
        if (in.isEmpty() || !(in.is(ItemTags.PLANKS) || in.is(ItemTags.LOGS)))
            return List.of();
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(level.getRecipeManager(), m -> new java.util.HashMap<>())
                    .computeIfAbsent(in.getItem(), item -> build(level, new ItemStack(item)));
        }
    }

    /** The plank a log saws to: what vanilla's own one-ingredient planks recipe for it makes. */
    public static Optional<Item> planksOf(Level level, ItemStack log) {
        for (RecipeHolder<CraftingRecipe> r : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            ItemStack out = r.value().getResultItem(level.registryAccess());
            if (out.is(ItemTags.PLANKS) && single(r.value().getIngredients()) != null && single(r.value().getIngredients()).test(log))
                return Optional.of(out.getItem());
        }
        return Optional.empty();
    }

    private static Ingredient single(List<Ingredient> all) {
        List<Ingredient> some = all.stream().filter(i -> !i.isEmpty()).toList();
        return some.size() == 1 ? some.get(0) : null;
    }

    private static List<Cut> build(Level level, ItemStack in) {
        boolean log = in.is(ItemTags.LOGS);
        ItemStack plank = in;
        List<Cut> out = new ArrayList<>();
        if (log) {
            Optional<Item> planks = planksOf(level, in);
            if (planks.isEmpty())
                return List.of();
            plank = new ItemStack(planks.get());
            stripped(in.getItem()).ifPresent(s -> out.add(new Cut(new ItemStack(s), 1, true)));
            out.add(new Cut(new ItemStack(planks.get(), LOG_PLANKS), LOG_PLANKS, false));
        }
        Set<Item> seen = new HashSet<>();
        out.forEach(c -> seen.add(c.result().getItem()));
        ItemStack stick = new ItemStack(Items.STICK);
        for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            CraftingRecipe r = holder.value();
            ItemStack result = r.getResultItem(level.registryAccess());
            if (result.isEmpty() || result.is(ItemTags.PLANKS))
                continue;
            int halves = 0;
            boolean usesPlanks = false, ok = true, any = false;
            for (Ingredient i : r.getIngredients()) {
                if (i.isEmpty())
                    continue;
                any = true;
                if (i.test(plank)) {
                    halves += 2;
                    usesPlanks = true;
                } else if (i.test(stick))
                    halves++;
                else {
                    ok = false;
                    break;
                }
            }
            if (ok && any && usesPlanks && seen.add(result.getItem()))
                out.add(new Cut(result.copy(), (halves + 1) / 2, false));
        }
        return List.copyOf(out);
    }

    /**
     * The stripped form of a log or wood block, if there is one: the same id with {@code stripped_} in front, which is
     * how vanilla and the mods that follow it name them.
     * shortcut: by name; vanilla's strippables map is protected, and an addon that names its own differently is not found, until one asks.
     */
    private static Optional<Item> stripped(Item log) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(log);
        if (id.getPath().startsWith("stripped_"))
            return Optional.empty();
        return BuiltInRegistries.ITEM.getOptional(id.withPath("stripped_" + id.getPath())).filter(i -> i != Items.AIR);
    }
}
