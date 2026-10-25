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
import java.util.LinkedHashMap;
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
 * Putting a tool together at the Tool Table. Six slots, always six, and the player never says what
 * is being made: <b>the parts imply the type</b>.
 *
 * <h2>The inference</h2>
 * For each type, every non-empty slot is assigned a distinct role that accepts its stack
 * ({@link ModularType.Role#accepts}; a shaped role wants a shaped part, one item) such that the
 * role-to-count map is one the type is made from ({@link ModularType#assemblies}). A small
 * backtracking search over at most six slots and six roles. Which slot a stack sits in never
 * matters: when several assignments work (the twin's two heads), the one whose parts sort lowest
 * wins, so the same parts always make the same tool.
 *
 * <h2>One set of parts, one tool</h2>
 * {@link #audit} is run when the types load: two types that accept the same part set would make
 * the answer depend on data order, so the load logs an error naming both. The patterns' letters
 * are never consulted; what is unambiguous is the roles and counts.
 *
 * <p>Tinkers' tool station is the same shape (parts in, a tool out, the type read off what is in);
 * its slot count is fixed by the type, which is the part not taken: the type here follows the parts.
 */
public final class ToolTableAssembly {

    private ToolTableAssembly() {
    }

    /** Slots for parts. All alike: any role may sit in any, and the screen labels each by the role its part fills. */
    public static final int SLOTS = 6;
    /**
     * Largest stack any slot holds. An arbitrary ceiling, not a physical limit (a shaped part is one
     * item whatever the slot and a role's count is checked by {@link #fits}); it is sized to the
     * biggest count a role asks for, and {@link #audit} reports any role that asks for more.
     */
    public static final int CAPACITY = 8;
    /** The Gun Table's part slots: a 4 by 4 grid, the most a gun has (fifteen roles) with one to spare. */
    public static final int GUN_SLOTS = 16;

    /** Slots the table that makes {@code type} has: guns are made at the Gun Table, which has more. */
    public static int slots(ModularType type) {
        return type.family() == ModularType.Family.GUN ? GUN_SLOTS : SLOTS;
    }

    /** Which table makes a type: guns at the Gun Table, dishes at the Prep Table, everything else at the Tool Table. */
    public enum Kind { TOOL, GUN, DISH }

    public static Kind kindOf(ModularType type) {
        return switch (type.family()) {
            case GUN -> Kind.GUN;
            case DISH -> Kind.DISH;
            default -> Kind.TOOL;
        };
    }

    /** The table {@code stack} is reworked at: the one that makes its type. */
    public static Kind kindOf(ItemStack stack) {
        ModularDesign d = ModularStacks.design(stack);
        ModularType type = d == null ? null : ModularTypes.known().get(d.type());
        return type == null ? Kind.TOOL : kindOf(type);
    }

    /** The types {@code kind} says: one table's filter, which is the only thing the tables differ by in what they make. */
    public static Map<ResourceLocation, ModularType> ofKind(Map<ResourceLocation, ModularType> types, Kind kind) {
        Map<ResourceLocation, ModularType> out = new java.util.TreeMap<>();
        types.forEach((id, t) -> {
            if (kindOf(t) == kind)
                out.put(id, t);
        });
        return out;
    }

    /**
     * What the six slots make.
     *
     * @param type   the type id, which is also the item.
     * @param role   for each slot, the role letter it filled, or null for an empty one.
     * @param tool   the baked item.
     */
    public record Match(ResourceLocation type, String[] role, ItemStack tool) {}

    /**
     * The tool {@code slots} make, if they make one. {@code plan} is the slot's blueprint (empty
     * for none), which a type that says {@code blueprint} needs one of.
     */
    public static Optional<Match> infer(List<ItemStack> slots, ItemStack plan, Map<ResourceLocation, Material> materials,
                                        Map<ResourceLocation, ModularType> types) {
        List<Integer> used = new ArrayList<>();
        List<Material.Located> what = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).isEmpty())
                continue;
            Optional<Material.Located> l = Material.find(materials, slots.get(i));
            if (l.isEmpty())
                return Optional.empty();
            used.add(i);
            what.add(l.get());
        }
        if (used.isEmpty())
            return Optional.empty();
        ModularDesign drawn = plan.isEmpty() ? null : ModularStacks.design(plan);
        for (Map.Entry<ResourceLocation, ModularType> e : types.entrySet()) {
            ModularType type = e.getValue();
            if (type.blueprint() && (drawn == null || !drawn.type().equals(e.getKey())))
                continue;
            Best best = new Best();
            for (ModularType.Assembly a : type.assemblies(used.size()))
                assign(type, e.getKey(), a.counts(), slots, used, what, 0, new String[used.size()], new boolean[used.size()], best);
            if (best.design == null)
                continue;
            ModularDesign design = best.design;
            Optional<ItemStack> tool = ModularStacks.blank(e.getKey()).filter(stack ->
                    ModularStacks.assemble(stack, design, materials, types));
            if (tool.isEmpty())
                continue;
            String[] role = new String[slots.size()];
            for (int i = 0; i < used.size(); i++)
                role[used.get(i)] = best.roles[i];
            return Optional.of(new Match(e.getKey(), role, tool.get()));
        }
        return Optional.empty();
    }

    /** The lowest-sorting assignment found so far. */
    private static final class Best {
        String key;
        ModularDesign design;
        String[] roles;
    }

    private static void assign(ModularType type, ResourceLocation id, Map<String, Integer> counts, List<ItemStack> slots,
                               List<Integer> used, List<Material.Located> what, int i, String[] chosen, boolean[] taken, Best best) {
        List<String> letters = new ArrayList<>(counts.keySet());
        if (i == used.size()) {
            List<Part> parts = new ArrayList<>();
            StringBuilder key = new StringBuilder();
            for (String role : type.order()) {
                int s = java.util.Arrays.asList(chosen).indexOf(role);
                if (s < 0)
                    continue;
                Part p = part(role, what.get(s), slots.get(used.get(s)), slots.get(used.get(s)).getCount());
                parts.add(p);
                key.append(p).append(';');
            }
            if (best.key == null || key.toString().compareTo(best.key) < 0) {
                best.key = key.toString();
                best.design = new ModularDesign(id, parts, Edge.STANDARD);
                best.roles = chosen.clone();
            }
            return;
        }
        ItemStack stack = slots.get(used.get(i));
        for (int r = 0; r < letters.size(); r++) {
            ModularType.Role role = type.roles().get(letters.get(r));
            if (taken[r] || role == null || !fits(role, what.get(i), stack, counts.get(letters.get(r))))
                continue;
            taken[r] = true;
            chosen[i] = letters.get(r);
            assign(type, id, counts, slots, used, what, i + 1, chosen, taken, best);
            taken[r] = false;
            chosen[i] = null;
        }
    }

    /**
     * Whether {@code stack} is what {@code role} wants, in the number it wants: a shaped part is one
     * item, stock is as many as its size. A shaped part is never "any stock": a role that is not
     * shaped takes one only if it names that part (a crossbow's lock takes a trigger group), so a
     * lining that takes anything fibrous cannot swallow a wool plate meant for another piece.
     */
    private static boolean fits(ModularType.Role role, Material.Located what, ItemStack stack, int wanted) {
        if (!role.accepts(what))
            return false;
        boolean shaped = role.part().isPresent() || role.striker();
        boolean part = stack.has(FDataComponents.MADE_OF.get());
        if ((shaped && !part) || (!shaped && part && !role.forms().contains(what.form())))
            return false;
        return stack.getCount() == wanted;
    }

    /** The part a role gets from {@code stack}: a shaped part by what it was shaped from, raw stock as it lies. */
    public static Part part(String role, Material.Located what, ItemStack stack, int count) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        // A pot serving is food already baked: its figures come off the stack, not a material block.
        if (made == null && what.material().traits().contains(Material.Trait.BROTH))
            return new Part(role, what.id(), what.form(), count, "", "", 0, DishBake.servingDims(stack));
        if (made == null && what.material().food().isPresent())
            return new Part(role, what.id(), what.form(), count, "", "", 0, foodDims(stack, what));
        if (made != null && !(stack.getItem() instanceof ModularPartItem)) // raw stock the enchanting table stamped
            return new Part(role, what.id(), what.form(), count, "", made.variant(), made.flaw(), made.dims());
        return made != null ? new Part(role, made.material(), made.stock(), made.cells(),
                BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(), made.variant(), made.flaw(), made.dims())
                : new Part(role, what.id(), what.form(), count);
    }

    /** A food's state plus what the Prep Table did to it: its cut level and evenness grade. */
    private static java.util.Map<String, Float> foodDims(ItemStack stack, Material.Located what) {
        java.util.Map<String, Float> dims = DishBake.dimsOf(foodState(stack, what));
        io.github.soundgoodizerfan.feedback.process.FoodCut cut = stack.get(FDataComponents.FOOD_CUT.get());
        if (cut != null) {
            dims.put("cut", (float) cut.level());
            dims.put("evenness", (float) cut.grade());
        }
        return dims;
    }

    /** A food stack's state as it is stored (infer has no level to settle it by), else its material's own: the dish bakes from it. */
    private static io.github.soundgoodizerfan.feedback.process.FoodState foodState(ItemStack stack, Material.Located what) {
        io.github.soundgoodizerfan.feedback.process.FoodState stored = stack.get(FDataComponents.FOOD_STATE.get());
        return stored != null ? stored : DishBake.fresh(what.material().food().get(), what.form());
    }

    // --- what the slots could still become ------------------------------------------------------

    /**
     * The types the non-empty slots could be on the way to: every stack takes a distinct role of the
     * type, whatever the counts. The screen names the roles of these so the player can see what is
     * missing.
     */
    public static List<ResourceLocation> candidates(List<ItemStack> slots, ItemStack plan, Map<ResourceLocation, Material> materials,
                                                    Map<ResourceLocation, ModularType> types) {
        List<Material.Located> what = new ArrayList<>();
        for (ItemStack s : slots)
            if (!s.isEmpty()) {
                Optional<Material.Located> l = Material.find(materials, s);
                if (l.isEmpty())
                    return List.of();
                what.add(l.get());
            }
        if (what.isEmpty())
            return List.of();
        List<ResourceLocation> out = new ArrayList<>();
        ModularDesign drawn = plan.isEmpty() ? null : ModularStacks.design(plan);
        for (Map.Entry<ResourceLocation, ModularType> e : types.entrySet()) {
            ModularType type = e.getValue();
            if (type.patterns().isEmpty() || (type.blueprint() && (drawn == null || !drawn.type().equals(e.getKey()))))
                continue;
            if (reaches(new ArrayList<>(type.roles().values().stream().filter(r -> !r.after()).toList()), what, 0))
                out.add(e.getKey());
        }
        return out;
    }

    private static boolean reaches(List<ModularType.Role> roles, List<Material.Located> what, int i) {
        if (i == what.size())
            return true;
        for (int r = 0; r < roles.size(); r++) {
            ModularType.Role role = roles.remove(r);
            if (role.accepts(what.get(i)) && reaches(roles, what, i + 1))
                return true;
            roles.add(r, role);
        }
        return false;
    }

    // --- the load-time check --------------------------------------------------------------------

    /**
     * Problems in a set of types: two that accept the same part set, any that needs more than
     * six slots, and a type with two roles that take an overlapping item (the slot an item sits in
     * must never decide what it is). Roles are compared by what they accept, counts exactly, since a pattern is only how
     * many of each role there are. A wildcard narrowed by required traits (a lining) is trusted not to meet an explicit form (studs); otherwise it errs towards reporting.
     */
    public static List<String> audit(Map<ResourceLocation, ModularType> types) {
        List<String> out = new ArrayList<>();
        List<Map.Entry<ResourceLocation, ModularType>> list = new ArrayList<>(types.entrySet());
        // The fullest assembly of a form is the form with every optional part on, so the limits are checked on that, not on each combination.
        for (Map.Entry<ResourceLocation, ModularType> e : list) {
            ModularType t = e.getValue();
            int extras = t.extras().size();
            for (Map<String, Integer> form : t.forms()) {
                if (form.size() + extras > slots(t))
                    out.add(e.getKey() + " has up to " + (form.size() + extras) + " roles in " + form + " and every optional part, more than the " + slots(t) + " slots of its table");
                if (form.values().stream().anyMatch(n -> n > CAPACITY))
                    out.add(e.getKey() + " asks for more than " + CAPACITY + " of one role in " + form + ", more than a Tool Table slot holds");
            }
            for (String x : t.extras())
                if (t.roles().get(x).bulk() > CAPACITY)
                    out.add(e.getKey() + " asks for more than " + CAPACITY + " of optional role " + x + ", more than a Tool Table slot holds");
        }
        for (Map.Entry<ResourceLocation, ModularType> e : list) {
            List<String> keys = new ArrayList<>(e.getValue().roles().keySet());
            for (int i = 0; i < keys.size(); i++)
                for (int j = i + 1; j < keys.size(); j++) {
                    ModularType.Role a = e.getValue().roles().get(keys.get(i)), b = e.getValue().roles().get(keys.get(j));
                    // A twin-headed tool's two heads are the same kind of role by design; which is which is settled by sorting.
                    // A second barrel is the first's pair by design (Role#alt), settled by sorting the same way.
                    boolean pair = a.alt().equals(keys.get(j)) || b.alt().equals(keys.get(i));
                    // A dish's food roles each require a food trait and a material has one, so they never meet.
                    boolean apart = e.getValue().family() == ModularType.Family.DISH && !a.traits().isEmpty() && !b.traits().isEmpty()
                            && java.util.Collections.disjoint(a.traits(), b.traits());
                    if (!(a.striker() && b.striker()) && !pair && !apart && overlap(a, b))
                        out.add(e.getKey() + " roles " + a.name() + " and " + b.name() + " accept overlapping items: "
                                + a.forms() + " against " + b.forms() + "; make one a shaped part with an item of its own");
                }
        }
        for (int i = 0; i < list.size(); i++)
            for (int j = i + 1; j < list.size(); j++) {
                ModularType a = list.get(i).getValue();
                ModularType b = list.get(j).getValue();
                // A gun is made at the Gun Table and a dish at the Prep Table, and never meet a tool, so they cannot be confused.
                if (a.blueprint() != b.blueprint() || kindOf(a) != kindOf(b))
                    continue;
                ResourceLocation ia = list.get(i).getKey(), ib = list.get(j).getKey();
                clash(a, b).ifPresent(maps -> out.add(ia + " and " + ib + " accept the same parts (" + maps
                        + "): the Tool Table could not tell which to make"));
            }
        return out;
    }

    /**
     * {@link #audit(Map)}, and the check that what the Blueprint Table can draw is what the table
     * makes ({@link ToolDesigns#consistency}). Needs the materials, so it is run once both tables have loaded.
     */
    public static List<String> audit(Map<ResourceLocation, ModularType> types, Map<ResourceLocation, Material> materials) {
        List<String> out = audit(types);
        out.addAll(ToolDesigns.consistency(types, materials));
        return out;
    }

    /** The first pair of assemblies, one of each type, that the same parts could fill. */
    private static Optional<String> clash(ModularType a, ModularType b) {
        for (int size = 1; size <= slots(a); size++) {
            List<ModularType.Assembly> ys = b.assemblies(size);
            for (ModularType.Assembly x : a.assemblies(size))
                for (ModularType.Assembly y : ys)
                    if (confusable(a, x.counts(), b, y.counts()))
                        return Optional.of(x.counts() + " against " + y.counts());
        }
        return Optional.empty();
    }

    /** Whether some pairing of the two maps' roles matches counts and has overlapping accepts everywhere. */
    private static boolean confusable(ModularType a, Map<String, Integer> x, ModularType b, Map<String, Integer> y) {
        return pair(a, new ArrayList<>(x.entrySet()), b, new LinkedHashMap<>(y), 0);
    }

    private static boolean pair(ModularType a, List<Map.Entry<String, Integer>> mine, ModularType b, Map<String, Integer> theirs, int i) {
        if (i == mine.size())
            return true;
        ModularType.Role left = a.roles().get(mine.get(i).getKey());
        for (Map.Entry<String, Integer> t : new ArrayList<>(theirs.entrySet())) {
            ModularType.Role right = b.roles().get(t.getKey());
            // Two dishes' food roles that need different traits (a starchy base, a broth base) never take one item.
            boolean apart = a.family() == ModularType.Family.DISH && b.family() == ModularType.Family.DISH && left != null && right != null
                    && !left.traits().isEmpty() && !right.traits().isEmpty() && java.util.Collections.disjoint(left.traits(), right.traits());
            if (left == null || right == null || apart || !t.getValue().equals(mine.get(i).getValue()) || !overlap(left, right))
                continue;
            theirs.remove(t.getKey());
            boolean ok = pair(a, mine, b, theirs, i + 1);
            theirs.put(t.getKey(), t.getValue());
            if (ok)
                return true;
        }
        return false;
    }

    private static boolean overlap(ModularType.Role a, ModularType.Role b) {
        boolean sa = a.part().isPresent() || a.striker(), sb = b.part().isPresent() || b.striker();
        for (String f : a.forms())
            for (String g : b.forms())
                if (!(sb && f.startsWith("*")) && !(sa && g.startsWith("*"))
                        && !(f.startsWith("*") && !a.traits().isEmpty() && !g.startsWith("*"))
                        && !(g.startsWith("*") && !b.traits().isEmpty() && !f.startsWith("*")) && formsMeet(f, g))
                    return true;
        return false;
    }

    private static boolean formsMeet(String a, String b) {
        boolean wa = a.startsWith("*"), wb = b.startsWith("*");
        if (!wa && !wb)
            return a.equals(b);
        if (wa && !wb)
            return b.endsWith(a.substring(1));
        if (wb && !wa)
            return a.endsWith(b.substring(1));
        return a.endsWith(b.substring(1)) || b.endsWith(a.substring(1));
    }
}
