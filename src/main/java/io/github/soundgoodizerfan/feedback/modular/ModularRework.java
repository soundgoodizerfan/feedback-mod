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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Working on a tool that is already made, at the Tool Table's tool slot. It moved here from the
 * crafting recipe, with its rules unchanged:
 *
 * <ul>
 *   <li><b>Repair.</b> The stuff the wearing part is made of: each piece puts back its share of the
 *       durability (spec 2.3, 11).</li>
 *   <li><b>Swap a part.</b> A different material in as many cells as the part has (or one shaped
 *       part) replaces it; a swapped wearing part is a fresh one and sheds its plating.</li>
 *   <li><b>Add.</b> A tip, a binding or a gun's fitting is put on a finished tool, one piece of what the role takes.
 *       Armour studs are counted, so they go in with the plate when it is made.</li>
 *   <li><b>Take apart.</b> The parts come out: shaped parts as {@code MadeOf} stacks, raw stock as
 *       stacks. A worn wearing part comes back short by what the wear spent, so taking a worn tool
 *       apart and putting it together again is not a free repair.</li>
 * </ul>
 *
 * Tinkers' repair keyed on tool id and material id, which is identity, so repair here is by
 * property: the item is made of this stuff.
 */
public final class ModularRework {

    private ModularRework() {
    }

    /** What {@code stock} does to {@code tool}: the changed item, or empty if nothing it does. */
    public static Optional<ItemStack> rework(ItemStack tool, List<ItemStack> stock, Map<ResourceLocation, Material> materials,
                                             Map<ResourceLocation, ModularType> types) {
        ModularDesign design = ModularStacks.design(tool);
        ModularType type = design == null ? null : types.get(design.type());
        if (type == null || stock.isEmpty())
            return Optional.empty();
        Material.Located what = Material.find(materials, stock.get(0)).orElse(null);
        if (what == null)
            return Optional.empty();
        for (ItemStack o : stock) {
            Material.Located l = Material.find(materials, o).orElse(null);
            if (l == null || !l.id().equals(what.id()) || !l.form().equals(what.form()))
                return Optional.empty();
        }
        ItemStack out = tool.copy();
        out.setCount(1);
        Part wearing = design.part(type.wearRole());
        // Repair: the stuff the wearing part is made of puts its share of the durability back.
        if (wearing != null && wearing.material().equals(what.id()) && type.roles().get(type.wearRole()).acceptsStock(what)) {
            if (tool.getDamageValue() == 0)
                return Optional.empty();
            int share = tool.getMaxDamage() / Math.max(1, wearing.count());
            float cells = 0;
            for (ItemStack o : stock)
                cells += o.getCount() * (float) io.github.soundgoodizerfan.feedback.registry.FormItems.volume(what.form()) / ModularTuning.CELL_MB;
            out.setDamageValue(Math.max(0, tool.getDamageValue() - Math.round(share * cells)));
            ModularStacks.refresh(out);
            return Optional.of(out);
        }
        // Swap: a different material, in as many cells as the part has (or one shaped part), replaces that part.
        for (Map.Entry<String, ModularType.Role> r : type.roles().entrySet()) {
            Part part = design.part(r.getKey());
            boolean shaped = r.getValue().part().isPresent() || r.getValue().striker();
            int cells = shaped ? 1 : part == null ? -1 : part.count();
            String kind = shaped ? BuiltInRegistries.ITEM.getKey(stock.get(0).getItem()).getPath() : "";
            MadeOf madeOf = stock.get(0).get(FDataComponents.MADE_OF.get());
            String variant = madeOf == null ? "" : madeOf.variant();
            if (part == null || cells != stock.size() || (part.material().equals(what.id()) && part.kind().equals(kind) && part.variant().equals(variant))
                    || !r.getValue().accepts(what))
                continue;
            if (r.getKey().equals(type.wearRole())) {
                out.setDamageValue(0);
                out.remove(FDataComponents.PLATING.get());
            }
            return ModularStacks.assemble(out, design.withPart(ToolTableAssembly.part(r.getKey(), what, stock.get(0), part.count())), materials, types)
                    ? Optional.of(out) : Optional.empty();
        }
        // Add: an optional part (a tip, a binding, a gun's fitting or sight), as many pieces as its role asks, on a tool made
        // without one. A part the type's patterns do not lay is optional; a design the bake refuses (a second muzzle device
        // on a gun) is refused here too.
        if (type.family() != ModularType.Family.ARMOUR) {
            for (String key : type.extras()) {
                ModularType.Role role = type.roles().get(key);
                boolean shaped = role.part().isPresent() || role.striker();
                if (design.part(key) != null || stock.size() != (shaped ? 1 : role.bulk()) || !role.accepts(what))
                    continue;
                if (shaped != stock.get(0).has(FDataComponents.MADE_OF.get()))
                    continue;
                List<Part> parts = new ArrayList<>(design.parts());
                parts.add(ToolTableAssembly.part(key, what, stock.get(0), shaped ? 1 : role.bulk()));
                parts.sort(Comparator.comparingInt(p -> type.order().indexOf(p.role())));
                return ModularStacks.assemble(out, new ModularDesign(design.type(), parts, design.edge()), materials, types)
                        ? Optional.of(out) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * The parts {@code tool} is made of, as items. A shaped part is its kind's item carrying what it
     * was made of; raw stock is the item of its material and form. The wearing part, if the tool is
     * worn, comes back with the share the wear spent taken off, rounded down (a shaped part that is
     * short comes back as raw stock: the shape went with the metal).
     */
    public static List<ItemStack> dismantle(ItemStack tool, Map<ResourceLocation, Material> materials,
                                            Map<ResourceLocation, ModularType> types) {
        ModularDesign design = ModularStacks.design(tool);
        ModularType type = design == null ? null : types.get(design.type());
        List<ItemStack> out = new ArrayList<>();
        if (type == null)
            return out;
        float kept = 1f - ModularStacks.worn(tool);
        for (Part part : design.parts()) {
            ModularType.Role role = type.roles().get(part.role());
            if (role == null)
                continue;
            int count = part.role().equals(type.wearRole()) ? (int) Math.floor(part.count() * kept) : part.count();
            if (count <= 0)
                continue;
            out.addAll(ToolDesigns.items(part, role, materials, count));
        }
        return out;
    }
}
