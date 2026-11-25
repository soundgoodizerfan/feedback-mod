// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.Map;

import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A part whose shape is forged to a pattern (a variant with a rhythm) and a metal plate make a cam for that pattern; the part comes back. You can
 * only cut a cam for a shape you have forged, so a machine plays what a hand learned first. Nothing is named: the part is read for its variant, the
 * backing is any metal plate form.
 */
public class HammerCamRecipe extends CustomRecipe {

    public HammerCamRecipe(CraftingBookCategory category) {
        super(category);
    }

    /** The forged shape's name in the grid (one forged part, one plate, nothing else), or null. */
    private static String read(CraftingInput input, Map<ResourceLocation, Material> materials, Map<ResourceLocation, ModularType> types) {
        String pattern = null;
        int plates = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            var made = stack.get(FDataComponents.MADE_OF.get());
            if (made != null && !made.variant().isEmpty() && forged(made.variant(), types)) {
                if (pattern != null)
                    return null;
                pattern = made.variant();
                continue;
            }
            var found = Material.find(materials, stack.getItem());
            if (found.isPresent() && found.get().form().equals("plate"))
                plates++;
            else
                return null;
        }
        return plates == 1 ? pattern : null;
    }

    private static boolean forged(String variant, Map<ResourceLocation, ModularType> types) {
        for (ModularType t : types.values())
            for (ModularType.Role r : t.roles().values())
                if (r.part().isPresent())
                    for (ModularType.Variant v : r.part().get().variants())
                        if (v.name().equals(variant) && v.cells() <= 0f && !v.rhythm().isEmpty())
                            return true;
        return false;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input, ClientMaterials.on(level), ModularTypes.on(level)) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        var level = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        Map<ResourceLocation, ModularType> types = level == null ? Map.of() : ModularTypes.on(level.overworld());
        String pattern = read(input, MaterialTable.get().materials(), types);
        if (pattern == null)
            return ItemStack.EMPTY;
        ItemStack out = new ItemStack(FModular.HAMMER_CAM.get());
        out.set(FWeapons.HAMMER_CAM.get(), pattern);
        return out;
    }

    /** The forged part is not used up: only the plate is. */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> out = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++)
            if (input.getItem(i).has(FDataComponents.MADE_OF.get()))
                out.set(i, input.getItem(i).copyWithCount(1));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.HAMMER_CAM.get();
    }
}
