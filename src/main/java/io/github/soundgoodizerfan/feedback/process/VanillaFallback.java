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
 * Philosophy 15's fallback: the compatibility shim that lets the Furnace, Smoker and Crude Blast
 * Furnace attempt any vanilla or modded cooking recipe, so that reworking the three of them
 * cannot make a single existing recipe uncraftable.
 *
 * <h2>Whitelists come off here, on purpose</h2>
 * Vanilla keeps three separate recipe types -- {@code smelting}, {@code blasting},
 * {@code smoking} -- as a stand-in for "which appliance this belongs in". §15 says that stand-in
 * has to go: nothing may declare the Smoker a food machine or the Blast Furnace an ore machine,
 * three numbers in {@code FTuning} do that instead. So every vessel searches all three recipe
 * types for a match on its own input, and whichever one exists is a fact about the <em>item</em>,
 * never about which block it happens to be sitting in.
 * <p>
 * A recipe's type is still read once it is found, but only as a thermal hint: vanilla recipe types become thermal hints: {@code smoking} reads as
 * food-like (low, stable heat, a real spoil ceiling), {@code blasting} as metallurgical (rewards
 * a hotter environment, no food-style ceiling), and {@code smelting} as the generic default --
 * three suitability curves in {@code FTuning}, never a machine check.
 *
 * <h2>Do not let the shortest recipe win</h2>
 * An earlier version of {@link #find} picked whichever of the three matching recipes had the
 * shortest declared cooking time, once, and ran that recipe forever -- which made an ore's
 * blast-furnace time win in every vessel, including a plain Furnace that can never reach
 * blasting's optimum. That turns a vessel's
 * emergent specialisation (§15) into a hidden recipe rule. This version instead evaluates every
 * matching type's suitability against the vessel's <em>current</em> temperature, every tick, and
 * runs whichever one is actually thriving there -- so an iron ore's blasting profile only wins in
 * a vessel that is actually hot enough to be near it, exactly the way the smoker and blast
 * furnace are already supposed to specialise without either block knowing the other exists.
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
                    if (recipe.getIngredients().isEmpty())
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
