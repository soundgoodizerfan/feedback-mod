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

    /** Gun Blueprint + its material's ingots -> Gun (guns spec section 4). */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.weapon.GunRecipe>>
            GUN = SERIALIZERS.register("gun",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.weapon.GunRecipe::new));

    /** A magazine and loose rounds -> that magazine, fuller (guns spec section 5). */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<io.github.soundgoodizerfan.feedback.weapon.MagazineLoadRecipe>>
            MAGAZINE_LOAD = SERIALIZERS.register("magazine_load",
                    () -> new SimpleCraftingRecipeSerializer<>(io.github.soundgoodizerfan.feedback.weapon.MagazineLoadRecipe::new));

    private FRecipes() {
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }
}
