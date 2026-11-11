// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import io.github.soundgoodizerfan.feedback.modular.ModularFit;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.ToolTableAssembly;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;
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
 * The plain crafting table's assembly: the parts in the 3x3 make every simple (non-{@code blueprint}) modular type, positions ignored and
 * slots of one material and form merged into a role's count (one item per slot, as a crafting grid spends them). Complex types are the
 * Engineering Table's. No fasteners, tools or knowledge are asked; the item starts at the hand fit.
 */
public class ModularCraftingRecipe extends CustomRecipe {

    public ModularCraftingRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static Map<ResourceLocation, ModularType> simple() {
        Map<ResourceLocation, ModularType> out = new TreeMap<>();
        ModularTypes.known().forEach((id, t) -> {
            // A gated type (one with a school) is complex: only the Engineering Table, which checks the treatise, assembles it.
            if (!t.blueprint() && t.school().isEmpty() && t.family() != ModularType.Family.DISH)
                out.put(id, t);
        });
        return out;
    }

    private static Optional<ItemStack> make(CraftingInput input) {
        List<ItemStack> slots = new ArrayList<>();
        for (int i = 0; i < input.size(); i++)
            if (!input.getItem(i).isEmpty())
                slots.add(input.getItem(i).copyWithCount(1));
        if (slots.isEmpty())
            return Optional.empty();
        return ToolTableAssembly.infer(slots, ItemStack.EMPTY, MaterialIngredient.materialsNow(), simple(), t -> true, ModularFit.Start.NONE)
                .map(ToolTableAssembly.Match::tool);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return make(input).isPresent();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return make(input).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 1 && height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.MODULAR_ASSEMBLY.get();
    }
}
