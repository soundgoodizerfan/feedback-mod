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
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.HandDeformationRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's one crafting-table recipe serializer, and it is likely to stay the only one.
 *
 * <p>Feedback's processes are datapack tables rather than {@code RecipeType}s, on purpose -- a
 * machine that owned a recipe list would know when it had finished, which is the one thing this
 * mod's machines must never do. What lives here is the exception that proves it: hand work, where
 * the crafting table is vanilla's recipe machine and the player is the one who decides to stop.
 *
 * @see io.github.soundgoodizerfan.feedback.process.HandDeformationRecipe
 */
public class FRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Feedback.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<HandDeformationRecipe>>
            HAND_DEFORMATION = SERIALIZERS.register("hand_deformation",
                    () -> new SimpleCraftingRecipeSerializer<>(HandDeformationRecipe::new));

    /** Blueprint + its material's ingots -> Engine (vehicles spec section 6). */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.vehicle.engine.EngineRecipe>>
            ENGINE = SERIALIZERS.register("engine",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.vehicle.engine.EngineRecipe::new));

    /** A magazine and loose rounds -> that magazine, fuller (guns spec section 5). */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.weapon.MagazineLoadRecipe>>
            MAGAZINE_LOAD = SERIALIZERS.register("magazine_load",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.weapon.MagazineLoadRecipe::new));

    /** Two rods of a metal and a grip -> tongs made of exactly those (hot-contact protection). */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.item.TongsRecipe>>
            TONGS = SERIALIZERS.register("tongs",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.item.TongsRecipe::new));

    /** Plates and straps -> a bracer, sash or back piece baked from them (combat gear). */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.combat.gear.GearRecipe>>
            GEAR = SERIALIZERS.register("gear",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.combat.gear.GearRecipe::new));

    /** Three wires of one conductor and three paper -> cable in that conductor. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.machine.cable.CableRecipe>>
            CABLE = SERIALIZERS.register("cable",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.machine.cable.CableRecipe::new));

    public static final DeferredRegister<net.neoforged.neoforge.common.crafting.IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.INGREDIENT_TYPES, Feedback.MOD_ID);

    /** Any item of a form whose material's figure is in range -- see {@code MaterialIngredient}. */
    public static final DeferredHolder<net.neoforged.neoforge.common.crafting.IngredientType<?>,
            net.neoforged.neoforge.common.crafting.IngredientType<io.github.soundgoodizerfan.feedback.process.MaterialIngredient>>
            MATERIAL_INGREDIENT = INGREDIENT_TYPES.register("material",
                    () -> io.github.soundgoodizerfan.feedback.process.MaterialIngredient.TYPE);

    private FRecipes() {
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
        INGREDIENT_TYPES.register(modBus);
    }
}
