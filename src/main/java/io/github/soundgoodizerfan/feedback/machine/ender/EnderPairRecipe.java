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
package io.github.soundgoodizerfan.feedback.machine.ender;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * One pearl and two metal plates (any arrangement) make two ender ports that carry the same pair id:
 * the pearl is the capital, one port stays and the other is carried away. The plates are told by their
 * form, not their material.
 */
public class EnderPairRecipe extends CustomRecipe {

    /** Every item that is a pearl by what it does: the real one and the synthetic. */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> SPATIAL_ANCHORS = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, io.github.soundgoodizerfan.feedback.Feedback.id("spatial_anchors"));

    public EnderPairRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        var materials = ClientMaterials.on(level);
        int pearls = 0, plates = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty())
                continue;
            if (s.is(SPATIAL_ANCHORS))
                pearls++;
            else if (io.github.soundgoodizerfan.feedback.process.Material.find(materials, s).map(l -> l.form().equals("plate")).orElse(false))
                plates++;
            else
                return false;
        }
        return pearls == 1 && plates == 2;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack out = new ItemStack(FItems.ENDER_PORT.get(), 2);
        CompoundTag tag = new CompoundTag();
        tag.putUUID(EnderPortBlockEntity.PAIR_KEY, UUID.randomUUID());
        out.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.ENDER_PAIR.get();
    }
}
