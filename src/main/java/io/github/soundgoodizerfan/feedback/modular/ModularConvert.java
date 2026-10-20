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
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

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
        return locate(stack.getItem()).isPresent() && typeFor(locate(stack.getItem()).get()) != null;
    }

    private static Map.Entry<ResourceLocation, ModularType> typeFor(Material.Located located) {
        for (Map.Entry<ResourceLocation, ModularType> e : ModularTypes.get().types().entrySet())
            if (e.getValue().vanilla().containsKey(located.form()) && BuiltInRegistries.ITEM.containsKey(e.getKey()))
                return e;
        return null;
    }

    /** The modular item {@code vanilla} becomes, or the stack itself if it is not one of those. */
    public static ItemStack convert(ItemStack vanilla) {
        if (!convertible(vanilla))
            return vanilla;
        Material.Located located = locate(vanilla.getItem()).get();
        Map.Entry<ResourceLocation, ModularType> hit = typeFor(located);
        ModularType type = hit.getValue();
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        List<Part> parts = new ArrayList<>();
        Map<String, Integer> counts = type.canonicalCounts();
        for (String role : type.order()) {
            Integer count = counts.get(role);
            if (count == null)
                continue;
            ResourceLocation material = role.equals(type.primary()) ? located.id() : type.defaults().get(role);
            if (material == null || !materials.containsKey(material))
                return vanilla;
            String form = role.equals(type.primary()) ? type.vanilla().get(located.form()) : type.roles().get(role).stockForms().get(0);
            parts.add(new Part(role, material, form, count));
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
