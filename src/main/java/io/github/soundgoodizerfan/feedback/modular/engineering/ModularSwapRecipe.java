// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import io.github.soundgoodizerfan.feedback.modular.bench.ModularAssembly;
import io.github.soundgoodizerfan.feedback.modular.blueprint.ToolDesigns;
import io.github.soundgoodizerfan.feedback.modular.item.ModularRework;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

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
 * Part swap in the plain crafting table: one finished modular item of a simple type (no blueprint, no school) and replacement parts or
 * stock, positions ignored. The stock goes through {@link ModularRework} (a swap of a filled role, else an add to an empty one), the
 * old part comes back in the grid (a worn wearing part short by its wear) and the item wears a point.
 * Prior art: Silent Gear's {@code GearPartSwapRecipe} (MIT), design only: one shapeless recipe, one item plus parts, the old part
 * returned in the new part's slot. Rejected there: per-type max-per-item slot choice, the destroy-swapped-parts toggle, repair and
 * part-removal kit items, item-id part lookups, a repair-efficiency scalar, wear carried onto the removed head.
 */
public class ModularSwapRecipe extends CustomRecipe {

    public ModularSwapRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static Map<ResourceLocation, ModularType> simple() {
        Map<ResourceLocation, ModularType> out = new TreeMap<>();
        ModularTypes.known().forEach((id, t) -> {
            if (!t.blueprint() && t.school().isEmpty() && t.family() != ModularType.Family.DISH)
                out.put(id, t);
        });
        return out;
    }

    /** The grid read as an item and its stock: slot of the item, slots of the stock. Empty unless exactly one finished modular item sits with at least one other stack. */
    private record Grid(int toolSlot, ItemStack tool, List<Integer> stockSlots, List<ItemStack> stock) {
    }

    private static Optional<Grid> read(CraftingInput input, Map<ResourceLocation, ModularType> types) {
        int toolSlot = -1;
        List<Integer> slots = new ArrayList<>();
        List<ItemStack> stock = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty())
                continue;
            ModularDesign d = ModularStacks.design(s);
            if (d != null && types.containsKey(d.type())) {
                if (toolSlot >= 0)
                    return Optional.empty();
                toolSlot = i;
            } else {
                slots.add(i);
                stock.add(s.copyWithCount(1));
            }
        }
        if (toolSlot < 0 || stock.isEmpty())
            return Optional.empty();
        return Optional.of(new Grid(toolSlot, input.getItem(toolSlot).copyWithCount(1), slots, stock));
    }

    /** The reworked item, a swap first and then an add, with the wear of one point. */
    private static Optional<ItemStack> make(Grid g) {
        var types = simple();
        var materials = MaterialIngredient.materialsNow();
        Optional<ItemStack> out = ModularRework.rework(g.tool(), g.stock(), materials, types, ModularRework.Mode.SWAP);
        if (out.isEmpty())
            out = ModularRework.rework(g.tool(), g.stock(), materials, types, ModularRework.Mode.ADD);
        out.ifPresent(s -> {
            if (s.isDamageableItem() && s.getDamageValue() < s.getMaxDamage() - 2) {
                s.setDamageValue(s.getDamageValue() + 1);
                ModularStacks.refresh(s);
            }
        });
        return out;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input, simple()).flatMap(ModularSwapRecipe::make).isPresent();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return read(input, simple()).flatMap(ModularSwapRecipe::make).orElse(ItemStack.EMPTY);
    }

    /** The part a swap takes off goes back in the grid: in the item's own slot, then the stock's. */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> rest = super.getRemainingItems(input);
        var types = simple();
        Optional<Grid> grid = read(input, types);
        if (grid.isEmpty())
            return rest;
        Grid g = grid.get();
        Optional<ItemStack> made = make(g);
        ModularDesign before = ModularStacks.design(g.tool());
        ModularDesign now = made.map(ModularStacks::design).orElse(null);
        ModularType type = before == null ? null : types.get(before.type());
        if (now == null || type == null)
            return rest;
        var materials = MaterialIngredient.materialsNow();
        List<ItemStack> back = new ArrayList<>();
        for (ModularDesign.Part old : before.parts()) {
            ModularType.Role role = type.roles().get(old.role());
            if (role == null || old.equals(now.part(old.role())))
                continue;
            int count = old.role().equals(type.wearRole()) ? (int) Math.floor(old.count() * ModularRework.kept(g.tool(), 0f)) : old.count();
            back.addAll(ToolDesigns.items(old, role, materials, count));
        }
        List<Integer> room = new ArrayList<>();
        room.add(g.toolSlot());
        room.addAll(g.stockSlots());
        for (int i = 0; i < back.size() && i < room.size(); i++)
            rest.set(room.get(i), back.get(i));
        return rest;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.MODULAR_SWAP.get();
    }
}
