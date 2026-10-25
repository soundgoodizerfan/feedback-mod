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
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

/**
 * The bootstrap exception's six recipes (modular items spec section 7): wooden and stone pickaxe,
 * axe and shovel, laid out as vanilla lays them, but the result is the modular tool of the
 * canonical design. Left as plain vanilla results they were rolled into a family shape the moment
 * they reached a pocket, so a crafted shovel could come out a scoop and an axe a hatchet.
 */
public class BootstrapToolRecipe extends CustomRecipe {

    private static final Ingredient STICK = Ingredient.of(Items.STICK);
    private static final List<ShapedRecipe> RECIPES = new ArrayList<>();

    static {
        add(Ingredient.of(ItemTags.PLANKS), Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_SHOVEL);
        add(Ingredient.of(ItemTags.STONE_TOOL_MATERIALS), Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_SHOVEL);
    }

    /** Four leather in a square: a plain leather glove (the gauntlet type with a leather plate), the first one before any Tool Table. */
    private static final ShapedRecipe GLOVE = shaped(Map.of('L', Ingredient.of(net.neoforged.neoforge.common.Tags.Items.LEATHERS)),
            Items.LEATHER, "LL", "LL");

    private static void add(Ingredient head, ItemLike pickaxe, ItemLike axe, ItemLike shovel) {
        Map<Character, Ingredient> key = Map.of('X', head, '#', STICK);
        RECIPES.add(shaped(key, pickaxe, "XXX", " # ", " # "));
        RECIPES.add(shaped(key, axe, "XX", "X#", " #"));
        RECIPES.add(shaped(key, shovel, "X", "#", "#"));
    }

    private static ShapedRecipe shaped(Map<Character, Ingredient> key, ItemLike result, String... rows) {
        return new ShapedRecipe("", CraftingBookCategory.EQUIPMENT, ShapedRecipePattern.of(key, rows), new ItemStack(result));
    }

    public BootstrapToolRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return GLOVE.matches(input, level) || RECIPES.stream().anyMatch(r -> r.matches(input, level));
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        if (GLOVE.matches(input, null))
            return glove();
        for (ShapedRecipe r : RECIPES)
            if (r.matches(input, null))
                return ModularConvert.convert(r.assemble(input, registries), 0L, false);
        return ItemStack.EMPTY;
    }

    private static ItemStack glove() {
        ResourceLocation id = FModular.GAUNTLET.getId();
        Map<ResourceLocation, ModularType> types = ModularTypes.get().types();
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        ModularType type = types.get(id);
        ResourceLocation leather = ResourceLocation.fromNamespaceAndPath("feedback", "leather");
        ModularType.Role role = type.roles().get(type.primary());
        ToolTableShaping.Option shape = ToolDesigns.standard(role, types);
        Optional<Material.Located> located = ToolDesigns.located(role, shape, leather, materials);
        if (located.isEmpty())
            return ItemStack.EMPTY;
        ItemStack out = new ItemStack(FModular.GAUNTLET.get());
        ModularDesign design = new ModularDesign(id, List.of(ToolDesigns.part(type.primary(), located.get(), shape, 1)), Edge.STANDARD);
        if (!ModularStacks.assemble(out, design, materials, types))
            return ItemStack.EMPTY;
        ModularStacks.refresh(out);
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.BOOTSTRAP_TOOL.get();
    }
}
