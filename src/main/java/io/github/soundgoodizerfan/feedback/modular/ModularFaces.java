// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.process.Measures;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The hafted plan (spec 2): a haft with one to three <b>faces</b> (heads, blades, points: {@link ModularHeads}) on its front, back and top
 * mounts. The plan has no dig table, strike figure, combat class or name of its own; each comes from the faces it carries, and nothing
 * is authored per combination. {@link #view} is the seam: for one design it returns the plan's type as the build sees it, with the
 * front face's family, mines, strike and wear and each face's own litres, place and layer, so every bake and trait path that reads
 * a type works unchanged.
 *
 * <p>Each face wears on its own: its share spent is kept in its part's {@link Measures#state} ({@link #WORN}), the bar shows the most
 * worn face, a face past its life is {@linkplain #spent spent} and digs and cuts nothing, and the tool is {@linkplain
 * ModularStacks#broken broken} only when every face is.
 */
public final class ModularFaces {

    private ModularFaces() {
    }

    /** The one hafted item and type. */
    public static final ResourceLocation PLAN = Feedback.id("hafted_tool");
    /** The head kinds in the order the item model's {@code feedback:face} overrides list them: the property is 1 plus a kind's place here. */
    public static final List<String> KINDS = List.of("pickaxe_head", "axe_head", "shovel_head", "hoe_head", "maul_head", "sickle_head",
            "cleaver_head", "sword_blade", "spear_head", "head_blank", "knife_blade", "file_head", "driver_bit");
    /** The face letters, front first: the grid's reading order seats the first face here. */
    public static final List<String> LETTERS = List.of("H", "J", "K");
    /** State key on a face's part: the share of its life spent, 0 new to 1 spent. */
    public static final String WORN = "worn";
    /** State key on a face's part: 1 once its life is spent. */
    public static final String SPENT = "spent";

    /** The face a swing or a dig is wearing right now: set around the vanilla call that charges the durability, then cleared. */
    private static final ThreadLocal<Integer> ACTIVE = ThreadLocal.withInitial(() -> 0);

    public static boolean hafted(ModularDesign design) {
        return design != null && design.type().equals(PLAN);
    }

    public static boolean spent(Part face) {
        return face != null && face.measures().state(SPENT, 0f) > 0f;
    }

    /** The faces of {@code design}, front first, as parts (a face not fitted is left out). */
    public static List<Part> faces(ModularDesign design) {
        List<Part> out = new ArrayList<>(3);
        for (String letter : LETTERS) {
            Part p = design.part(letter);
            if (p != null)
                out.add(p);
        }
        return out;
    }

    /** The definition of the head kind a face part is. */
    public static Optional<ModularHeads> kind(Part face, ModularType type) {
        ModularType.Role role = type.roles().get(face.role());
        return role == null ? Optional.empty() : ModularHeads.of(ModularHeads.kindOf(face, role));
    }

    /** Whether every face of {@code design} is spent (and there is at least one). */
    public static boolean allSpent(ModularDesign design) {
        boolean any = false;
        for (String letter : LETTERS) {
            Part p = design.part(letter);
            if (p == null)
                continue;
            any = true;
            if (!spent(p))
                return false;
        }
        return any;
    }

    /**
     * {@code type} as {@code design} builds it. A type that is not the hafted plan is returned as it is. For the plan: the first face that
     * is not spent gives the family, mines, strike and wear; each face's role takes its kind's litres, place on the haft and layer.
     */
    public static ModularType view(ModularType type, ModularDesign design) {
        if (type.family() != ModularType.Family.HAFTED)
            return type;
        Map<String, ModularType.Role> roles = new LinkedHashMap<>(type.roles());
        ModularHeads front = null;
        for (String letter : LETTERS) {
            Part p = design.part(letter);
            ModularType.Role role = type.roles().get(letter);
            if (p == null || role == null)
                continue;
            Optional<ModularHeads> kind = ModularHeads.of(ModularHeads.kindOf(p, role));
            if (kind.isEmpty())
                continue;
            roles.put(letter, role.withFace(kind.get().role()));
            if (front == null && !spent(p))
                front = kind.get();
        }
        if (front == null)
            return type.with(ModularType.Family.DIG, List.of(), type.strike(), type.wearLitres(), roles, type.trait(), type.traitParams());
        return type.with(front.combat(), front.mines(), front.strike(), front.wearLitres(), roles, front.trait(), front.traitParams());
    }

    /** The family a swing of {@code stack} is judged as, from its front live face: the base for any item that is not hafted. */
    public static ModularType.Family combat(ItemStack stack) {
        ModularDesign d = ModularStacks.design(stack);
        if (d == null)
            return null;
        ModularType type = ModularTypes.known().get(d.type());
        if (type == null)
            return null;
        if (type.family() != ModularType.Family.HAFTED)
            return type.family();
        for (String letter : LETTERS) {
            Part p = d.part(letter);
            if (p == null || spent(p))
                continue;
            ModularType.Role role = type.roles().get(letter);
            return role == null ? ModularType.Family.DIG
                    : ModularHeads.of(ModularHeads.kindOf(p, role)).map(ModularHeads::combat).orElse(ModularType.Family.DIG);
        }
        return ModularType.Family.DIG;
    }

    /** What the live faces of {@code stack} do to a block they are used on, merged: strip, scrape, till and the rest. */
    public static Set<String> actions(ItemStack stack) {
        Set<String> out = new LinkedHashSet<>();
        ModularDesign d = ModularStacks.design(stack);
        ModularType type = d == null ? null : ModularTypes.known().get(d.type());
        if (type == null || type.family() != ModularType.Family.HAFTED)
            return out;
        for (Part p : faces(d))
            if (!spent(p))
                kind(p, type).ifPresent(h -> out.addAll(h.actions()));
        return out;
    }

    /** The treatise a build of {@code design} needs: the first gated face's school, or empty. A build holding a gated face is complex. */
    public static String school(ModularDesign design, ModularType type) {
        for (Part p : faces(design)) {
            String school = kind(p, type).map(ModularHeads::school).orElse("");
            if (!school.isEmpty())
                return school;
        }
        return "";
    }

    /** The live faces of {@code stack}'s kinds, front first (empty for an item that is not hafted). */
    private static List<ModularHeads> liveKinds(ItemStack stack) {
        ModularDesign d = ModularStacks.design(stack);
        ModularType type = d == null ? null : ModularTypes.known().get(d.type());
        if (type == null || type.family() != ModularType.Family.HAFTED)
            return List.of();
        List<ModularHeads> out = new ArrayList<>(3);
        for (Part p : faces(d))
            if (!spent(p))
                kind(p, type).ifPresent(out::add);
        return out;
    }

    /** Whether a live face of {@code stack} is the head kind {@code kind} ({@code sickle_head}). */
    public static boolean has(ItemStack stack, String kind) {
        ModularDesign d = ModularStacks.design(stack);
        ModularType type = d == null ? null : ModularTypes.known().get(d.type());
        if (type == null || type.family() != ModularType.Family.HAFTED)
            return false;
        for (Part p : faces(d))
            if (!spent(p) && type.roles().containsKey(p.role()) && ModularHeads.kindOf(p, type.roles().get(p.role())).equals(kind))
                return true;
        return false;
    }

    /** Whether a live face digs {@code mineable/<tool>} ({@code pickaxe}, {@code axe}, {@code shovel}, {@code hoe}). */
    public static boolean digs(ItemStack stack, String tool) {
        for (ModularHeads h : liveKinds(stack))
            for (ModularType.Mine m : h.mines())
                if (m.tag().getPath().equals("mineable/" + tool))
                    return true;
        return false;
    }

    /** Whether a live face is a bench tool's (hammer, knife, file, driver bit): the item is also a hand tool. */
    public static boolean hand(ItemStack stack) {
        for (ModularHeads h : liveKinds(stack))
            if (h.combat() == ModularType.Family.HAND)
                return true;
        return false;
    }

    /** Whether a live face is a bench tool's that lands a blow (the hammer's). */
    public static boolean handStrikes(ItemStack stack) {
        for (ModularHeads h : liveKinds(stack))
            if (h.combat() == ModularType.Family.HAND && h.strike() > 0f)
                return true;
        return false;
    }

    /** The Engineering Table's tool abilities a live face gives: a hammer face is the hammer, a file head the file, a driver bit the driver. */
    public static boolean toolAbility(ItemStack stack, net.neoforged.neoforge.common.ItemAbility ability) {
        ModularDesign d = ModularStacks.design(stack);
        ModularType type = d == null ? null : ModularTypes.known().get(d.type());
        if (type == null || type.family() != ModularType.Family.HAFTED)
            return false;
        for (Part p : faces(d)) {
            if (spent(p) || !type.roles().containsKey(p.role()))
                continue;
            net.neoforged.neoforge.common.ItemAbility has = io.github.soundgoodizerfan.feedback.modular.engineering.ToolAbilities.forFace(
                    ModularHeads.kindOf(p, type.roles().get(p.role())));
            if (has != null && has.equals(ability))
                return true;
        }
        return false;
    }

    /** The hafted plan's letter for a role of a face definition: its face, haft, binding, tip, guard, pommel, spring, lanyard or edge guard. */
    public static String haftRole(ModularType.Role r) {
        return switch (r.name()) {
            case "head", "blade", "bit" -> "H";
            case "handle", "grip" -> "S";
            case "binding" -> "B";
            case "tip" -> "T";
            case "guard" -> "G";
            case "pommel" -> r.part().filter(s -> s.item().getPath().equals("sword_pommel")).isPresent() ? "Q" : "P";
            case "spring" -> "R";
            case "lanyard" -> "Y";
            default -> "Z";
        };
    }

    /** {@code design} of a face definition's type ({@code def}) as the hafted plan holds it: the same parts in the plan's letters. */
    public static ModularDesign toPlan(ModularDesign design, ModularType def) {
        List<Part> mapped = new ArrayList<>();
        for (Part p : design.parts()) {
            ModularType.Role r = def.roles().get(p.role());
            mapped.add(new Part(r == null ? p.role() : haftRole(r), p.material(), p.form(), p.count(), p.kind(), p.variant(), p.flaw(), p.measures()));
        }
        return new ModularDesign(PLAN, mapped, design.edge());
    }

    /**
     * {@code design} as an order for the face-definition type {@code orderType} sees it (a commission for a sword, a pickaxe, a cleaver): the
     * same parts in that type's letters, if the front face is that type's head kind; else the design itself when it is of that type, else null.
     */
    public static ModularDesign forOrder(ModularDesign design, ResourceLocation orderType) {
        if (design == null)
            return null;
        if (!hafted(design))
            return design.type().equals(orderType) ? design : null;
        ModularType def = ModularTypes.known().get(orderType);
        ModularType plan = ModularTypes.known().get(PLAN);
        if (def == null || !def.face() || plan == null)
            return null;
        ModularType.Role wear = def.roles().get(def.wearRole());
        if (wear == null || wear.part().isEmpty())
            return null;
        for (Part p : faces(design)) {
            if (spent(p))
                continue;
            if (!ModularHeads.kindOf(p, plan.roles().get(p.role())).equals(wear.part().get().item().getPath()))
                return null;
            List<Part> mapped = new ArrayList<>();
            for (Part q : design.parts()) {
                String letter = q.role();
                for (Map.Entry<String, ModularType.Role> e : def.roles().entrySet())
                    if (haftRole(e.getValue()).equals(q.role()) && !LETTERS.contains(q.role())) {
                        letter = e.getKey();
                        break;
                    }
                if (LETTERS.contains(q.role()))
                    letter = q == p ? def.wearRole() : letter;
                mapped.add(new Part(letter, q.material(), q.form(), q.count(), q.kind(), q.variant(), q.flaw(), q.measures()));
            }
            return new ModularDesign(orderType, mapped, design.edge());
        }
        return null;
    }

    /**
     * Mending a hafted tool: stock of a face's own material puts that face's life back, a cell of it giving back its share of the
     * part's cells. The most worn face that takes the stock is the one mended; a face that was spent works again once it has life.
     * Empty when nothing is worn or no face is made of the stock.
     */
    public static Optional<ItemStack> mend(ItemStack tool, List<ItemStack> stock, io.github.soundgoodizerfan.feedback.process.Material.Located what,
                                           ModularType type) {
        ModularDesign d = ModularStacks.design(tool);
        if (d == null)
            return Optional.empty();
        Part best = null;
        float bestWorn = 0f;
        for (Part p : faces(d)) {
            ModularType.Role role = type.roles().get(p.role());
            float worn = p.measures().state(WORN, 0f);
            if (role != null && p.material().equals(what.id()) && role.acceptsStock(what) && worn > bestWorn) {
                best = p;
                bestWorn = worn;
            }
        }
        if (best == null)
            return Optional.empty();
        float cells = 0;
        for (ItemStack o : stock)
            cells += o.getCount() * (float) io.github.soundgoodizerfan.feedback.registry.FormItems.volume(what.form()) / ModularTuning.CELL_MB;
        float back = cells / Math.max(1, best.count());
        float worn = Math.max(0f, bestWorn - back);
        Measures m = best.measures().withState(WORN, worn).withState(SPENT, 0f);
        ItemStack out = tool.copy();
        out.setCount(1);
        ModularDesign next = d.withPart(new Part(best.role(), best.material(), best.form(), best.count(), best.kind(), best.variant(), best.flaw(), m));
        out.set(FModular.DESIGN.get(), next);
        float worst = 0f;
        for (Part p : faces(next))
            worst = Math.max(worst, spent(p) ? 1f : p.measures().state(WORN, 0f));
        int max = Math.max(1, out.getMaxDamage());
        out.setDamageValue(allSpent(next) ? max - 1 : Math.min(max - 2, Math.round(worst * max)));
        ModularStacks.rebake(out);
        return Optional.of(out);
    }

    // --- wear, face by face ---------------------------------------------------------------------

    /** Names the face the next durability point lands on: the first live face that digs {@code state}, else the front one. */
    public static void use(ItemStack stack, BlockState state) {
        ModularDesign d = ModularStacks.design(stack);
        ModularType type = d == null ? null : ModularTypes.known().get(d.type());
        int chosen = 0;
        if (type != null && type.family() == ModularType.Family.HAFTED && state != null) {
            List<Part> faces = faces(d);
            for (int i = 0; i < faces.size(); i++) {
                Part p = faces.get(i);
                if (!spent(p) && kind(p, type).filter(h -> h.digs(state)).isPresent()) {
                    chosen = i;
                    break;
                }
            }
        }
        ACTIVE.set(chosen);
    }

    /** The face the next point lands on is the front one again. */
    public static void done() {
        ACTIVE.set(0);
    }

    /**
     * Charges {@code amount} points to the face in use: its share of its own life (figure {@link Fig#DURABILITY} for the front face,
     * {@link Fig#DURABILITY_B} and {@link Fig#DURABILITY_C} for the others) goes up, the bar shows the most worn live face, and a face
     * that passes its life is spent and the tool rebaked without it. Returns the points the vanilla call should add: none, the bar is set here.
     */
    public static int wear(ItemStack stack, int amount) {
        ModularDesign d = ModularStacks.design(stack);
        ModularFigures f = ModularStacks.figures(stack);
        if (d == null || f == null || amount <= 0)
            return 0;
        List<Part> faces = faces(d);
        if (faces.isEmpty())
            return 0;
        int index = Math.min(ACTIVE.get(), faces.size() - 1);
        // A spent face is not worn further: the point lands on the next face that still lives.
        if (spent(faces.get(index)))
            for (int i = 0; i < faces.size(); i++)
                if (!spent(faces.get(i))) {
                    index = i;
                    break;
                }
        Part face = faces.get(index);
        if (spent(face))
            return 0;
        float life = Math.max(1f, f.get(index == 0 ? Fig.DURABILITY : index == 1 ? Fig.DURABILITY_B : Fig.DURABILITY_C));
        float worn = Math.min(1f, face.measures().state(WORN, 0f) + ModularStacks.soak(stack, amount) / life);
        boolean wasSpent = worn >= 1f;
        Measures m = face.measures().withState(WORN, worn);
        if (wasSpent)
            m = m.withState(SPENT, 1f);
        ModularDesign next = d.withPart(new Part(face.role(), face.material(), face.form(), face.count(), face.kind(), face.variant(), face.flaw(), m));
        stack.set(FModular.DESIGN.get(), next);
        float worst = 0f;
        for (Part p : faces(next))
            worst = Math.max(worst, spent(p) ? 1f : p.measures().state(WORN, 0f));
        int max = Math.max(1, stack.getMaxDamage());
        stack.setDamageValue(allSpent(next) ? max - 1 : Math.min(max - 2, Math.round(worst * max)));
        if (wasSpent || ModularStacks.stepOf(stack) != f.step())
            ModularStacks.rebake(stack);
        return 0;
    }
}
