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
 * A recipe's type is still read once it is found, but only as a thermal hint: {@code
 * tpu_spec_doc.md}'s "Vanilla Recipe Types Become Thermal Hints" reads {@code smoking} as
 * food-like (low, stable heat, a real spoil ceiling), {@code blasting} as metallurgical (rewards
 * a hotter environment, no food-style ceiling), and {@code smelting} as the generic default --
 * three suitability curves in {@code FTuning}, never a machine check.
 *
 * <h2>Do not let the shortest recipe win</h2>
 * An earlier version of {@link #find} picked whichever of the three matching recipes had the
 * shortest declared cooking time, once, and ran that recipe forever -- which made an ore's
 * blast-furnace time win in every vessel, including a plain Furnace that can never reach
 * blasting's optimum. {@code tpu_spec_doc.md} calls this out by name: that turns a vessel's
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

    /** A candidate vanilla recipe together with how well the vessel's current temperature suits
     * it -- see the class doc on why this replaces returning a single fixed recipe. */
    public record Match(RecipeHolder<AbstractCookingRecipe> recipe, float suitability) {
    }

    /**
     * Every vanilla/modded cooking recipe across all three types that matches this input,
     * evaluated against {@code currentTemperature} and returning whichever is currently most
     * suitable -- ties (including "none of them are in band right now") broken by shortest
     * declared cooking time, the same tie-break the old single-recipe version always used.
     */
    @SuppressWarnings("unchecked")
    public static Optional<Match> find(Level level, ItemStack input, Tu currentTemperature) {
        if (input.isEmpty())
            return Optional.empty();

        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Match best = null;
        for (RecipeType<? extends AbstractCookingRecipe> type : TYPES) {
            RecipeHolder<AbstractCookingRecipe> match = (RecipeHolder<AbstractCookingRecipe>) (RecipeHolder<?>)
                    level.getRecipeManager()
                            .getRecipeFor((RecipeType<AbstractCookingRecipe>) type, recipeInput, level)
                            .orElse(null);
            if (match == null)
                continue;
            float suitability = profileSuitability(match, currentTemperature);
            if (best == null
                    || suitability > best.suitability()
                    || (suitability == best.suitability()
                        && match.value().getCookingTime() < best.recipe().value().getCookingTime()))
                best = new Match(match, suitability);
        }
        return Optional.ofNullable(best);
    }

    /** Whether vanilla itself already calls this recipe food, per its {@code smoking} type. */
    public static boolean isFood(RecipeHolder<AbstractCookingRecipe> recipe) {
        return recipe.value() instanceof SmokingRecipe;
    }

    /**
     * The thermal hint's suitability at this temperature -- see the class doc. Food uses a floor
     * of ambient rather than zero, since nothing in this engine drops below it anyway and a
     * min/optimal/max triangle wants a real floor to ramp from.
     */
    private static float profileSuitability(RecipeHolder<AbstractCookingRecipe> recipe, Tu currentTemperature) {
        if (isFood(recipe))
            return ThermalProcess.suitability(currentTemperature.value(),
                    FTuning.AMBIENT_TU.value(), FTuning.SMOKING_OPTIMAL_TU.value(), FTuning.FOOD_MAX_TU.value());
        Tu optimal = recipe.value() instanceof BlastingRecipe ? FTuning.BLASTING_OPTIMAL_TU : FTuning.SMELTING_OPTIMAL_TU;
        return ThermalProcess.suitability(currentTemperature.value(),
                FTuning.METAL_MIN_TU.value(), optimal.value(), Float.MAX_VALUE);
    }
}
