// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Measures;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Putting a tool together at the Engineering Table (and in the plain crafting table, which runs the same inference). The player never
 * says what is being made: <b>the parts imply the type</b>. The part benches no longer assemble; the Prep Table still puts a dish
 * together from its own {@link #DISH_SLOTS}.
 *
 * <h2>The inference</h2>
 * For each type, every non-empty slot is assigned a distinct role that accepts its stack
 * ({@link ModularType.Role#accepts}; a shaped role wants a shaped part, one item) such that the
 * role-to-count map is one the type is made from ({@link ModularType#assemblies}). A small
 * backtracking search over at most nine slots and nine roles. Which slot a stack sits in never
 * matters: when several assignments work (the twin's two heads), the one whose parts sort lowest
 * wins, so the same parts always make the same tool.
 *
 * <h2>One set of parts, one tool</h2>
 * {@link #audit} is run when the types load: two types that accept the same part set would make
 * the answer depend on data order, so the load logs an error naming both. The patterns' letters
 * are never consulted; what is unambiguous is the roles and counts.
 *
 * <p>Tinkers' tool station is the same shape (parts in, a tool out, the type read off what is in);
 * its slot count is fixed by the type, which is the part not taken: the type here follows the parts. Complex types ({@code blueprint: true})
 * assemble only at the Engineering Table; {@link #audit} checks their laid roles fit its {@link #GRID_SLOTS}.
 */
public final class ToolTableAssembly {

    private ToolTableAssembly() {
    }

    /** The Prep Table's slots for a dish's foods. All alike: any role may sit in any. */
    public static final int DISH_SLOTS = 6;
    /** Slots the Engineering Table's crafting grid feeds {@link #infer}: the 3x3, the most any type's laid roles may need. {@code infer} takes any count up to this. */
    public static final int GRID_SLOTS = 9;
    /**
     * Largest stack any slot holds. An arbitrary ceiling, not a physical limit (a shaped part is one
     * item whatever the slot and a role's count is checked by {@link #fits}); it is sized to the
     * biggest count a role asks for, and {@link #audit} reports any role that asks for more.
     */
    public static final int CAPACITY = 8;
    /** Which table makes a type: dishes at the Prep Table, everything else (guns included) at the Engineering Table, its parts at the part benches. */
    public enum Kind { TOOL, DISH }

    public static Kind kindOf(ModularType type) {
        return type.family() == ModularType.Family.DISH ? Kind.DISH : Kind.TOOL;
    }

    /** The table {@code stack} is made at: the one that makes its type. */
    public static Kind kindOf(ItemStack stack) {
        ModularDesign d = ModularStacks.design(stack);
        ModularType type = d == null ? null : ModularTypes.known().get(d.type());
        return type == null ? Kind.TOOL : kindOf(type);
    }

    /** The types {@code kind} says: one table's filter. */
    public static Map<ResourceLocation, ModularType> ofKind(Map<ResourceLocation, ModularType> types, Kind kind) {
        Map<ResourceLocation, ModularType> out = new java.util.TreeMap<>();
        types.forEach((id, t) -> {
            if (kindOf(t) == kind)
                out.put(id, t);
        });
        return out;
    }

    /**
     * What the slots make.
     *
     * @param type   the type id, which is also the item.
     * @param role   for each slot, the role letter it filled, or null for an empty one.
     * @param tool   the baked item.
     */
    public record Match(ResourceLocation type, String[] role, ItemStack tool) {}

    /**
     * The tool {@code slots} make, if they make one. Any number of slots up to the 3x3's nine: stacks of the same raw stock (one
     * material, one form) merge into the role's count, so slot position and splitting never matter. {@code plan} is the blueprint
     * slot (empty for none): a blueprint is never required, it is the yardstick the fit is judged against
     * ({@link ModularFit#judge}) and gives credit. Every type is allowed and no start-condition bonus is given; see the overload below.
     */
    public static Optional<Match> infer(List<ItemStack> slots, ItemStack plan, Map<ResourceLocation, Material> materials,
                                        Map<ResourceLocation, ModularType> types) {
        return infer(slots, plan, materials, types, t -> true, null);
    }

    /**
     * As above, for a bench that gates and rewards the build.
     *
     * @param may   which types the maker may assemble: the school gate ({@link Treatises#assembles} over the masks the bench finds);
     *              a type it refuses is skipped as if it did not exist.
     * @param start what the maker brought beyond the parts (fastener levels, minigame score), or null for none and for a prediction:
     *              the item then carries its fit but starts with a full bar. The fit is written onto the tool ({@code FModular.FIT}).
     */
    public static Optional<Match> infer(List<ItemStack> slots, ItemStack plan, Map<ResourceLocation, Material> materials,
                                        Map<ResourceLocation, ModularType> types, java.util.function.Predicate<ModularType> may,
                                        ModularFit.Start start) {
        // Merge raw stock of one material and form into one entry, remembering which slots fed it.
        List<ItemStack> merged = new ArrayList<>();
        List<List<Integer>> origin = new ArrayList<>();
        List<Material.Located> what = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            ItemStack s = slots.get(i);
            if (s.isEmpty())
                continue;
            Optional<Material.Located> l = Material.find(materials, s);
            if (l.isEmpty())
                return Optional.empty();
            int into = mergeable(s, l.get()) ? indexOfStock(merged, what, l.get()) : -1;
            if (into >= 0) {
                merged.set(into, merged.get(into).copyWithCount(merged.get(into).getCount() + s.getCount()));
                origin.get(into).add(i);
            } else {
                merged.add(s);
                what.add(l.get());
                origin.add(new ArrayList<>(List.of(i)));
            }
        }
        if (merged.isEmpty())
            return Optional.empty();
        List<Integer> used = new ArrayList<>();
        for (int i = 0; i < merged.size(); i++)
            used.add(i);
        List<ModularDesign> sheets = ModularBlueprints.designs(plan);
        for (Map.Entry<ResourceLocation, ModularType> e : types.entrySet()) {
            ModularType type = e.getValue();
            if (!may.test(type))
                continue;
            Best best = new Best();
            for (ModularType.Assembly a : type.assemblies(used.size()))
                assign(type, e.getKey(), a.counts(), merged, used, what, 0, new String[used.size()], new boolean[used.size()], best);
            if (best.design == null)
                continue;
            ModularDesign design = best.design;
            ModularFit.Result judged = ModularFit.judge(sheets.stream().filter(d -> d.type().equals(e.getKey())).toList(), design, type, materials);
            ModularFit.Fit fit = ModularFit.fit(judged, start == null ? ModularFit.Start.NONE : start);
            // A coat rolled onto the wearing plate (roll cladding) rides onto the tool, where ModularBake reads it.
            io.github.soundgoodizerfan.feedback.process.Plating coat = null;
            for (int i = 0; i < used.size(); i++)
                if (best.roles[i].equals(type.wearRole()))
                    coat = merged.get(used.get(i)).get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.PLATING.get());
            io.github.soundgoodizerfan.feedback.process.Plating carried = coat;
            Optional<ItemStack> tool = ModularStacks.blank(e.getKey()).filter(stack -> {
                if (carried != null)
                    stack.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.PLATING.get(), carried);
                stack.set(FModular.FIT.get(), fit);
                return ModularStacks.assemble(stack, design, materials, types);
            });
            if (tool.isEmpty())
                continue;
            credit(tool.get(), plan, e.getKey(), design, type, materials);
            if (start != null) {
                ModularFit.start(tool.get(), fit);
                ModularStacks.assemble(tool.get(), design, materials, types);
            }
            String[] role = new String[slots.size()];
            for (int i = 0; i < used.size(); i++)
                for (int slot : origin.get(i))
                    role[slot] = best.roles[i];
            return Optional.of(new Match(e.getKey(), role, tool.get()));
        }
        return Optional.empty();
    }

    /** Whether {@code stack} is plain stock that may merge with another of its material and form: no shaped part, held item or food state. */
    private static boolean mergeable(ItemStack stack, Material.Located what) {
        return !stack.has(FDataComponents.MADE_OF.get()) && !ModularNest.holdable(stack) && what.material().food().isEmpty()
                && !what.material().traits().contains(Material.Trait.BROTH) && !stack.has(FDataComponents.PLATING.get());
    }

    private static int indexOfStock(List<ItemStack> merged, List<Material.Located> what, Material.Located l) {
        for (int i = 0; i < merged.size(); i++)
            if (what.get(i).id().equals(l.id()) && what.get(i).form().equals(l.form()) && mergeable(merged.get(i), what.get(i)))
                return i;
        return -1;
    }

    /**
     * Judges {@code made} against the sheet {@code drawn}: the parts' materials against the drawn materials' floors and bands
     * (yield strength, hardness, conductivity, insulation band, density), their axes against the drawn targets. See {@link ModularFit#against}.
     */
    public static ModularFit.Result drawnTo(ModularDesign drawn, ModularDesign made, ModularType type, Map<ResourceLocation, Material> materials) {
        return ModularFit.against(drawn, made, type, materials);
    }

    /**
     * Writes the credit of the first sheet in {@code plan} that {@code design} is drawn to (parts in spec) onto {@code item}, whoever made it.
     * A sheet of another type, or one the parts depart from, gives nothing. A blueprint is optional: it gates no type, it only gives credit.
     */
    private static void credit(ItemStack item, ItemStack plan, ResourceLocation type, ModularDesign design, ModularType mt,
                               Map<ResourceLocation, Material> materials) {
        for (ItemStack sheet : ModularBlueprints.sheets(plan)) {
            ModularDesign drawn = ModularStacks.design(sheet);
            BlueprintCredit credit = BlueprintCredit.of(sheet);
            if (credit != null && drawn.type().equals(type) && ModularFit.drawnTo(drawn, design, mt, materials)) {
                item.set(FModular.CREDIT.get(), credit.onItem(drawn));
                return;
            }
        }
    }

    /** Drops the credit {@code stack} carries if {@code design} no longer shows the parts it was drawn with. */
    static void keepCredit(ItemStack stack, ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        BlueprintCredit credit = BlueprintCredit.of(stack);
        if (credit != null && credit.drawn().filter(d -> ModularFit.drawnTo(d, design, type, materials)).isEmpty())
            stack.remove(FModular.CREDIT.get());
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
        // A part shaped in a variant its role does not list (a coil barrel in a flintlock) is not that role's part.
        var made = stack.get(FDataComponents.MADE_OF.get());
        if (made != null && !made.variant().isEmpty() && role.part().filter(s -> !s.variants().isEmpty()).isPresent()
                && role.part().get().variant(made.variant()).isEmpty())
            return false;
        return stack.getCount() == wanted;
    }

    /** The part a role gets from {@code stack}: a shaped part by what it was shaped from, raw stock as it lies. */
    public static Part part(String role, Material.Located what, ItemStack stack, int count) {
        if (ModularNest.holdable(stack))
            return Part.holding(role, what.id(), what.form(), stack);
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        // A pot serving is food already baked: its figures come off the stack, not a material block.
        if (made == null && what.material().traits().contains(Material.Trait.BROTH))
            return new Part(role, what.id(), what.form(), count, "", "", 0, Measures.ofState(DishBake.servingState(stack)));
        if (made == null && what.material().food().isPresent())
            return new Part(role, what.id(), what.form(), count, "", stack.getOrDefault(FDataComponents.FOOD_SHAPE.get(), ""), 0, Measures.ofState(foodStateMap(stack, what)));
        if (made != null && !(stack.getItem() instanceof ModularPartItem)) // raw stock the enchanting table stamped
            return new Part(role, what.id(), what.form(), count, "", made.variant(), made.flaw(), made.measures());
        return made != null ? new Part(role, made.material(), made.stock(), made.cells(),
                BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(), made.variant(), made.flaw(), made.measures())
                : new Part(role, what.id(), what.form(), count);
    }

    /** A food's state plus what the Prep Table did to it: its cut level and evenness grade. */
    private static java.util.Map<String, Float> foodStateMap(ItemStack stack, Material.Located what) {
        java.util.Map<String, Float> state = DishBake.stateOf(foodState(stack, what));
        io.github.soundgoodizerfan.feedback.process.FoodCut cut = stack.get(FDataComponents.FOOD_CUT.get());
        if (cut != null) {
            state.put("cut", (float) cut.level());
            state.put("evenness", (float) cut.grade());
        }
        io.github.soundgoodizerfan.feedback.process.FoodKnead knead = stack.get(FDataComponents.FOOD_KNEAD.get());
        if (knead != null)
            state.put(DishBake.KNEAD, (float) knead.grade());
        return state;
    }

    /** A food stack's state as it is stored (infer has no level to settle it by), else its material's own: the dish bakes from it. */
    private static io.github.soundgoodizerfan.feedback.process.FoodState foodState(ItemStack stack, Material.Located what) {
        io.github.soundgoodizerfan.feedback.process.FoodState stored = stack.get(FDataComponents.FOOD_STATE.get());
        return stored != null ? stored : DishBake.fresh(what.material().food().get(), what.form());
    }

    // --- what the slots could still become ------------------------------------------------------

    /**
     * The types the non-empty slots could be on the way to (the Prep Table's fan reads this for a dish): every stack takes a distinct role of the
     * type, whatever the counts. The screen names the roles of these so the player can see what is
     * missing.
     */
    public static List<ResourceLocation> candidates(List<ItemStack> slots, Map<ResourceLocation, Material> materials,
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
        for (Map.Entry<ResourceLocation, ModularType> e : types.entrySet()) {
            ModularType type = e.getValue();
            if (type.patterns().isEmpty())
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
     * Problems in a set of types: two that accept the same part set, any whose laid roles need more than
     * the Engineering Table's nine grid slots (a dish: the Prep Table's six), and a type with two roles that take an overlapping item (the slot an item sits in
     * must never decide what it is). Roles are compared by what they accept, counts exactly, since a pattern is only how
     * many of each role there are. A wildcard narrowed by required traits (a lining) is trusted not to meet an explicit form (studs); otherwise it errs towards reporting.
     */
    public static List<String> audit(Map<ResourceLocation, ModularType> types) {
        List<String> out = new ArrayList<>();
        List<Map.Entry<ResourceLocation, ModularType>> list = new ArrayList<>(types.entrySet());
        // The fullest assembly of a form is the form with every optional part on, so the limits are checked on that, not on each combination.
        for (Map.Entry<ResourceLocation, ModularType> e : list) {
            ModularType t = e.getValue();
            // Optional parts are fitted to a finished item on the Fit tab (ModularRework add), so they never share the grid with the core
            // roles at assembly: only the laid (required) roles of a form must fit, a complex type's as much as a simple one's.
            int slots = t.family() == ModularType.Family.DISH ? DISH_SLOTS : GRID_SLOTS;
            for (Map<String, Integer> form : t.forms()) {
                if (form.size() > slots)
                    out.add(e.getKey() + " has " + form.size() + " required roles in " + form + ", more than the " + slots + " slots of its table");
                if (form.values().stream().anyMatch(n -> n > CAPACITY))
                    out.add(e.getKey() + " asks for more than " + CAPACITY + " of one role in " + form + ", more than a table slot holds");
            }
            for (String x : t.extras())
                if (t.roles().get(x).bulk() > CAPACITY)
                    out.add(e.getKey() + " asks for more than " + CAPACITY + " of optional role " + x + ", more than a table slot holds");
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
                    boolean apart = e.getValue().family() == ModularType.Family.DISH && !a.seen().isEmpty() && !b.seen().isEmpty()
                            && java.util.Collections.disjoint(a.seen(), b.seen());
                    if (!(a.striker() && b.striker()) && !pair && !apart && overlap(a, b))
                        out.add(e.getKey() + " roles " + a.name() + " and " + b.name() + " accept overlapping items: "
                                + a.forms() + " against " + b.forms() + "; make one a shaped part with an item of its own");
                }
        }
        for (int i = 0; i < list.size(); i++)
            for (int j = i + 1; j < list.size(); j++) {
                ModularType a = list.get(i).getValue();
                ModularType b = list.get(j).getValue();
                // A complex type is assembled only at the Engineering Table and a dish at the Prep Table, and neither meets a simple type, so they cannot be confused.
                if (a.blueprint() != b.blueprint() || kindOf(a) != kindOf(b))
                    continue;
                ResourceLocation ia = list.get(i).getKey(), ib = list.get(j).getKey();
                clash(a, b).ifPresent(maps -> out.add(ia + " and " + ib + " accept the same parts (" + maps
                        + "): the Engineering Table could not tell which to make"));
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
        // Role pairs are judged once per type pair, and two assemblies whose sorted counts differ can never be paired, so most are skipped cheaply.
        Map<String, Boolean> meets = new HashMap<>();
        // Optional parts are fitted on afterwards and a gun has dozens of them (millions of subsets), so the audit judges the required parts alone.
        Map<List<Integer>, List<Map<String, Integer>>> bySig = new HashMap<>();
        for (ModularType.Assembly y : b.assembliesUpTo(0))
            bySig.computeIfAbsent(signature(y.counts()), k -> new ArrayList<>()).add(y.counts());
        for (ModularType.Assembly x : a.assembliesUpTo(0))
            for (Map<String, Integer> y : bySig.getOrDefault(signature(x.counts()), List.of()))
                if (confusable(a, x.counts(), b, y, meets))
                    return Optional.of(x.counts() + " against " + y);
        return Optional.empty();
    }

    /** Whether some pairing of the two maps' roles matches counts and has overlapping accepts everywhere (a perfect matching, found by augmenting paths). */
    private static boolean confusable(ModularType a, Map<String, Integer> x, ModularType b, Map<String, Integer> y, Map<String, Boolean> meets) {
        List<Map.Entry<String, Integer>> mine = new ArrayList<>(x.entrySet());
        List<Map.Entry<String, Integer>> theirs = new ArrayList<>(y.entrySet());
        if (mine.size() > theirs.size())
            return false;
        int[] taken = new int[theirs.size()];
        java.util.Arrays.fill(taken, -1);
        for (int i = 0; i < mine.size(); i++)
            if (!augment(a, mine, b, theirs, i, new boolean[theirs.size()], taken, meets))
                return false;
        return true;
    }

    /** The counts of an assembly, sorted: a pairing must match counts, so unequal signatures cannot pair. */
    private static List<Integer> signature(Map<String, Integer> counts) {
        return counts.values().stream().sorted().toList();
    }

    private static boolean augment(ModularType a, List<Map.Entry<String, Integer>> mine, ModularType b, List<Map.Entry<String, Integer>> theirs,
                                   int i, boolean[] seen, int[] taken, Map<String, Boolean> meets) {
        ModularType.Role left = a.roles().get(mine.get(i).getKey());
        for (int k = 0; k < theirs.size(); k++) {
            if (seen[k])
                continue;
            Map.Entry<String, Integer> t = theirs.get(k);
            ModularType.Role right = b.roles().get(t.getKey());
            // Two dishes' food roles that need different traits (a starchy base, a broth base) never take one item.
            boolean apart = a.family() == ModularType.Family.DISH && b.family() == ModularType.Family.DISH && left != null && right != null
                    && !left.seen().isEmpty() && !right.seen().isEmpty() && java.util.Collections.disjoint(left.seen(), right.seen());
            if (left == null || right == null || apart || !t.getValue().equals(mine.get(i).getValue())
                    || !meets.computeIfAbsent(mine.get(i).getKey() + "|" + t.getKey(), key -> overlap(left, right)))
                continue;
            seen[k] = true;
            if (taken[k] < 0 || augment(a, mine, b, theirs, taken[k], seen, taken, meets)) {
                taken[k] = i;
                return true;
            }
        }
        return false;
    }

    private static boolean overlap(ModularType.Role a, ModularType.Role b) {
        boolean sa = a.part().isPresent() || a.striker(), sb = b.part().isPresent() || b.striker();
        for (String f : a.forms())
            for (String g : b.forms())
                if (!(sb && f.startsWith("*")) && !(sa && g.startsWith("*"))
                        && !(f.startsWith("*") && !a.seen().isEmpty() && !g.startsWith("*"))
                        && !(g.startsWith("*") && !b.seen().isEmpty() && !f.startsWith("*")) && formsMeet(f, g))
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
