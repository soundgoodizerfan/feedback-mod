// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bench;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FormItems;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularBake;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.item.ModularRework;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.part.ModularPartItem;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Making a part at the Tool Table: the shapes it can make, and what stock makes each. This is what
 * {@code ModularPartRecipe} was, as plain functions of a list of stacks rather than a crafting grid
 * -- the rules are the same ones.
 *
 * <ul>
 *   <li><b>Every shape of every type is on the list</b>: a shaped role's standard shape, each
 *       variant that lays cells ({@link ModularType.Shaping#laid}) and each variant that is forged
 *       ({@link ModularType.Shaping#forged}). The list is the player's choice; nothing infers it,
 *       because one stock makes any of several shapes.</li>
 *   <li><b>Stock is volume, and divides.</b> A cell is an ingot (144 mB, {@link ModularTuning#CELL_MB}); the pile
 *       must hold at least the shape's cells in volume, all of one material and one form the shape
 *       takes. The shape spends only what it needs and {@link #change} returns the rest as smaller items
 *       (a stone block, nine ingots' worth, gives a part and the ingots left over). A forged variant costs
 *       the standard shape's cells. A part is never made from a part.</li>
 *   <li><b>The hot-working window holds.</b> A material with a window must be inside it, judged on
 *       the coldest piece, and the part comes out at that heat -- hot wherever it goes (philosophy 9).
 *       Heat is whatever the stock items carry ({@link ItemHeat}); a Hearth beside the table
 *       reheats them in the cells ({@code machine.hearth.HearthBlockEntity}).</li>
 *   <li><b>A forged variant plays its rhythm</b> ({@link ForgeRhythm}) in the table's screen; the
 *       flaws it earns are on the part. Any other comes out as soon as the stock is in.</li>
 * </ul>
 */
public final class PartBenchShaping {

    private PartBenchShaping() {
    }

    /** One entry in the shape list: a shape of a part of some type. */
    public record Option(ResourceLocation type, ModularType.Shaping shaping, ModularType.Variant variant) {

        /** Whether it is played on the anvil rather than made as soon as the stock is in. */
        public boolean forged() {
            return variant.cells() <= 0f && !variant.rhythm().isEmpty();
        }

        /** Its size in cells of stock at default axes: its own if made at its own size, the standard shape's if forged. */
        public float size() {
            return forged() ? shaping.cells() : variant.cells();
        }

        /** Whole cells of stock it takes at default axes, rounded up: what a part item records and a tooltip counts. */
        public int cells() {
            return (int) Math.ceil(size());
        }

        /** Millibuckets of stock it takes when its axes scale its volume by {@code scale}, up to the next nugget (the smallest stock there is). */
        public int stockMb(float scale) {
            int nuggets = (int) Math.ceil(size() * scale * ModularTuning.CELL_MB / ModularTuning.NUGGET_MB - 1e-3f);
            return Math.max(1, nuggets) * ModularTuning.NUGGET_MB;
        }

        /** The name the list shows: the part, and the shape if it is not the standard one. */
        public Component label() {
            Component part = Component.translatable("item." + shaping.item().getNamespace() + "." + shaping.item().getPath());
            // A standard shape that is itself a named kind (a receiver's is a handgun) shows its name.
            String name = variant.name().isEmpty() ? shaping.base() : variant.name();
            return name.isEmpty() ? part : Component.translatable("modular.feedback.variant." + name);
        }
    }

    /** Every shape the types can make, in type order, each part once: the standard, the laid, then the forged. */
    public static List<Option> options(Map<ResourceLocation, ModularType> types) {
        List<Option> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map.Entry<ResourceLocation, ModularType> e : types.entrySet()) {
            List<ModularType.Shaping> shapings = new ArrayList<>();
            for (String key : e.getValue().order())
                e.getValue().roles().get(key).part().ifPresent(shapings::add);
            shapings.addAll(e.getValue().shapes());
            for (ModularType.Shaping shaping : shapings) {
                List<ModularType.Variant> all = new ArrayList<>(shaping.laid());
                all.addAll(shaping.forged());
                for (ModularType.Variant v : all)
                    if (seen.add(shaping.item() + "/" + v.name()))
                        out.add(new Option(e.getKey(), shaping, v));
            }
        }
        return out;
    }

    /**
     * Whether stock of {@code what}, in any amount, could make {@code option} at {@code station}: the bench works that
     * kind of stuff, the form is one the shape takes, and the material has the traits it asks for. What the bench's list
     * shows once a material is in the cells.
     */
    public static boolean takes(Station station, Option option, Material.Located what) {
        return (!option.forged() || station.forges()) && station.accepts(what.material())
                && ModularType.fits(option.shaping().stock(), what.form())
                && what.material().traits().containsAll(option.shaping().traits())
                && holdsExperience(option, what);
    }

    /** A vial body of a material that holds no experience bakes into nothing, so the bench does not offer it. */
    private static boolean holdsExperience(Option option, Material.Located what) {
        ModularType type = ModularTypes.known(option.type());
        if (type != null && type.family() == ModularType.Family.CELL)
            return what.material().electrical().secondaryCell().isPresent();
        if (type != null && type.family() == ModularType.Family.BELT)
            return type.roles().entrySet().stream()
                    .filter(r -> r.getValue().part().filter(s -> s.item().equals(option.shaping().item())).isPresent())
                    .allMatch(r -> ModularBake.suits(type, r.getKey(), what.material()));
        if (type != null && (type.family() == ModularType.Family.MACHINE || type.family() == ModularType.Family.ENGINE
                || type.family() == ModularType.Family.GEARBOX || type.family() == ModularType.Family.PRESSURE || type.family() == ModularType.Family.BLOCK
                || type.family() == ModularType.Family.PROPELLER))
            return type.roles().entrySet().stream()
                    .filter(r -> r.getValue().part().filter(s -> s.item().equals(option.shaping().item())).isPresent())
                    .allMatch(r -> ModularBake.suits(type, r.getKey(), what.material()));
        return type == null || type.family() != ModularType.Family.VESSEL || what.material().xpStore().isPresent();
    }

    /**
     * What a valid pile of stock is: the stuff, how many pieces, and the coldest piece's heat. {@code what} is the part's main slot;
     * {@code more} are the other slots filled from stock of their own (a lens, an edge), each spending its own pile.
     */
    public record Stock(Material.Located what, int cells, int spent, float coldest, int mb, List<Extra> more) {

        public Stock(Material.Located what, int cells, int spent, float coldest, int mb) {
            this(what, cells, spent, coldest, mb, List.of());
        }

        /** One extra slot's pile: the slot's name and its stock slot beside the centre (0 for the first), its stuff, what the shape spends of it and its coldest piece's heat. */
        public record Extra(String slot, int index, Material.Located what, int spent, float coldest, int mb) {
            public int left() {
                return mb - spent;
            }
        }

        /** Millibuckets of the pile the shape leaves. */
        public int left() {
            return mb - spent();
        }
    }

    /**
     * Whether {@code contents} is exactly the stock {@code option} takes. Empty cells are ignored.
     * A piece that is already a part, an unknown item, a second material or form, the wrong number,
     * or a material outside its hot-working window all refuse.
     */
    public static Optional<Stock> check(Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level) {
        return check(Station.TOOL, option, contents, materials, level);
    }

    /** As {@link #check(Option, List, Map, Level)}, at a bench that takes only the stock it works ({@link Station}). */
    public static Optional<Stock> check(Station station, Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level) {
        return check(station, option, contents, materials, level, false, 1f);
    }

    /** As above, for a part aimed at axes that scale its volume by {@code scale} ({@link ModularType.Role#volumeScaleOf}). */
    public static Optional<Stock> check(Station station, Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level,
                                        float scale) {
        return check(station, option, contents, materials, level, false, scale);
    }

    /** The role of {@code option}'s type whose part is the one this option makes, if it has one. */
    public static Optional<ModularType.Role> roleOf(Option option) {
        ModularType type = ModularTypes.known(option.type());
        if (type == null)
            return Optional.empty();
        return type.roles().values().stream()
                .filter(r -> r.part().filter(s -> s.item().equals(option.shaping().item())).isPresent()).findFirst();
    }

    /** The axes the bench lets the player aim for {@code option}'s part, in the order the menu indexes them; empty for a part that declares none. */
    public static List<Map.Entry<String, ModularType.Axis>> aimable(Option option) {
        return roleOf(option).map(r -> r.sortedAxes(option.variant().name())).orElse(List.of());
    }

    /**
     * The spread of an as-built axis around its aim, as a share of the axis's range: a laid part's flat
     * {@link ModularTuning#AXIS_ERROR_LAID}; a forged one's from {@link ModularTuning#AXIS_ERROR_GOOD} at a work ratio of 1
     * to {@link ModularTuning#AXIS_ERROR_POOR} as the ratio strays {@link ModularTuning#AXIS_ERROR_RATIO_SPAN}, and each flaw adds more.
     */
    public static float spread(Option option, float workRatio, int flaw) {
        if (!option.forged())
            return ModularTuning.AXIS_ERROR_LAID;
        float off = Math.min(1f, Math.abs(workRatio - 1f) / ModularTuning.AXIS_ERROR_RATIO_SPAN);
        return ModularTuning.AXIS_ERROR_GOOD + (ModularTuning.AXIS_ERROR_POOR - ModularTuning.AXIS_ERROR_GOOD) * off
                + flaw * ModularTuning.AXIS_ERROR_PER_FLAW;
    }

    /**
     * What the part is built to: each axis the role declares, its aim (default if unaimed) plus symmetric noise of
     * {@code spread} of the range, never exactly zero, clamped to the axis. Axes the role does not declare are dropped.
     */
    public static Map<String, Float> asBuilt(Option option, Map<String, Float> aim, float spread, net.minecraft.util.RandomSource random) {
        Map<String, Float> out = new java.util.HashMap<>();
        for (Map.Entry<String, ModularType.Axis> a : aimable(option)) {
            ModularType.Axis ax = a.getValue();
            float noise = (random.nextFloat() * 2f - 1f) * spread * (ax.max() - ax.min());
            float floor = ax.grain() * 0.25f;
            if (Math.abs(noise) < floor)
                noise = noise < 0f || (noise == 0f && random.nextBoolean()) ? -floor : floor;
            out.put(a.getKey(), ax.clamp(aim.getOrDefault(a.getKey(), ax.def()) + noise));
        }
        return out;
    }

    /** As above; {@code exact} refuses a pile bigger than the shape (a food shape reshapes the whole pile). */
    private static Optional<Stock> check(Station station, Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level,
                                         boolean exact, float scale) {
        return check(station, option, contents, List.of(), materials, level, exact, scale);
    }

    /**
     * As above for a part of several slots: {@code extras} are the piles for the slots after the first, in order. A slot left empty is made of
     * the first slot's material, drawn from its pile; a filled one takes its share of the stock from its own pile, of one material and one form
     * the slot accepts ({@link ModularType.PartSlot#accepts}), inside its hot-working window.
     */
    public static Optional<Stock> check(Station station, Option option, List<ItemStack> contents, List<ItemStack> extras,
                                        Map<ResourceLocation, Material> materials, Level level, float scale) {
        return check(station, option, contents, extras, materials, level, false, scale);
    }

    private static Optional<Stock> check(Station station, Option option, List<ItemStack> contents, List<ItemStack> extras,
                                         Map<ResourceLocation, Material> materials, Level level, boolean exact, float scale) {
        if (option.forged() && !station.forges())
            return Optional.empty();
        List<ModularType.PartSlot> slots = option.shaping().slots();
        List<Stock.Extra> more = new ArrayList<>();
        int need = option.stockMb(scale);
        int mainNeed = need;
        List<ModularType.PartSlot> choices = option.shaping().choices();
        for (int j = 0; j < choices.size() && j < extras.size(); j++) {
            int i = slots.indexOf(choices.get(j));
            ItemStack s = extras.get(j);
            if (s.isEmpty())
                continue;
            if (s.has(FDataComponents.MADE_OF.get()))
                return Optional.empty();
            Optional<Material.Located> l = Material.find(materials, s.getItem());
            int share = Math.max(1, (int) Math.ceil(need * option.shaping().shareOf(i) / ModularTuning.NUGGET_MB - 1e-3f)) * ModularTuning.NUGGET_MB;
            int mb = s.getCount() * (l.isEmpty() ? 0 : FormItems.volume(l.get().form()));
            if (l.isEmpty() || !station.accepts(l.get().material()) || !slots.get(i).accepts(l.get(), option.shaping().stock()) || mb < share)
                return Optional.empty();
            float heat = ItemHeat.get(s, level).value();
            Material em = l.get().material();
            if (em.minTemperature().value() > 0 && (heat < em.minTemperature().value() || heat > em.maxTemperature().value()))
                return Optional.empty();
            more.add(new Stock.Extra(slots.get(i).name(), j, l.get(), share, heat, mb));
            mainNeed -= share;
        }
        if (mainNeed < ModularTuning.NUGGET_MB)
            return Optional.empty();
        Optional<Stock> main = checkMain(station, option, contents, materials, level, exact, mainNeed);
        return main.map(m -> more.isEmpty() ? m : new Stock(m.what(), m.cells(), m.spent(), m.coldest(), m.mb(), more));
    }

    private static Optional<Stock> checkMain(Station station, Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level,
                                             boolean exact, int need) {
        if (option.forged() && !station.forges())
            return Optional.empty();
        Material.Located what = null;
        int mb = 0;
        float coldest = Float.MAX_VALUE;
        for (ItemStack s : contents) {
            if (s.isEmpty())
                continue;
            if (s.has(FDataComponents.MADE_OF.get()))
                return Optional.empty();
            Optional<Material.Located> l = Material.find(materials, s.getItem());
            if (l.isEmpty() || (what != null && (!what.id().equals(l.get().id()) || !what.form().equals(l.get().form()))))
                return Optional.empty();
            what = l.get();
            mb += s.getCount() * FormItems.volume(l.get().form());
            coldest = Math.min(coldest, ItemHeat.get(s, level).value());
        }
        if (what == null || !takes(station, option, what) || (exact ? mb != need : mb < need))
            return Optional.empty();
        Material m = what.material();
        if (m.minTemperature().value() > 0 && (coldest < m.minTemperature().value() || coldest > m.maxTemperature().value()))
            return Optional.empty();
        if (!option.shaping().slots().isEmpty() && !option.shaping().slots().get(0).traits().isEmpty()
                && !what.material().traits().containsAll(option.shaping().slots().get(0).traits()))
            return Optional.empty();
        return Optional.of(new Stock(what, option.cells(), need, coldest, mb));
    }

    /** The part {@code stock} makes in {@code option}, with {@code flaw} flaws, at the stock's heat; empty if the part item is gone. */
    public static Optional<ItemStack> part(Option option, Stock stock, int flaw, Level level) {
        return part(option, stock, flaw, 1f, level);
    }

    /**
     * As {@link #part(Option, Stock, int, Level)}, and with the work the forge did against the work it asked
     * ({@link ForgeRhythm.Work#ratio}) kept on the part, which the bake reads as thickness and mass.
     * Exactly 1 leaves nothing on it, so a part made without a forge is the drawing.
     */
    public static Optional<ItemStack> part(Option option, Stock stock, int flaw, float work, Level level) {
        return part(option, stock, flaw, work, Map.of(), level);
    }

    /** As above, with the as-built {@code axes} ({@link #asBuilt}) on the part. */
    public static Optional<ItemStack> part(Option option, Stock stock, int flaw, float work, Map<String, Float> axes, Level level) {
        return BuiltInRegistries.ITEM.getOptional(option.shaping().item()).map(item -> {
            ItemStack out = new ItemStack(item);
            MadeOf made = new MadeOf(stock.what().id(), stock.what().form(), stock.cells(), option.variant().name(), flaw);
            if (work != 1f)
                made = made.withMeasures(made.measures().withWork(work));
            if (!axes.isEmpty())
                made = made.withMeasures(made.measures().withAxes(axes));
            if (!stock.more().isEmpty()) {
                Map<String, io.github.soundgoodizerfan.feedback.process.Measures.Slot> slots = new java.util.HashMap<>();
                for (Stock.Extra e : stock.more())
                    if (!e.what().id().equals(stock.what().id()))
                        slots.put(e.slot(), new io.github.soundgoodizerfan.feedback.process.Measures.Slot(e.what().id(), e.what().form()));
                made = made.withMeasures(made.measures().withSlots(slots));
            }
            out.set(FDataComponents.MADE_OF.get(), made);
            if (stock.coldest() > 0 && stock.what().material().minTemperature().value() > 0)
                ItemHeat.set(out, new Tu(stock.coldest()), level);
            return out;
        });
    }

    /**
     * A food shape ({@link ModularType#shapes}): the stock reshaped, one food per piece, so volume and the food's state
     * (its {@code FoodState}, knead, heat) carry over -- dough rolled thin is the same dough, not a fresh one.
     */
    private static ItemStack food(Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level) {
        if (check(Station.TOOL, option, contents, materials, level, true, 1f).isEmpty())
            return ItemStack.EMPTY;
        // shortcut: the first piece's state stands for the pile; pieces of differing age would want the worst kept.
        ItemStack first = contents.stream().filter(s -> !s.isEmpty()).findFirst().orElseThrow();
        int count = contents.stream().mapToInt(ItemStack::getCount).sum();
        ItemStack out = first.transmuteCopy(BuiltInRegistries.ITEM.get(option.shaping().item()), count);
        // A named shape (loaf, flat, roll) rides the dough as a choice, not as another item: the dish reads it as the part's variant.
        String shape = option.variant().name().isEmpty() ? option.shaping().base() : option.variant().name();
        if (shape.isEmpty())
            out.remove(FDataComponents.FOOD_SHAPE.get());
        else
            out.set(FDataComponents.FOOD_SHAPE.get(), shape);
        return out;
    }

    /** The part {@code contents} makes in a shape that is not forged: no flaws, out as soon as the stock is in. */
    public static ItemStack make(Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level) {
        return make(Station.TOOL, option, contents, materials, level);
    }

    public static ItemStack make(Station station, Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level) {
        return make(station, option, contents, materials, level, 1f, Map.of());
    }

    /** As above, for a part aimed at axes: stock for {@code scale}, the part carrying the as-built {@code axes}. */
    public static ItemStack make(Station station, Option option, List<ItemStack> contents, Map<ResourceLocation, Material> materials, Level level,
                                 float scale, Map<String, Float> axes) {
        return make(station, option, contents, List.of(), materials, level, scale, axes);
    }

    /** As above for a part of several slots: {@code extras} are the piles of the slots after the first ({@link #check}). */
    public static ItemStack make(Station station, Option option, List<ItemStack> contents, List<ItemStack> extras, Map<ResourceLocation, Material> materials,
                                 Level level, float scale, Map<String, Float> axes) {
        if (option.forged())
            return ItemStack.EMPTY;
        if (!(BuiltInRegistries.ITEM.get(option.shaping().item()) instanceof ModularPartItem))
            return food(option, contents, materials, level);
        return check(station, option, contents, extras, materials, level, scale).flatMap(s -> part(option, s, 0, 1f, axes, level).map(p -> {
                    // A coat rolled onto the plate (roll cladding) rides onto the part.
                    contents.stream().map(c -> c.get(FDataComponents.PLATING.get())).filter(java.util.Objects::nonNull).findFirst()
                            .ifPresent(c -> p.set(FDataComponents.PLATING.get(), c));
                    return finished(option, s, p, materials, level);
                }))
                .orElse(ItemStack.EMPTY);
    }

    /**
     * What the bench hands over for {@code part}: the part itself, except a piece of armour's plate, which is the armour, and
     * a vessel's vial body, which is the XP container, and a glove's body, which is the glove -- a tool whose design holds that one part ({@code ['P']} is one of its
     * patterns), finished as it comes out. Lining and studs are
     * fitted to it afterwards ({@link ModularRework}). Heat and flaws ride over from the part.
     */
    public static ItemStack finished(Option option, Stock stock, ItemStack part, Map<ResourceLocation, Material> materials, Level level) {
        ModularType type = ModularTypes.on(level).get(option.type());
        if (type == null || (type.family() != ModularType.Family.ARMOUR && type.family() != ModularType.Family.VESSEL
                && type.family() != ModularType.Family.GLOVE)
                || !option.shaping().item().equals(
                type.roles().get(type.primary()).part().map(ModularType.Shaping::item).orElse(null)))
            return part;
        ModularDesign design = new ModularDesign(option.type(),
                List.of(ModularAssembly.part(type.primary(), stock.what(), part, 1)), Edge.STANDARD);
        ItemStack tool = ModularStacks.blank(option.type()).orElse(ItemStack.EMPTY);
        if (tool.isEmpty())
            return part;
        if (part.has(FDataComponents.PLATING.get()))
            tool.set(FDataComponents.PLATING.get(), part.get(FDataComponents.PLATING.get()));
        if (!ModularStacks.assemble(tool, design, materials, ModularTypes.on(level)))
            return part;
        if (stock.coldest() > 0 && stock.what().material().minTemperature().value() > 0)
            ItemHeat.set(tool, new Tu(stock.coldest()), level);
        return tool;
    }

    /** Forms leftover stock comes back as, largest first: an ingot of metal or stone, then a nugget. */
    private static final List<String> CHANGE = List.of("ingot", "nugget");

    /**
     * {@code mb} of {@code what} as items smaller than the form it came in: what the bench hands back when a shape spends
     * part of a block. Largest form first, so a block less one part comes back as ingots.
     * A remainder no form of the material holds is lost.
     */
    public static List<ItemStack> change(Material.Located what, int mb) {
        List<ItemStack> out = new ArrayList<>();
        int own = FormItems.volume(what.form());
        for (String form : CHANGE) {
            int v = FormItems.volume(form);
            Optional<net.minecraft.world.item.Item> item = what.material().item(what.id(), form);
            // Only smaller than the stock's own form: a leftover is never the thing that was put in.
            if (mb < v || v >= own || item.isEmpty())
                continue;
            int n = mb / v;
            mb -= n * v;
            for (int max = item.get().getDefaultMaxStackSize(); n > 0; n -= Math.min(n, max))
                out.add(new ItemStack(item.get(), Math.min(n, max)));
        }
        return out;
    }
}
