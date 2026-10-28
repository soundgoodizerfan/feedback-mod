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
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Putting a tool together, and reworking one, in an ordinary crafting grid. This is what the Tool Table's Assemble and Rework
 * tabs were: the same inference ({@link ToolTableAssembly#infer}) and the same rework ({@link ModularRework#rework}), fed from
 * the grid.
 *
 * <ul>
 *   <li><b>No finished tool in the grid</b>: the pieces are the parts, the parts imply the tool. Pieces alike in every way
 *       share a slot (a role that wants two of a part is two of it in two cells); a blueprint in the grid is the plan.</li>
 *   <li><b>One finished tool and other pieces</b>: the pieces are stock to mend it, swap a part or add a tip or binding.</li>
 * </ul>
 *
 * Taking a tool apart is not a recipe (it gives back several things), and there is no hand hammer to wear here. Guns and
 * dishes are not made here: they have their own tables.
 */
public class ModularAssemblyRecipe extends CustomRecipe {

    public ModularAssemblyRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !make(input, ClientMaterials.on(level), ModularTypes.on(level)).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        // Server-side only (a crafting menu makes its result there): the server's tables.
        return make(input, io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials(), ModularTypes.get().types());
    }

    private static ItemStack make(CraftingInput input, Map<ResourceLocation, Material> materials, Map<ResourceLocation, ModularType> all) {
        if (materials.isEmpty() || all.isEmpty())
            return ItemStack.EMPTY;
        Map<ResourceLocation, ModularType> types = ToolTableAssembly.ofKind(all, ToolTableAssembly.Kind.TOOL);
        ItemStack tool = ItemStack.EMPTY, plan = ItemStack.EMPTY;
        List<ItemStack> pieces = new ArrayList<>();
        for (ItemStack s : input.items()) {
            if (s.isEmpty())
                continue;
            if (ModularStacks.design(s) != null) {
                if (!tool.isEmpty())
                    return ItemStack.EMPTY;
                tool = s;
            } else if (s.is(FModular.BLUEPRINT.get())) {
                if (!plan.isEmpty())
                    return ItemStack.EMPTY;
                plan = s;
            } else {
                ItemStack alike = pieces.stream().filter(p -> ItemStack.isSameItemSameComponents(p, s)).findFirst().orElse(null);
                if (alike != null)
                    alike.grow(1);
                else
                    pieces.add(s.copyWithCount(1));
            }
        }
        if (!tool.isEmpty())
            return ToolTableAssembly.kindOf(tool) != ToolTableAssembly.Kind.TOOL || !plan.isEmpty() ? ItemStack.EMPTY
                    : ModularRework.rework(tool, pieces, materials, types).orElse(ItemStack.EMPTY);
        if (pieces.size() > ToolTableAssembly.SLOTS)
            return ItemStack.EMPTY;
        return ToolTableAssembly.infer(pieces, plan, materials, types).map(ToolTableAssembly.Match::tool).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.MODULAR_ASSEMBLY.get();
    }
}
