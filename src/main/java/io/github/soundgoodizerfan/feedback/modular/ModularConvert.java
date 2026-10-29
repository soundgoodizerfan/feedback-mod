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

import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Vanilla tools, weapons and armour go the way of the furnace (spec 7): their recipes are off,
 * and whatever of them still turns up -- a chest, a mob's hand, a villager's offer, a pocket -- is
 * made into the modular item it would have been. The canonical design of a vanilla item is read off
 * the material table, which already names it: iron's {@code pickaxe} form is
 * {@code minecraft:iron_pickaxe}, so that item is an iron head on a type's default handle. Nothing
 * here knows an item id; the table does, and a pack that adds a material with a {@code sword} form
 * converts its own sword with no edit.
 *
 * <p>The converted item keeps everything the vanilla one carried -- enchantments, name, trim, how
 * worn -- and is the same stack, so a player who never opens a bench never sees a difference but
 * the tooltip.
 */
public final class ModularConvert {

    private ModularConvert() {
    }

    private static int cachedGeneration = -1;
    private static final Map<Item, Optional<Material.Located>> LOCATED = new HashMap<>();

    private static Optional<Material.Located> locate(Item item) {
        int generation = MaterialTable.generation();
        if (generation != cachedGeneration) {
            LOCATED.clear();
            cachedGeneration = generation;
        }
        return LOCATED.computeIfAbsent(item, i -> Material.find(MaterialTable.get().materials(), i));
    }

    /** Whether the stack is a vanilla item that has a modular type to become. Cheap enough to ask every second. */
    public static boolean convertible(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof Modular || ModularStacks.design(stack) != null)
            return false;
        Optional<Material.Located> found = locate(stack.getItem());
        return found.isPresent() && (typeFor(found.get()) != null || partItem(stack.getItem(), found.get()) != null);
    }

    /**
     * The shaped part a vanilla item already is, if the material table names its form after one: wood's {@code ceramic_bowl}
     * form is {@code minecraft:bowl}, so that bowl is a wooden bowl part. Only vanilla ids, so a part is never converted twice.
     */
    private static Item partItem(Item item, Material.Located located) {
        if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft"))
            return null;
        Item part = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("feedback", located.form()));
        return part instanceof ModularPartItem ? part : null;
    }

    /** The cells of stock a part is shaped from, read off the first role that makes it: what its volume and size scale with. */
    private static int cells(Item part) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(part);
        for (ModularType t : ModularTypes.get().types().values())
            for (ModularType.Role r : t.roles().values())
                if (r.part().isPresent() && r.part().get().item().equals(id))
                    return Math.max(1, (int) r.part().get().shape().stream().flatMapToInt(String::chars).filter(c -> c != ' ').count());
        return 1;
    }

    private static Map.Entry<ResourceLocation, ModularType> typeFor(Material.Located located) {
        for (Map.Entry<ResourceLocation, ModularType> e : ModularTypes.get().types().entrySet())
            if (e.getValue().vanilla().containsKey(located.form()) && BuiltInRegistries.ITEM.containsKey(e.getKey()))
                return e;
        return null;
    }

    public static ItemStack convert(ItemStack vanilla) {
        return convert(vanilla, 0L);
    }

    /**
     * The modular item {@code vanilla} becomes, or the stack itself if it is not one of those. The
     * head is drawn from the family of shapes its canonical one belongs to (a pickaxe head may come
     * out heavy, a prospector's, a miner's): the same stack and {@code salt} always roll the same,
     * so a source passes what tells its stacks apart (a mob, a slot) and a plain stack is otherwise alike.
     */
    public static ItemStack convert(ItemStack vanilla, long salt) {
        return convert(vanilla, salt, true);
    }

    /** {@code roll} false keeps the canonical shape (a crafted bootstrap tool is the standard one, not a draw). */
    public static ItemStack convert(ItemStack vanilla, long salt, boolean roll) {
        if (!convertible(vanilla))
            return vanilla;
        Material.Located located = locate(vanilla.getItem()).get();
        Item partItem = partItem(vanilla.getItem(), located);
        if (partItem != null) {
            ItemStack part = new ItemStack(partItem, vanilla.getCount());
            part.applyComponents(vanilla.getComponentsPatch());
            part.set(FDataComponents.MADE_OF.get(), new MadeOf(located.id(), located.form(), cells(partItem), ""));
            return part;
        }
        Map.Entry<ResourceLocation, ModularType> hit = typeFor(located);
        ModularType type = hit.getValue();
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        Map<ResourceLocation, ModularType> types = ModularTypes.get().types();
        List<Part> parts = new ArrayList<>();
        net.minecraft.util.RandomSource dice = net.minecraft.util.RandomSource.create(
                31L * (31L * BuiltInRegistries.ITEM.getKey(vanilla.getItem()).hashCode() + vanilla.getComponentsPatch().hashCode()) + salt);
        // A crystal cannot be forged into a head or a plate (diamond is not a metal): what vanilla
        // made of it is the body of the type's default metal with the crystal set in it.
        boolean set = located.material().kind().filter(k -> k == Material.Kind.CRYSTAL).isPresent();
        java.util.Set<String> extras = new java.util.LinkedHashSet<>();
        if (set)
            type.extras().stream().filter(k -> type.roles().get(k).set()).forEach(extras::add);
        Map<String, Integer> counts = new java.util.LinkedHashMap<>(ToolDesigns.counts(type, 0, extras));
        if (set && type.family() == ModularType.Family.ARMOUR)
            type.order().stream().filter(k -> type.roles().get(k).set()).forEach(k -> counts.put(k, ModularTuning.CONVERT_STUDS));
        for (Map.Entry<String, Integer> c : counts.entrySet()) {
            String role = c.getKey();
            ModularType.Role r = type.roles().get(role);
            ResourceLocation material = r.set() ? located.id()
                    : role.equals(type.primary()) ? (set ? ModularTuning.CRYSTAL_BODY : located.id()) : type.defaults().get(role);
            // A variant named for the vanilla form (a trident, a mace) is the shape that form was; else the standard.
            ToolTableShaping.Option shape = ToolDesigns.shapes(r, types).stream()
                    .filter(o -> o.variant().name().equals(located.form())).findFirst().orElse(ToolDesigns.standard(r, types));
            Material m = material == null ? null : materials.get(material);
            if (m == null)
                return vanilla;
            ToolTableShaping.Option base = shape;
            if (roll && shape != null && role.equals(type.primary())) {
                List<ToolTableShaping.Option> family = ToolDesigns.shapes(r, types).stream()
                        .filter(o -> o.shaping().item().equals(base.shaping().item())).toList();
                if (!family.isEmpty())
                    shape = family.get(dice.nextInt(family.size()));
            }
            // The primary part is the form vanilla made it in (an ingot, a length of chain); the rest are standard stock.
            Optional<Material.Located> l = role.equals(type.primary()) && !r.set() && !set
                    ? Optional.of(new Material.Located(material, m, type.vanilla().get(located.form())))
                    : ToolDesigns.located(r, shape, material, materials);
            if (l.isEmpty() && shape != base) {
                shape = base;
                l = ToolDesigns.located(r, shape, material, materials);
            }
            if (l.isEmpty())
                return vanilla;
            parts.add(ToolDesigns.part(role, l.get(), shape, c.getValue()));
        }
        ItemStack out = new ItemStack(BuiltInRegistries.ITEM.get(hit.getKey()), vanilla.getCount());
        out.applyComponents(vanilla.getComponentsPatch());
        out.remove(net.minecraft.core.component.DataComponents.MAX_DAMAGE);
        float worn = vanilla.isDamageableItem() ? vanilla.getDamageValue() / (float) vanilla.getMaxDamage() : 0f;
        out.remove(net.minecraft.core.component.DataComponents.DAMAGE);
        if (!ModularStacks.assemble(out, new ModularDesign(hit.getKey(), parts, Edge.STANDARD), materials, ModularTypes.get().types()))
            return vanilla;
        out.setDamageValue(Math.min(out.getMaxDamage() - 1, Math.round(worn * out.getMaxDamage())));
        ModularStacks.refresh(out);
        return out;
    }
}
