// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.Level;

/**
 * Philosophy 15's compatibility shim: any vanilla or modded cooking recipe becomes a one-input thermal row ({@link #rowsFor}), so reworking
 * the furnaces cannot make a single existing recipe uncraftable.
 *
 * <p>Vanilla keeps three recipe types, {@code smelting}, {@code blasting} and {@code smoking}, as a stand-in for "which appliance this
 * belongs in". §15 says that stand-in has to go: nothing may declare the Smoker a food machine or the Blast Furnace an ore machine. So every
 * vessel reads all three, and a recipe's type survives only as a thermal hint: smoking reads as food (a floor at ambient, a ceiling that
 * ruins), blasting and smelting as metallurgy (a floor and an optimum, nothing above). Of several rows for one input,
 * {@link ThermalProcessTable#findSingle} runs the one the vessel's current heat suits best, so an ore's blasting profile only wins in a vessel
 * hot enough to be near it, and the shortest recipe never wins by default.
 */
public final class VanillaFallback {

    private static final List<RecipeType<? extends AbstractCookingRecipe>> TYPES =
            List.of(RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING);

    private VanillaFallback() {
    }

    private static net.minecraft.world.item.crafting.RecipeManager builtFor;
    private static List<ThermalProcess> built = List.of();
    private static final java.util.Map<net.minecraft.world.item.Item, List<ThermalProcess>> BY_ITEM = new java.util.HashMap<>();

    /**
     * Every vanilla or modded cooking recipe as a one-input thermal row, so a vessel runs them the way it runs any row: smoking reads as food
     * (a floor at ambient, a ceiling that ruins), blasting and smelting as metallurgy (a floor and an optimum, nothing above), the recipe's
     * cooking time as the hold. These rows are built from the recipe manager, never authored, and are only a fallback for an input no authored
     * row takes.
     */
    public static synchronized List<ThermalProcess> rowsFor(Level level, ItemStack stack) {
        allRows(level);
        return BY_ITEM.computeIfAbsent(stack.getItem(), item -> {
            ItemStack probe = new ItemStack(item);
            return built.stream().filter(p -> p.inputs().get(0).test(probe)).toList();
        });
    }

    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> IRON_BEARERS =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, io.github.soundgoodizerfan.feedback.Feedback.id("iron_bearers"));

    /**
     * Whether a vanilla smelting row would hand over a metal the ore does not melt into: an ore (an item of the {@code iron_bearers} tag, or
     * any bearer whose cooking result is not the metal it bears) cooked straight to an ingot skips bloom, blast and puddling. Such a row is
     * not offered; the ore goes through its own processes.
     */
    private static boolean bypassesReduction(net.minecraft.world.item.crafting.Ingredient input, ItemStack result) {
        var materials = MaterialIngredient.materialsNow();
        var made = Material.find(materials, result);
        for (ItemStack stack : input.getItems()) {
            if (stack.is(IRON_BEARERS))
                return true;
            var ore = Material.find(materials, stack);
            if (ore.isEmpty() || ore.get().material().bears().isEmpty())
                continue;
            var bears = ore.get().material().bears().entrySet().stream().max(java.util.Map.Entry.comparingByValue()).get().getKey();
            if (made.isEmpty() || !made.get().id().equals(bears) && !ore.get().id().equals(bears))
                return true;
        }
        return false;
    }

    /** Every cooking recipe of {@code level}'s recipe manager as a row (see {@link #rowsFor}). */
    @SuppressWarnings("unchecked")
    public static synchronized List<ThermalProcess> allRows(Level level) {
        var manager = level.getRecipeManager();
        if (manager != builtFor) {
            builtFor = manager;
            BY_ITEM.clear();
            List<ThermalProcess> all = new java.util.ArrayList<>();
            for (RecipeType<? extends AbstractCookingRecipe> type : TYPES)
                for (RecipeHolder<? extends AbstractCookingRecipe> holder : manager.getAllRecipesFor((RecipeType<AbstractCookingRecipe>) type)) {
                    var recipe = holder.value();
                    if (recipe.getIngredients().isEmpty() || bypassesReduction(recipe.getIngredients().get(0), recipe.getResultItem(level.registryAccess())))
                        continue;
                    List<net.minecraft.world.item.crafting.Ingredient> inputs = List.of(recipe.getIngredients().get(0));
                    ItemStack result = recipe.getResultItem(level.registryAccess());
                    float hold = recipe.getCookingTime();
                    if (recipe instanceof SmokingRecipe)
                        all.add(new ThermalProcess(inputs, FTuning.AMBIENT_TU, FTuning.SMOKING_OPTIMAL_TU, FTuning.FOOD_MAX_TU, hold,
                                new io.github.soundgoodizerfan.feedback.core.unit.TuRate(Float.MAX_VALUE), result, FTuning.FOOD_MAX_TU,
                                ItemStack.EMPTY, net.neoforged.neoforge.fluids.FluidStack.EMPTY, false));
                    else
                        all.add(new ThermalProcess(inputs, FTuning.METAL_MIN_TU,
                                recipe instanceof BlastingRecipe ? FTuning.BLASTING_OPTIMAL_TU : FTuning.SMELTING_OPTIMAL_TU,
                                new Tu(Float.MAX_VALUE), hold, new io.github.soundgoodizerfan.feedback.core.unit.TuRate(Float.MAX_VALUE),
                                result, new Tu(Float.MAX_VALUE), ItemStack.EMPTY, net.neoforged.neoforge.fluids.FluidStack.EMPTY, false));
                }
            built = List.copyOf(all);
        }
        return built;
    }
}
