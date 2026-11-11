// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Working on a tool that is already made, at the Engineering Table's Repair and Fit tabs ({@code EngineeringRework}):
 *
 * <ul>
 *   <li><b>Repair.</b> The stuff the wearing part is made of: each piece puts back its share of the
 *       durability (spec 2.3, 11).</li>
 *   <li><b>Swap a part.</b> A different material in as many cells as the part has (or one shaped
 *       part) replaces it; a swapped wearing part is a fresh one and sheds its plating.</li>
 *   <li><b>Add.</b> A tip, a binding or a gun's fitting is put on a finished tool, one piece of what the role takes.
 *       Armour takes its lining and studs (a pair) the same way, on the plate it was shaped as.</li>
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

    /** Which of the three things {@link #rework} does is allowed: {@code ALL} allows each; the Engineering Table splits them across tabs. */
    public enum Mode {
        ALL(true, true, true), MEND(true, false, false), SWAP(false, true, false), ADD(false, false, true);

        final boolean mend, swap, add;

        Mode(boolean mend, boolean swap, boolean add) {
            this.mend = mend;
            this.swap = swap;
            this.add = add;
        }
    }

    /** What {@code stock} does to {@code tool}: the changed item, or empty if nothing it does. */
    public static Optional<ItemStack> rework(ItemStack tool, List<ItemStack> stock, Map<ResourceLocation, Material> materials,
                                             Map<ResourceLocation, ModularType> types) {
        return rework(tool, stock, materials, types, Mode.ALL);
    }

    /** As {@link #rework(ItemStack, List, Map, Map)}, doing only what {@code mode} allows (mend, swap and add never overlap). */
    public static Optional<ItemStack> rework(ItemStack tool, List<ItemStack> stock, Map<ResourceLocation, Material> materials,
                                             Map<ResourceLocation, ModularType> types, Mode mode) {
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
        if (mode.mend && wearing != null && wearing.material().equals(what.id()) && type.roles().get(type.wearRole()).acceptsStock(what)) {
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
        // A held modular item is never a swap (its slot is taken out and refilled whole); it only goes into an empty role, below.
        for (Map.Entry<String, ModularType.Role> r : type.roles().entrySet()) {
            if (!mode.swap || ModularNest.holdable(stock.get(0)))
                break;
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
                // The new wearing stock brings its own coat (roll cladding), or none.
                if (stock.get(0).has(FDataComponents.PLATING.get()))
                    out.set(FDataComponents.PLATING.get(), stock.get(0).get(FDataComponents.PLATING.get()));
                else
                    out.remove(FDataComponents.PLATING.get());
            }
            return ModularStacks.assemble(out, design.withPart(ModularAssembly.part(r.getKey(), what, stock.get(0), part.count())), materials, types)
                    ? Optional.of(out) : Optional.empty();
        }
        // Add: an optional part (a tip, a binding, a gun's fitting or sight, an armour piece's lining or studs), as many pieces as
        // its role asks, on a tool made without one. A part the type's patterns do not lay is optional, and so is everything on armour
        // but its plate (the plate is the piece); a design the bake refuses (a second muzzle device on a gun) is refused here too.
        // A finished module is held whole by a role that lists its nest form (never laid at assembly, so marked after).
        if (!mode.add)
            return Optional.empty();
        List<String> optional = ModularNest.holdable(stock.get(0)) ? type.order()
                : type.family() == ModularType.Family.ARMOUR
                ? type.placed().stream().filter(k -> !k.equals(type.primary())).toList() : type.extras();
        for (String key : optional) {
            ModularType.Role role = type.roles().get(key);
            boolean shaped = role.part().isPresent() || role.striker();
            if (design.part(key) != null || stock.size() != (shaped ? 1 : role.bulk()) || !role.accepts(what))
                continue;
            if (shaped != (stock.get(0).getItem() instanceof ModularPartItem))
                continue;
            List<Part> parts = new ArrayList<>(design.parts());
            parts.add(ModularAssembly.part(key, what, stock.get(0), shaped ? 1 : role.bulk()));
            parts.sort(Comparator.comparingInt(p -> type.order().indexOf(p.role())));
            // An addon sits on a mount some part of the item offers (a suppressor on a muzzle, a sight on a rail), else there is nowhere to fit it.
            if (!type.mounted(key, parts))
                return Optional.empty();
            return ModularStacks.assemble(out, new ModularDesign(design.type(), parts, design.edge()), materials, types)
                    ? Optional.of(out) : Optional.empty();
        }
        return Optional.empty();
    }

    /** The outcome of {@link #addAll}: the fitted item and the pieces each group used, or why it was refused ({@code failed} is the group that did not fit). */
    public record Added(Optional<ItemStack> out, int[] used, int failed) {
        public boolean ok() {
            return out.isPresent();
        }
    }

    /**
     * Fits several attachments at once. Each group is the stack of one attachment (its pieces, in order); the groups go on one after the
     * other, each onto the item the last made, so every addon is checked against the others fitted (a clashing extra, a second muzzle
     * device, is refused by the bake exactly as one at a time). A group takes the fewest pieces of its stack that fit (a shaped part one,
     * stock as many as its role's bulk). All or nothing: when one group does not fit, nothing is fitted and {@code failed} names it.
     */
    public static Added addAll(ItemStack tool, List<List<ItemStack>> groups, Map<ResourceLocation, Material> materials,
                               Map<ResourceLocation, ModularType> types) {
        ItemStack now = tool;
        int[] used = new int[groups.size()];
        for (int g = 0; g < groups.size(); g++) {
            List<ItemStack> pieces = groups.get(g);
            Optional<ItemStack> next = Optional.empty();
            for (int n = 1; n <= pieces.size() && next.isEmpty(); n++) {
                next = rework(now, pieces.subList(0, n), materials, types, Mode.ADD);
                used[g] = n;
            }
            if (next.isEmpty())
                return new Added(Optional.empty(), used, g);
            now = next.get();
        }
        return new Added(groups.isEmpty() ? Optional.empty() : Optional.of(now), used, groups.isEmpty() ? 0 : -1);
    }

    /**
     * The parts {@code tool} is made of, as items. A shaped part is its kind's item carrying what it
     * was made of; raw stock is the item of its material and form. The wearing part, if the tool is
     * worn, comes back with the share the wear spent taken off, rounded down (a shaped part that is
     * short comes back as raw stock: the shape went with the metal).
     */
    public static List<ItemStack> dismantle(ItemStack tool, Map<ResourceLocation, Material> materials,
                                            Map<ResourceLocation, ModularType> types) {
        return dismantle(tool, materials, types, 0f);
    }

    /** The share of the wearing part a worn {@code tool} gives back; {@code relief} (negative) takes that fraction off the wear it holds. */
    public static float kept(ItemStack tool, float relief) {
        return Math.min(1f, 1f - ModularStacks.worn(tool) * Math.max(0f, 1f + relief));
    }

    /** As {@link #dismantle(ItemStack, Map, Map)}, the wear held being lessened by {@code relief} (the Salvage skill). */
    public static List<ItemStack> dismantle(ItemStack tool, Map<ResourceLocation, Material> materials,
                                            Map<ResourceLocation, ModularType> types, float relief) {
        ModularDesign design = ModularStacks.design(tool);
        ModularType type = design == null ? null : types.get(design.type());
        List<ItemStack> out = new ArrayList<>();
        if (type == null)
            return out;
        float kept = kept(tool, relief);
        for (Part part : design.parts()) {
            ModularType.Role role = type.roles().get(part.role());
            if (role == null)
                continue;
            if (!part.sub().isEmpty()) {
                out.add(part.sub().copy());
                continue;
            }
            int count = part.role().equals(type.wearRole()) ? (int) Math.floor(part.count() * kept) : part.count();
            if (count <= 0)
                continue;
            out.addAll(ToolDesigns.items(part, role, materials, count));
        }
        return out;
    }
}
