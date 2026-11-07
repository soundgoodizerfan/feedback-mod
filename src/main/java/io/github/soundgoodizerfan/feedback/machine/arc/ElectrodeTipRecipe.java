// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.arc;

import io.github.soundgoodizerfan.feedback.process.Material;
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
 * A bar of an electrode-capable material over an electrode: the electrode with that material as its tip ({@link ElectrodeItem#tipped}). A tip
 * that can take more than the rod lifts the rod's limit, so a graphite electrode tipped with tungsten wears far slower near tungsten's melt.
 * A tip of the rod's own material, or of one that can take less, changes nothing and is refused.
 */
public class ElectrodeTipRecipe extends CustomRecipe {

    public ElectrodeTipRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return result(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = result(input);
        return result == null ? ItemStack.EMPTY : result;
    }

    private static ItemStack result(CraftingInput input) {
        if (input.width() != 1 || input.height() != 2)
            return null;
        ItemStack rod = input.getItem(0, 1);
        if (!(rod.getItem() instanceof ElectrodeItem) || ElectrodeItem.tip(rod) != null)
            return null;
        var bar = Material.find(MaterialIngredient.materialsNow(), input.getItem(0, 0));
        var base = rod.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get());
        if (bar.isEmpty() || base == null || !ElectrodeRecipe.STOCK.contains(bar.get().form())
                || bar.get().material().electrodeLimit().isEmpty())
            return null;
        ResourceLocation tip = bar.get().id();
        if (tip.equals(base.material()) || bar.get().material().electrodeLimit().get().value() <= ElectrodeItem.limitTu(rod))
            return null;
        ItemStack out = ElectrodeItem.tipped(base.material(), tip);
        out.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.TOOL_CONDITION.get(), ElectrodeItem.condition(rod));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.ARC_ELECTRODE_TIP.get();
    }
}
