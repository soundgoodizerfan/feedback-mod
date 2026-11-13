// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;
import io.github.soundgoodizerfan.feedback.modular.bench.Edge;
import io.github.soundgoodizerfan.feedback.modular.bench.PartBenchShaping;
import io.github.soundgoodizerfan.feedback.modular.blueprint.ToolDesigns;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;

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
 * canonical design, each with a leather binding. Left as plain vanilla results they were rolled into a family shape the moment
 * they reached a pocket, so a crafted shovel could come out a scoop and an axe a hatchet.
 */
public class BootstrapToolRecipe extends CustomRecipe {

    private static final Ingredient STICK = Ingredient.of(Items.STICK);
    /** Every modular tool needs a binding or a guard; these are the binding, leather as the converted tool carries it. */
    private static final Ingredient BINDING = Ingredient.of(net.neoforged.neoforge.common.Tags.Items.LEATHERS);
    private static final List<ShapedRecipe> RECIPES = new ArrayList<>();

    static {
        add(Ingredient.of(ItemTags.PLANKS), Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_SHOVEL);
        add(Ingredient.of(ItemTags.STONE_TOOL_MATERIALS), Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_SHOVEL);
    }

    /** Five leather as fingers over a palm (belt owns the 2x2 square): a plain leather glove (the gauntlet type with a leather plate), the first one before any Tool Table. */
    private static final ShapedRecipe GLOVE = shaped(Map.of('L', Ingredient.of(net.neoforged.neoforge.common.Tags.Items.LEATHERS)),
            Items.LEATHER, "L L", "LLL");

    /** A cobblestone head on two sticks: the first hand hammer, a hafted tool whose one face is a hammer's. */
    private static final ShapedRecipe HAMMER = shaped(Map.of('S', Ingredient.of(Items.COBBLESTONE), 'T', STICK), Items.COBBLESTONE,
            "SSS", "STS", " T ");

    private static ItemStack hammer() {
        Map<ResourceLocation, ModularType> types = ModularTypes.known();
        Map<ResourceLocation, Material> materials = io.github.soundgoodizerfan.feedback.process.MaterialIngredient.materialsNow();
        return ToolDesigns.starter(io.github.soundgoodizerfan.feedback.Feedback.id("hand_hammer"), types, materials)
                .flatMap(d -> ModularStacks.blank(d.type()).filter(s -> ModularStacks.assemble(s, d, materials, types)))
                .orElse(ItemStack.EMPTY);
    }

    private static void add(Ingredient head, ItemLike pickaxe, ItemLike axe, ItemLike shovel) {
        Map<Character, Ingredient> key = Map.of('X', head, '#', STICK, 'B', BINDING);
        RECIPES.add(shaped(key, pickaxe, "XXX", " #B", " # "));
        RECIPES.add(shaped(key, axe, "XX", "X#", "B#"));
        RECIPES.add(shaped(key, shovel, "X ", "#B", "# "));
    }

    private static ShapedRecipe shaped(Map<Character, Ingredient> key, ItemLike result, String... rows) {
        return new ShapedRecipe("", CraftingBookCategory.EQUIPMENT, ShapedRecipePattern.of(key, rows), new ItemStack(result));
    }

    public BootstrapToolRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return GLOVE.matches(input, level) || HAMMER.matches(input, level) || RECIPES.stream().anyMatch(r -> r.matches(input, level));
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        if (GLOVE.matches(input, null))
            return glove();
        if (HAMMER.matches(input, null))
            return hammer();
        for (ShapedRecipe r : RECIPES)
            if (r.matches(input, null))
                return ModularConvert.convert(r.assemble(input, registries), 0L, false);
        return ItemStack.EMPTY;
    }

    /** The six tool layouts, for the recipe browser. */
    public static List<ShapedRecipe> tools() {
        return RECIPES;
    }

    /** The modular tool {@code layout} crafts. */
    public static ItemStack tool(ShapedRecipe layout) {
        return ModularConvert.convert(layout.getResultItem(null), 0L, false);
    }

    public static ItemStack glove() {
        ResourceLocation id = FModular.GLOVE.getId();
        Map<ResourceLocation, ModularType> types = ModularTypes.known();
        Map<ResourceLocation, Material> materials = io.github.soundgoodizerfan.feedback.process.MaterialIngredient.materialsNow();
        ModularType type = types.get(id);
        if (type == null)
            return ItemStack.EMPTY;
        ResourceLocation leather = ResourceLocation.fromNamespaceAndPath("feedback", "leather");
        ModularType.Role role = type.roles().get(type.primary());
        PartBenchShaping.Option shape = ToolDesigns.standard(role, types);
        Optional<Material.Located> located = ToolDesigns.located(role, shape, leather, materials);
        if (located.isEmpty())
            return ItemStack.EMPTY;
        ItemStack out = new ItemStack(FModular.GLOVE.get());
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
