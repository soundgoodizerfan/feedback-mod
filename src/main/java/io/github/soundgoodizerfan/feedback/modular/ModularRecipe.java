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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * The one crafting recipe for every modular item. Three things, told apart by what is in the grid:
 *
 * <ul>
 *   <li><b>Assembly.</b> Parts laid out in one of a {@link ModularType}'s patterns, each part
 *       found by what its item is made of (never by id). A head, blade or plate is one shaped
 *       part ({@link ModularPartRecipe}) that carries its material and size; a handle, binding or
 *       grip is raw stock, its cells its size. The parts are the design.</li>
 *   <li><b>Repair.</b> A modular item and the stuff its wearing part is made of: each item
 *       puts back its share of the durability. By hand, at the bench (spec 11).</li>
 *   <li><b>Re-forge.</b> A modular item and a different material in the number of cells one of its
 *       parts has: the part is swapped, and a swapped wearing part is a fresh one.</li>
 * </ul>
 *
 * Tinkers' tool station is the same shape (parts in, a tool out); its repair keyed on tool id and
 * material id, which is identity, so repair here is by property: the item is made of this stuff.
 */
public class ModularRecipe extends CustomRecipe {

    public ModularRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !build(input, ClientMaterials.on(level), ModularTypes.on(level)).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return build(input, MaterialTable.get().materials(), ModularTypes.get().types());
    }

    private static ItemStack build(CraftingInput input, Map<ResourceLocation, Material> materials,
                                   Map<ResourceLocation, ModularType> types) {
        if (materials.isEmpty() || types.isEmpty())
            return ItemStack.EMPTY;
        Map<Item, Optional<Material.Located>> memo = new HashMap<>();
        Function<ItemStack, Optional<Material.Located>> locate = s -> s.has(FDataComponents.MADE_OF.get())
                ? Material.find(materials, s) : memo.computeIfAbsent(s.getItem(), i -> Material.find(materials, i));
        ItemStack tool = null;
        List<ItemStack> others = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty())
                continue;
            if (ModularStacks.design(s) != null) {
                if (tool != null)
                    return ItemStack.EMPTY;
                tool = s;
            } else
                others.add(s);
        }
        if (tool != null)
            return rework(tool, others, materials, types, locate);
        for (Map.Entry<ResourceLocation, ModularType> e : types.entrySet()) {
            Optional<Map<String, ModularType.Found>> hit = e.getValue().match(input, locate);
            if (hit.isEmpty())
                continue;
            List<Part> parts = new ArrayList<>();
            for (String role : e.getValue().order()) {
                ModularType.Found found = hit.get().get(role);
                if (found != null)
                    parts.add(part(role, found.what(), found.stack(), found.count()));
            }
            return ModularStacks.blank(e.getKey()).filter(stack ->
                    ModularStacks.assemble(stack, new ModularDesign(e.getKey(), parts, Edge.STANDARD), materials, types))
                    .orElse(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack rework(ItemStack tool, List<ItemStack> others, Map<ResourceLocation, Material> materials,
                                    Map<ResourceLocation, ModularType> types, Function<ItemStack, Optional<Material.Located>> locate) {
        ModularDesign design = ModularStacks.design(tool);
        ModularType type = types.get(design.type());
        if (type == null || others.isEmpty())
            return ItemStack.EMPTY;
        Material.Located what = locate.apply(others.get(0)).orElse(null);
        if (what == null)
            return ItemStack.EMPTY;
        for (ItemStack o : others) {
            Material.Located l = locate.apply(o).orElse(null);
            if (l == null || !l.id().equals(what.id()) || !l.form().equals(what.form()))
                return ItemStack.EMPTY;
        }
        ItemStack out = tool.copy();
        out.setCount(1);
        Part wearing = design.part(type.wearRole());
        // Repair: the stuff the wearing part is made of puts its share of the durability back.
        if (wearing != null && wearing.material().equals(what.id()) && type.roles().get(type.wearRole()).acceptsStock(what)) {
            if (tool.getDamageValue() == 0)
                return ItemStack.EMPTY;
            int share = tool.getMaxDamage() / Math.max(1, wearing.count());
            out.setDamageValue(Math.max(0, tool.getDamageValue() - share * others.size()));
            ModularStacks.refresh(out);
            return out;
        }
        // Re-forge: a different material, in as many cells as the part has (or one shaped part),
        // replaces that part.
        for (Map.Entry<String, ModularType.Role> r : type.roles().entrySet()) {
            Part part = design.part(r.getKey());
            int cells = r.getValue().part().isPresent() ? 1 : part == null ? -1 : part.count();
            if (part == null || cells != others.size() || part.material().equals(what.id()) || !r.getValue().accepts(what))
                continue;
            if (r.getKey().equals(type.wearRole()))
                out.setDamageValue(0);
            return ModularStacks.assemble(out, design.withPart(part(r.getKey(), what, others.get(0), part.count())), materials, types)
                    ? out : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    /** The part a role gets from {@code stack}: a shaped part by what it was shaped from, raw stock as it lies. */
    private static Part part(String role, Material.Located what, ItemStack stack, int count) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        return made != null ? new Part(role, made.material(), made.stock(), made.cells())
                : new Part(role, what.id(), what.form(), count);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FModular.RECIPE.get();
    }
}
