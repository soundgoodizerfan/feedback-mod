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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * What can be made, in one place (spec 4.2). The Tool Table's assemble tab, the Blueprint Table's
 * screen, menu and printing, {@code /feedback build}, the starter design a vanilla item converts
 * to, the JEI tool cards and the load-time consistency check all ask this class, so a type's
 * forms, a role's choices and what counts as makeable cannot be said two ways. It is pure: it
 * takes the type and material tables as arguments and touches no world or screen.
 *
 * <h2>The idea</h2>
 * A design is makeable <b>if the Tool Table would accept the parts it names</b>. {@link #make} does
 * not re-state the table's rules: it turns the design into the stacks a player would put in the six
 * slots ({@link #stacks}), packs them into slots by capacity ({@link #layout}), and asks
 * {@link ToolTableAssembly#infer}. What comes back is the tool, so the blueprint's prediction, the
 * table's output and {@code /feedback build} are one computation.
 *
 * <h2>What a drawing is</h2>
 * A type, one of its {@link ModularType#forms forms} (which roles, how many of each), any of its
 * {@link ModularType#extras extras} switched on, and for each role a material and, for a shaped
 * part, a shape ({@link ToolTableShaping.Option}: the standard, a laid variant, a forged one; a
 * twin's head also picks which kind of head). Heat and flaws are not part of a drawing: a blueprint
 * is paper, and flaws are what forging adds.
 */
public final class ToolDesigns {

    private ToolDesigns() {
    }

    // --- what exists ----------------------------------------------------------------------------

    /** The types a blueprint can be drawn of: every one with a way to be made (a gun too, since 2026-10-21). */
    public static List<ResourceLocation> drawable(Map<ResourceLocation, ModularType> types) {
        return types.entrySet().stream()
                .filter(e -> !e.getValue().patterns().isEmpty())
                .map(Map.Entry::getKey).toList();
    }

    /** The items each role has in form {@code form} of {@code type} with {@code extras} on, in role order. */
    public static Map<String, Integer> counts(ModularType type, int form, Set<String> extras) {
        Map<String, Integer> base = type.forms().get(Math.floorMod(form, type.forms().size()));
        Map<String, Integer> out = new LinkedHashMap<>();
        for (String r : type.order()) {
            if (base.containsKey(r))
                out.put(r, base.get(r));
            else if (extras.contains(r))
                out.put(r, type.roles().get(r).bulk());
        }
        return out;
    }

    /** Whether the role takes a shaped part (one item with its material on it) rather than raw stock. */
    public static boolean shaped(ModularType.Role role) {
        return role.part().isPresent() || role.striker();
    }

    /**
     * Every shape a role's part can be made in, in the order of the Tool Table's own list: a plain
     * shaped role has its part's, a twin's head has every kind of head's. Empty for raw stock.
     */
    public static List<ToolTableShaping.Option> shapes(ModularType.Role role, Map<ResourceLocation, ModularType> types) {
        if (!shaped(role))
            return List.of();
        return ToolTableShaping.options(types).stream().filter(o -> role.striker()
                ? role.forms().contains(o.shaping().item().getPath()) : role.part().get().item().equals(o.shaping().item())).toList();
    }

    /**
     * Every material a role can be drawn in with {@code shape}, each in the first form it comes in
     * that the role takes: the form of a stock item for a shaped part (the part is made from it), of
     * the item itself for raw stock.
     */
    public static List<Material.Located> materials(ModularType.Role role, ToolTableShaping.Option shape, Map<ResourceLocation, Material> materials) {
        List<Material.Located> out = new ArrayList<>();
        materials.forEach((id, m) -> {
            // The smallest piece that fits: ingots before a block, which only fills a shape that is a whole number of blocks.
            Material.Located best = null;
            for (String form : m.items(id).keySet()) {
                Material.Located l = new Material.Located(id, m, form);
                boolean fits = shape != null
                        ? ModularType.fits(shape.shaping().stock(), form) && role.traits().stream().allMatch(m.traits()::contains)
                                && shape.cells() * ModularTuning.CELL_MB % io.github.soundgoodizerfan.feedback.registry.FormItems.volume(form) == 0
                        : role.accepts(l);
                if (fits && (best == null || io.github.soundgoodizerfan.feedback.registry.FormItems.volume(form)
                        < io.github.soundgoodizerfan.feedback.registry.FormItems.volume(best.form())))
                    best = l;
            }
            if (best != null)
                out.add(best);
        });
        return out;
    }

    // --- parts and designs ----------------------------------------------------------------------

    /** The part a role gets from {@code material}: shaped in {@code shape} (flawless), or raw stock {@code count} of it. */
    public static Part part(String role, Material.Located material, ToolTableShaping.Option shape, int count) {
        return shape == null ? new Part(role, material.id(), material.form(), count)
                : new Part(role, material.id(), material.form(), shape.cells(), shape.shaping().item().getPath(), shape.variant().name(), 0);
    }

    /** {@code material} as {@code role} takes it with {@code shape}, if it can be drawn so. */
    public static Optional<Material.Located> located(ModularType.Role role, ToolTableShaping.Option shape, ResourceLocation material,
                                                     Map<ResourceLocation, Material> materials) {
        return materials(role, shape, materials).stream().filter(l -> l.id().equals(material)).findFirst();
    }

    /** The first material in the role's choices whose id is {@code wanted}, else the first, else empty. */
    private static Optional<Material.Located> pick(List<Material.Located> choices, ResourceLocation wanted) {
        return choices.stream().filter(l -> l.id().equals(wanted)).findFirst().or(() -> choices.stream().findFirst());
    }

    /** The standard shape of a role (a twin's head: its first kind, as the role lists them), or null for raw stock. */
    public static ToolTableShaping.Option standard(ModularType.Role role, Map<ResourceLocation, ModularType> types) {
        return shapes(role, types).stream().filter(o -> o.variant().name().isEmpty())
                .min(Comparator.comparingInt(o -> role.striker() && !o.shaping().item().getPath().equals(role.forms().get(0)) ? 1 : 0))
                .orElse(null);
    }

    /**
     * The type drawn as plainly as it can be: form {@code form}, no extras, every role in its
     * {@code defaults} material (else the first that fits when {@code strict} is off), the standard shape.
     */
    private static Optional<ModularDesign> plain(ResourceLocation id, Map<ResourceLocation, ModularType> types, Map<String, Integer> counts,
                                                 boolean defaults, Map<ResourceLocation, Material> materials) {
        ModularType type = types.get(id);
        List<Part> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> r : counts.entrySet()) {
            ModularType.Role role = type.roles().get(r.getKey());
            ToolTableShaping.Option shape = role == null ? null : standard(role, types);
            if (role == null || (shaped(role) && shape == null))
                return Optional.empty();
            List<Material.Located> choices = materials(role, shape, materials);
            ResourceLocation wanted = type.defaults().get(r.getKey());
            Optional<Material.Located> l = defaults ? choices.stream().filter(c -> c.id().equals(wanted)).findFirst() : pick(choices, wanted);
            if (l.isEmpty())
                return Optional.empty();
            parts.add(part(r.getKey(), l.get(), shape, r.getValue()));
        }
        return Optional.of(new ModularDesign(id, parts, Edge.STANDARD));
    }

    /**
     * The design a type's own defaults make: what an item handed out with no design becomes, and
     * what a converted vanilla item starts from. Its first form, no extras.
     */
    public static Optional<ModularDesign> starter(ResourceLocation id, Map<ResourceLocation, ModularType> types, Map<ResourceLocation, Material> materials) {
        ModularType type = types.get(id);
        return type == null || type.patterns().isEmpty() ? Optional.empty() : plain(id, types, counts(type, 0, Set.of()), true, materials);
    }

    // --- items ----------------------------------------------------------------------------------

    /**
     * The items {@code part} comes back as when {@code count} of it is kept: a shaped part is its
     * kind's item carrying what it was made of; raw stock, or a shaped part that is short (the shape
     * went with the metal), is stock of its material and form.
     */
    public static List<ItemStack> items(Part part, ModularType.Role role, Map<ResourceLocation, Material> materials, int count) {
        List<ItemStack> out = new ArrayList<>();
        Material material = materials.get(part.material());
        boolean shaped = !part.kind().isEmpty() || shaped(role);
        if (count <= 0)
            return out;
        if (shaped && count == part.count()) {
            String kind = !part.kind().isEmpty() ? part.kind()
                    : role.striker() ? ModularHeads.kindOf(part, role) : role.part().get().item().getPath();
            BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(io.github.soundgoodizerfan.feedback.Feedback.MOD_ID, kind)).ifPresent(item -> {
                ItemStack stack = new ItemStack(item);
                stack.set(FDataComponents.MADE_OF.get(), new MadeOf(part.material(), part.form(), part.count(), part.variant(), part.flaw(), part.dims()));
                out.add(stack);
            });
        } else if (material != null) {
            material.item(part.material(), part.form()).ifPresent(item -> {
                // a short shaped part is cells (ingots' worth of volume; a block of 9 cells is one item); raw stock is items
                int left = shaped ? Math.max(1, count * ModularTuning.CELL_MB / io.github.soundgoodizerfan.feedback.registry.FormItems.volume(part.form())) : count;
                while (left > 0) {
                    int n = Math.min(left, item.getDefaultMaxStackSize());
                    out.add(new ItemStack(item, n));
                    left -= n;
                }
            });
        }
        return out;
    }

    /** The stacks a player would put on the table to make {@code design}: each part as it would be held. */
    public static List<ItemStack> stacks(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        List<ItemStack> out = new ArrayList<>();
        for (Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            if (role != null)
                out.addAll(items(p, role, materials, p.count()));
        }
        return out;
    }

    /** {@code stacks} in the six slots, largest first, or empty if they do not fit. */
    public static Optional<List<ItemStack>> layout(List<ItemStack> stacks) {
        return layout(stacks, ToolTableAssembly.SLOTS);
    }

    /** {@code stacks} in the {@code n} slots of a table (the Gun Table's sixteen), largest first, or empty if they do not fit. */
    public static Optional<List<ItemStack>> layout(List<ItemStack> stacks, int n) {
        if (stacks.size() > n || stacks.stream().anyMatch(s -> s.getCount() > ToolTableAssembly.CAPACITY))
            return Optional.empty();
        ItemStack[] slots = new ItemStack[n];
        List<ItemStack> sorted = new ArrayList<>(stacks);
        sorted.sort(Comparator.comparingInt((ItemStack s) -> s.getCount()).reversed());
        for (int i = 0; i < sorted.size(); i++)
            slots[i] = sorted.get(i);
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : slots)
            out.add(s == null ? ItemStack.EMPTY : s);
        return Optional.of(out);
    }

    // --- makeable -------------------------------------------------------------------------------

    /**
     * What the Tool Table makes from {@code design}'s parts, if it makes {@code design}'s type from
     * them: the design is put on the table and read back. A type that needs a blueprint is given one
     * of its own, since the plan is the gate and the parts are the item.
     */
    public static Optional<ToolTableAssembly.Match> make(ModularDesign design, Map<ResourceLocation, ModularType> types,
                                                         Map<ResourceLocation, Material> materials) {
        ModularType type = types.get(design.type());
        if (type == null)
            return Optional.empty();
        ItemStack plan = ItemStack.EMPTY;
        if (type.blueprint()) {
            plan = new ItemStack(FModular.BLUEPRINT.get());
            plan.set(FModular.DESIGN.get(), design);
        }
        ItemStack drawn = plan;
        return layout(stacks(design, type, materials), ToolTableAssembly.slots(type))
                .flatMap(slots -> ToolTableAssembly.infer(slots, drawn, materials,
                        ToolTableAssembly.ofKind(types, type.family() == ModularType.Family.GUN)))
                .filter(m -> m.type().equals(design.type()));
    }

    /** The item {@code design} makes at the Tool Table, with the edge it was drawn with: what {@code /feedback build} hands out. */
    public static Optional<ItemStack> build(ModularDesign design, Map<ResourceLocation, ModularType> types, Map<ResourceLocation, Material> materials) {
        return make(design, types, materials).flatMap(m -> {
            ModularDesign made = ModularStacks.design(m.tool());
            ModularType type = types.get(made.type());
            Edge edge = type.edged() ? design.edge() : Edge.STANDARD;
            return edge == made.edge() || ModularStacks.assemble(m.tool(), made.withEdge(edge), materials, types)
                    ? Optional.of(m.tool()) : Optional.empty();
        });
    }

    /** What {@code design} bakes to, as drawn and new: the figures of the item the table would make. The blueprint's prediction. */
    public static Optional<ModularFigures> predict(ModularDesign design, Map<ResourceLocation, ModularType> types, Map<ResourceLocation, Material> materials) {
        return build(design, types, materials).map(ModularStacks::figures);
    }

    // --- reading a design back onto the sheet ---------------------------------------------------

    /** Which form of its type a design is and which extras it has on. */
    public record Opened(int form, Set<String> extras) {}

    /** The form and extras {@code design} is, or empty if its roles are none of the type's. */
    public static Optional<Opened> open(ModularDesign design, ModularType type) {
        Map<String, Integer> drawn = new LinkedHashMap<>();
        for (Part p : design.parts())
            drawn.put(p.role(), type.roles().get(p.role()) != null && shaped(type.roles().get(p.role())) ? 1 : p.count());
        List<String> extras = type.extras();
        Set<String> on = new LinkedHashSet<>();
        drawn.keySet().stream().filter(extras::contains).forEach(on::add);
        List<Map<String, Integer>> forms = type.forms();
        for (int i = 0; i < forms.size(); i++)
            if (counts(type, i, on).equals(drawn))
                return Optional.of(new Opened(i, on));
        return Optional.empty();
    }

    // --- the check ------------------------------------------------------------------------------

    /**
     * Problems between what can be drawn and what the table makes: the starter design of every type,
     * and for every form and combination of extras a plain drawing in the first material that fits
     * each role, must be read back by the Tool Table as that same type. A fault here is a type whose
     * data the table and the blueprint would disagree about.
     */
    public static List<String> consistency(Map<ResourceLocation, ModularType> types, Map<ResourceLocation, Material> materials) {
        List<String> out = new ArrayList<>();
        for (ResourceLocation id : drawable(types)) {
            ModularType type = types.get(id);
            // A type whose defaults do not cover its first form just has no starter (it is not handed out bare).
            starter(id, types, materials).filter(d -> make(d, types, materials).isEmpty()).ifPresent(d ->
                    out.add(id + "'s starter design is not read back as " + id + " by the Tool Table: " + d.parts()));
            // A type with many optional parts (a gun's fittings) has thousands of assemblies: its plain drawings are
            // checked with at most one optional part on, which still reaches every role.
            for (ModularType.Assembly a : type.assembliesUpTo(type.extras().size() > 4 ? 1 : type.extras().size())) {
                Optional<ModularDesign> d = plain(id, types, a.counts(), false, materials);
                // A drawing the bake itself refuses (an action the feed does not suit, a winding that is no coil) is
                // not a part set the table could be asked to read back.
                if (d.isPresent() && ModularBake.bake(d.get(), type, materials, 0).isEmpty())
                    continue;
                if (d.isEmpty())
                    out.add(id + " " + a.counts() + " cannot be drawn: some role has no material or no shape");
                else if (make(d.get(), types, materials).isEmpty())
                    out.add(id + " " + a.counts() + " is drawn but the Tool Table does not read it back as " + id + ": " + d.get().parts());
            }
        }
        return out;
    }
}
