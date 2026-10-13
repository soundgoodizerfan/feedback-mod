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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A Gun Blueprint and its material's ingots make that Gun; the Blueprint is kept. The ingot is
 * the material's {@code ingot} form, asked of the material system -- no gun-side list -- and the
 * count is {@link GunDesign#ingots()}, a placeholder ({@code EngineRecipe}'s shape). The gun
 * carries the blueprint's design <em>and</em> its baked figures, so it needs no first-tick bake.
 */
public class GunRecipe extends CustomRecipe {

    public GunRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack blueprint = blueprint(input);
        if (blueprint == null)
            return false;
        GunDesign design = blueprint.get(FWeapons.GUN_DESIGN.get());
        var material = ClientMaterials.on(level).get(design.material());
        if (material == null)
            return false;
        Item ingot = material.item(design.material(), "ingot").orElse(null);
        if (ingot == null)
            return false;
        int ingots = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty() || s == blueprint)
                continue;
            if (!s.is(ingot))
                return false;
            ingots++;
        }
        return ingots == design.ingots();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack blueprint = blueprint(input); // matches() has already checked the ingots
        if (blueprint == null)
            return ItemStack.EMPTY;
        ItemStack gun = new ItemStack(FWeapons.GUN.get());
        gun.set(FWeapons.GUN_DESIGN.get(), blueprint.get(FWeapons.GUN_DESIGN.get()));
        var figures = blueprint.get(FWeapons.GUN_FIGURES.get());
        if (figures != null)
            gun.set(FWeapons.GUN_FIGURES.get(), figures);
        return gun;
    }

    /** The one blueprint in the grid that carries a design, or null if there is none or more than one. */
    @Nullable
    private static ItemStack blueprint(CraftingInput input) {
        ItemStack found = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.is(FWeapons.GUN_BLUEPRINT.get()) && s.has(FWeapons.GUN_DESIGN.get())) {
                if (found != null)
                    return null;
                found = s;
            }
        }
        return found;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> out = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++)
            if (input.getItem(i).is(FWeapons.GUN_BLUEPRINT.get()))
                out.set(i, input.getItem(i).copyWithCount(1));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.GUN.get();
    }
}
