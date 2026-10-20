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
package io.github.soundgoodizerfan.feedback.vehicle.engine;

import io.github.soundgoodizerfan.feedback.registry.FRecipes;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A Blueprint and its material's ingots make that Engine; the Blueprint is kept. Which item is
 * the ingot is the material's {@code ingot} form, asked of the material system -- no engine-side
 * list ({@code docs/feedback_vehicles_spec.md} section 6). The count is {@link EngineDesign#ingots()},
 * a placeholder.
 */
public class EngineRecipe extends CustomRecipe {

    public EngineRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return result(input, level) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        EngineDesign design = blueprint(input); // matches() has already checked the ingots
        if (design == null)
            return ItemStack.EMPTY;
        ItemStack engine = new ItemStack(FVehicles.ENGINE_ITEM.get());
        engine.set(FVehicles.ENGINE_DESIGN.get(), design);
        return engine;
    }

    /** The design this grid builds: one blueprint, and exactly its ingots, nothing else. */
    @org.jetbrains.annotations.Nullable
    private static EngineDesign result(CraftingInput input, Level level) {
        EngineDesign design = blueprint(input);
        if (design == null)
            return null;
        var material = io.github.soundgoodizerfan.feedback.process.ClientMaterials.on(level).get(design.material());
        if (material == null)
            return null;
        Item ingot = material.item(design.material(), "ingot").orElse(null);
        if (ingot == null)
            return null;
        int ingots = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty() || s.is(FVehicles.ENGINE_BLUEPRINT.get()))
                continue;
            if (!s.is(ingot))
                return null;
            ingots++;
        }
        return ingots == design.ingots() ? design : null;
    }

    /** The one blueprint's design, or null if there is none or more than one. */
    @org.jetbrains.annotations.Nullable
    private static EngineDesign blueprint(CraftingInput input) {
        EngineDesign design = null;
        for (int i = 0; i < input.size(); i++) {
            EngineDesign d = input.getItem(i).get(FVehicles.ENGINE_DESIGN.get());
            if (d != null && input.getItem(i).is(FVehicles.ENGINE_BLUEPRINT.get())) {
                if (design != null)
                    return null;
                design = d;
            }
        }
        return design;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> out = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++)
            if (input.getItem(i).is(FVehicles.ENGINE_BLUEPRINT.get()))
                out.set(i, input.getItem(i).copyWithCount(1));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.ENGINE.get();
    }
}
