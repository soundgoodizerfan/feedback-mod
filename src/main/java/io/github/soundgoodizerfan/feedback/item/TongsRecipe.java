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
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Two rods of one metal and one grip, in any arrangement, make a pair of tongs made of exactly
 * those.
 *
 * <p>No item is named: a rod is whatever the material table says is the {@code rod} form of some
 * metal, a grip is whatever it says is a {@code grip} form (philosophy 11). That is also what lets
 * the component carry the two materials, so the tongs' limit is read from files and not from the
 * recipe.
 */
public class TongsRecipe extends CustomRecipe {

    public TongsRecipe(CraftingBookCategory category) {
        super(category);
    }

    private record Parts(ResourceLocation arms, ResourceLocation grip) {
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input, ClientMaterials.on(level)) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Parts parts = read(input, MaterialTable.get().materials());
        if (parts == null)
            return ItemStack.EMPTY;
        ItemStack out = new ItemStack(FItems.TONGS.get());
        out.set(FDataComponents.TONGS.get(), new Tongs(parts.arms(), parts.grip()));
        return out;
    }

    @Nullable
    private static Parts read(CraftingInput input, Map<ResourceLocation, Material> materials) {
        ResourceLocation arms = null;
        ResourceLocation grip = null;
        int rods = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            Material.Located found = Material.find(materials, stack.getItem()).orElse(null);
            if (found == null)
                return null;
            switch (found.form()) {
                case "rod" -> {
                    if (arms != null && !arms.equals(found.id()))
                        return null;
                    arms = found.id();
                    rods++;
                }
                case "grip" -> {
                    if (grip != null)
                        return null;
                    grip = found.id();
                }
                default -> {
                    return null;
                }
            }
        }
        return rods == 2 && grip != null ? new Parts(arms, grip) : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.TONGS.get();
    }
}
