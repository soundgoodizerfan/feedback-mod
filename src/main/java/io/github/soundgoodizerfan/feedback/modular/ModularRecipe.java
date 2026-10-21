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

import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.weapon.GunAssembly;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * The crafting recipe for guns, and only guns. Tools, weapons, shields, bows and armour moved to
 * the Tool Table ({@link ToolTableMenu}); the gun waits for a table of its own, and until then it
 * is made at a crafting grid, as it was:
 *
 * <ul>
 *   <li><b>Assembly.</b> A gun blueprint in the grid hands the grid to {@link GunAssembly}, which
 *       reads the parts by what they are made of and bakes the gun from them, not from the paper.</li>
 *   <li><b>Refit.</b> A built gun and a fitting part or sight plates: the part displaces the one
 *       it replaces, which comes back in the grid.</li>
 * </ul>
 *
 * <p>A type that says {@code blueprint} is gated by a blueprint of it; the gun is such a type
 * ({@code modular_type/gun.json}) and its blueprint is its pattern. The blueprint stays in the grid.
 */
public class ModularRecipe extends CustomRecipe {

    public ModularRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !build(input, ClientMaterials.on(level), ModularTypes.on(level)).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return build(input, MaterialTable.get().materials(), ModularTypes.get().types());
    }

    public static ItemStack build(CraftingInput input, Map<ResourceLocation, Material> materials,
                                   Map<ResourceLocation, ModularType> types) {
        if (materials.isEmpty() || types.isEmpty())
            return ItemStack.EMPTY;
        ModularType gun = types.get(Feedback.id("gun"));
        if (gun == null || !gun.blueprint())
            return ItemStack.EMPTY;
        ItemStack drawn = GunAssembly.blueprint(input);
        if (drawn != null)
            return GunAssembly.assemble(input, drawn, materials);
        GunAssembly.Refit refit = GunAssembly.refit(input, materials);
        return refit != null ? GunAssembly.refitted(refit) : ItemStack.EMPTY;
    }

    /** A blueprint stays in the grid: a design is copied, not consumed. A refit hands back the part it displaced. */
    @Override
    public net.minecraft.core.NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        net.minecraft.core.NonNullList<ItemStack> out = super.getRemainingItems(input);
        for (int i = 0; i < input.size(); i++)
            if (input.getItem(i).is(FModular.BLUEPRINT.get()))
                out.set(i, input.getItem(i).copyWithCount(1));
        GunAssembly.Refit refit = GunAssembly.refit(input, MaterialTable.get().materials());
        if (refit != null)
            refit.displaced().ifPresent(s -> out.set(refit.partSlot(), s));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FModular.RECIPE.get();
    }
}
