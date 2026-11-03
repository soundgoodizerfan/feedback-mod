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


import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A simple metal tool hammered out hot: a striking hand tool (the hammer) and the metal pieces the tool is made of, in
 * any arrangement, with every piece at its metal's working temperature. A cold piece does not match, so the grid offers
 * nothing and nothing is spent; the hammer wears one point per craft like any hand work. Wrenches and crowbars are made
 * this way (heat spec section 2); tongs have their own recipe with the same rule.
 */
public class HotToolRecipe extends CustomRecipe {

    /** What the tool is made of: {@code count} pieces of the item {@code piece}, hammered to {@code result}. */
    private final ResourceLocation piece;
    private final int count;
    private final ItemStack result;

    public HotToolRecipe(CraftingBookCategory category, ResourceLocation piece, int count, ItemStack result) {
        super(category);
        this.piece = piece;
        this.count = count;
        this.result = result;
    }

    public ResourceLocation piece() {
        return piece;
    }

    public int count() {
        return count;
    }

    public ItemStack result() {
        return result;
    }

    public static final class Serializer implements RecipeSerializer<HotToolRecipe> {
        private static final MapCodec<HotToolRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(CustomRecipe::category),
                ResourceLocation.CODEC.fieldOf("piece").forGetter(HotToolRecipe::piece),
                Codec.intRange(1, 9).fieldOf("count").forGetter(HotToolRecipe::count),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(HotToolRecipe::result)).apply(i, HotToolRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, HotToolRecipe> STREAM = StreamCodec.composite(
                CraftingBookCategory.STREAM_CODEC, CustomRecipe::category,
                ResourceLocation.STREAM_CODEC, HotToolRecipe::piece,
                ByteBufCodecs.VAR_INT, HotToolRecipe::count,
                ItemStack.STREAM_CODEC, HotToolRecipe::result,
                HotToolRecipe::new);

        @Override
        public MapCodec<HotToolRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HotToolRecipe> streamCodec() {
            return STREAM;
        }
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return find(input, null) ? result.copy() : ItemStack.EMPTY;
    }

    /** The tool the grid makes; {@code level} (the clock heat is read against) is null only when assembling, after a match. */
    private boolean find(CraftingInput input, Level level) {
        boolean hammer = false;
        int found = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            if (stack.getItem() instanceof HandToolItem tool && tool.getStrength(stack).value() > 0f) {
                if (hammer)
                    return false;
                hammer = true;
                continue;
            }
            if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(this.piece))
                return false;
            found++;
            if (level != null && !workable(stack, level))
                return false;
        }
        return hammer && found == count;
    }

    /** Whether {@code stack} is at least as hot as its material needs to be worked. */
    private static boolean workable(ItemStack stack, Level level) {
        return Material.find(ClientMaterials.on(level), stack.getItem())
                .map(found -> ClientMaterials.on(level).get(found.id()))
                .map(m -> ItemHeat.get(stack, level).value() >= m.minTemperature().value())
                .orElse(false);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.HOT_TOOL.get();
    }
}
