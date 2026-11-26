// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import io.github.soundgoodizerfan.feedback.modular.part.ModularPartItem;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularFaces;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * {@link ModularConvert} turns a vanilla item into ours the moment a player holds it, so another mod's
 * recipe that asks for the vanilla one (a bowl, an iron pickaxe) would never see it. Here the question
 * is asked both ways: an ingredient accepts a stack that is what a vanilla item it accepts becomes.
 * Used by {@code IngredientMixin}, so it reaches every mod's recipes, JEI and the recipe book alike.
 *
 * <p>What "is what it becomes" means: a shaped part of the same item, material and stock; a modular
 * item of the same type whose primary part is the same kind in the same material (an iron-headed
 * pickaxe on any handle is an iron pickaxe). Read off {@code ModularConvert.convert} itself, so a
 * new vanilla conversion is covered with no edit here.
 */
public final class ConvertedIngredients {

    private ConvertedIngredients() {
    }

    private record Index(Map<String, List<Item>> sources, Map<Item, ItemStack> products) {}

    private static final Index EMPTY = new Index(Map.of(), Map.of());
    private static Index index = EMPTY;
    private static int built = -1;
    private static boolean building;

    /** What a vanilla item becomes is the same wherever it is asked; rebuilt when the material table is. */
    private static synchronized Index index() {
        if (building)
            return EMPTY;
        // A client without a world has no server table: key on its synced copy instead (as ModularConvert does).
        int generation = MaterialTable.generation() > 0 ? MaterialTable.generation()
                : -2 - System.identityHashCode(io.github.soundgoodizerfan.feedback.process.ClientMaterials.get());
        if (generation == built)
            return index;
        if (MaterialIngredient.materialsNow().isEmpty() || ModularTypes.known().isEmpty())
            return EMPTY;
        building = true;
        try {
            Map<String, List<Item>> sources = new HashMap<>();
            Map<Item, ItemStack> products = new IdentityHashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                // Any mod's item the material table names (a thaumium pickaxe), never our own.
                if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("feedback"))
                    continue;
                ItemStack vanilla = new ItemStack(item);
                if (!ModularConvert.convertible(vanilla))
                    continue;
                ItemStack made = ModularConvert.convert(vanilla, 0L, false);
                String key = key(made);
                if (key == null || made.getItem() == item)
                    continue;
                sources.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
                products.put(item, made);
            }
            index = new Index(sources, products);
            built = generation;
            return index;
        } finally {
            building = false;
        }
    }

    /** What a converted stack is, loosely: null for anything that is not a conversion product. */
    private static String key(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        if (made != null && stack.getItem() instanceof ModularPartItem)
            return "part|" + BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + made.material() + "|" + made.stock();
        ModularDesign design = ModularStacks.design(stack);
        if (design == null)
            return null;
        String primary = ModularFaces.PLAN.equals(design.type()) ? "H" : null;
        if (primary == null) {
            ModularType type = ModularTypes.known(design.type());
            primary = type == null ? null : type.primary();
        }
        if (primary == null)
            return null;
        for (ModularDesign.Part p : design.parts())
            if (p.role().equals(primary))
                return "tool|" + design.type() + "|" + p.kind() + "|" + p.material();
        return null;
    }

    /**
     * Whether {@code stack} takes the place of an item {@code wanted} accepts: it is our block's item for a replaced vanilla block
     * ({@link StandIns}), or what a vanilla item turns into. Cheap for a stack that is neither.
     */
    public static boolean accepts(Predicate<ItemStack> wanted, ItemStack stack) {
        if (stack.isEmpty())
            return false;
        // by-item-ok: a replaced vanilla block is one id on each side, and other mods ask for theirs by id
        for (Map.Entry<Item, Item> e : StandIns.items().entrySet())
            if (stack.is(e.getValue()) && wanted.test(new ItemStack(e.getKey())))
                return true;
        if (!stack.has(FDataComponents.MADE_OF.get()) && ModularStacks.design(stack) == null)
            return false;
        String key = key(stack);
        if (key == null)
            return false;
        List<Item> from = index().sources().get(key);
        if (from == null)
            return false;
        for (Item item : from)
            if (wanted.test(new ItemStack(item)))
                return true;
        return false;
    }

    /** What to show a player where {@code asked} is wanted: the item they can hold in its place, or {@code asked} itself. */
    public static ItemStack shown(ItemStack asked) {
        Item ours = StandIns.items().get(asked.getItem());
        if (ours != null)
            return new ItemStack(ours, asked.getCount());
        ItemStack made = index().products().get(asked.getItem());
        return made == null ? asked : made.copyWithCount(asked.getCount());
    }

    /** {@code shown} with the conversion of each vanilla item in it added, so a list of what an ingredient takes (JEI, the recipe book) names the item a player holds. */
    public static ItemStack[] widen(ItemStack[] shown) {
        Map<Item, ItemStack> products = index().products();
        // by-item-ok: as in accepts, one replaced vanilla id to our one id
        Map<Item, Item> standIns = StandIns.items();
        if (products.isEmpty() && standIns.isEmpty())
            return shown;
        List<ItemStack> extra = null;
        for (ItemStack s : shown) {
            Item ours = standIns.get(s.getItem());
            ItemStack made = ours != null ? new ItemStack(ours) : products.get(s.getItem());
            if (made == null)
                continue;
            if (extra == null)
                extra = new ArrayList<>();
            extra.add(made.copy());
        }
        if (extra == null)
            return shown;
        ItemStack[] out = new ItemStack[shown.length + extra.size()];
        System.arraycopy(shown, 0, out, 0, shown.length);
        for (int i = 0; i < extra.size(); i++)
            out[shown.length + i] = extra.get(i);
        return out;
    }
}
