// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.bearing;

import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * A Slider Joint and four wires of one metal, in any arrangement -> that slider with the metal wound
 * into its spring. The metal's yield strength picks the spring's band when the joint is placed
 * ({@link #band}): soft metal, soft ride; spring steel, stiff. A slider with no coil fitted is the
 * stock band. The wire is the form every metal already has ({@code <metal>_wire}); the spring is a
 * choice of what it is made of, not a second item per metal.
 */
public class SliderSpringRecipe extends CustomRecipe {

    public static final int WIRES = 4;

    public SliderSpringRecipe(CraftingBookCategory category) {
        super(category);
    }

    /** The metal whose wire {@code stack} is, from its item name, if that metal has a yield strength; else null. */
    @Nullable
    public static ResourceLocation metalOf(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!key.getPath().endsWith("_wire"))
            return null;
        ResourceLocation metal = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), key.getPath().substring(0, key.getPath().length() - 5));
        var material = MaterialTable.get().materials().get(metal);
        return material != null && material.mechanical().yieldStrength().isPresent() ? metal : null;
    }

    /** The spring band for {@code metal}: 0 soft, 1 stock, 2 stiff, by yield strength. */
    public static int band(@Nullable ResourceLocation metal) {
        var material = metal == null ? null : MaterialTable.get().materials().get(metal);
        if (material == null || material.mechanical().yieldStrength().isEmpty())
            return 1;
        float yield = material.mechanical().yieldStrength().get().value();
        return yield < MachineTuning.SPRING_SOFT_BELOW_YIELD ? 0 : yield >= MachineTuning.SPRING_STIFF_FROM_YIELD ? 2 : 1;
    }

    @Nullable
    private static ResourceLocation read(CraftingInput input, ItemStack[] slider) {
        slider[0] = null;
        ResourceLocation metal = null;
        int wires = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty())
                continue;
            if (s.is(FItems.SLIDER_JOINT.get()) && slider[0] == null)
                slider[0] = s;
            else {
                ResourceLocation m = metalOf(s);
                if (m == null || metal != null && !m.equals(metal))
                    return null;
                metal = m;
                wires++;
            }
        }
        return slider[0] != null && wires == WIRES && !metal.equals(slider[0].get(FDataComponents.SLIDER_SPRING.get())) ? metal : null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input, new ItemStack[1]) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack[] slider = new ItemStack[1];
        ResourceLocation metal = read(input, slider);
        if (metal == null)
            return ItemStack.EMPTY;
        ItemStack out = slider[0].copyWithCount(1);
        out.set(FDataComponents.SLIDER_SPRING.get(), metal);
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 5;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.SLIDER_SPRING.get();
    }
}
