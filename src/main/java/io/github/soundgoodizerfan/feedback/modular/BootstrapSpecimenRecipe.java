// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * A crafting grid that hands out one specific prebaked specimen of a treatise-gated modular type, made of one cheap material, like the
 * bootstrap tools. A type of a {@link School} is drawn only with its volume of the treatise, and a treatise is written by taking a specimen
 * of the type apart; this is the first specimen, so no gate can lock a player out of what it guards. The data file names the type and
 * the material and lays the grid out like a shaped recipe.
 */
public class BootstrapSpecimenRecipe extends ShapedRecipe {

    private final ResourceLocation type;
    private final ResourceLocation material;
    private ItemStack cached = ItemStack.EMPTY;

    public BootstrapSpecimenRecipe(CraftingBookCategory category, ShapedRecipePattern pattern, ResourceLocation type, ResourceLocation material) {
        super("", category, pattern, new ItemStack(BuiltInRegistries.ITEM.get(type)));
        this.type = type;
        this.material = material;
    }

    public ResourceLocation type() {
        return type;
    }

    public ResourceLocation material() {
        return material;
    }

    /** The specimen: the type's item carrying the design, or the bare item while the tables are not loaded. */
    public ItemStack specimen() {
        if (!cached.isEmpty() && cached.has(io.github.soundgoodizerfan.feedback.registry.FModular.DESIGN.get()))
            return cached.copy();
        Map<ResourceLocation, ModularType> types = ModularTypes.known();
        Map<ResourceLocation, Material> materials = MaterialIngredient.materialsNow();
        ItemStack out = new ItemStack(BuiltInRegistries.ITEM.get(type));
        if (types.containsKey(type) && ToolDesigns.specimen(type, material, types, materials)
                .filter(d -> ModularStacks.assemble(out, d, materials, types)).isPresent()) {
            ModularStacks.refresh(out);
            cached = out;
            return out.copy();
        }
        return out;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return specimen();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return specimen();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.BOOTSTRAP_SPECIMEN.get();
    }

    public static class Serializer implements RecipeSerializer<BootstrapSpecimenRecipe> {

        private static final MapCodec<BootstrapSpecimenRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.EQUIPMENT).forGetter(BootstrapSpecimenRecipe::category),
                ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
                ResourceLocation.CODEC.fieldOf("modular_type").forGetter(BootstrapSpecimenRecipe::type),
                ResourceLocation.CODEC.fieldOf("material").forGetter(BootstrapSpecimenRecipe::material)
        ).apply(i, BootstrapSpecimenRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BootstrapSpecimenRecipe> STREAM = StreamCodec.composite(
                CraftingBookCategory.STREAM_CODEC, BootstrapSpecimenRecipe::category,
                ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
                ResourceLocation.STREAM_CODEC, BootstrapSpecimenRecipe::type,
                ResourceLocation.STREAM_CODEC, BootstrapSpecimenRecipe::material,
                BootstrapSpecimenRecipe::new);

        @Override
        public MapCodec<BootstrapSpecimenRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BootstrapSpecimenRecipe> streamCodec() {
            return STREAM;
        }
    }
}
