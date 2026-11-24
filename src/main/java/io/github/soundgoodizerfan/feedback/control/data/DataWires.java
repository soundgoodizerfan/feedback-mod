// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.data;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * What a data link is made of. A link costs wire by length (one piece per block), and the wire's grade sets how far it
 * reaches and how true a reading is that comes down it: copper is poor (short, and the reading is rounded to one figure),
 * redstone-chromium is good (long, exact) until its line carries more links than its critical current allows, when it
 * quenches dark and recovers after it has cooled, undamaged. A node reads another only through a path of links; the
 * path's grade is its worst wire.
 */
public final class DataWires {

    public enum Grade {
        /** Short and lossy: a reading rounds to one figure. */
        POOR,
        /** A fibre run past its clean reach: a reading rounds to two figures. Never a wire grade; only a fibre degrades to it. */
        COARSE,
        /** Long and exact, but quenches under load. */
        GOOD,
        /** A fibre inside its clean reach: exact and immune to the critical current. Always between two {@link OpticalEnd}s. */
        FIBRE
    }

    /** Orders grades from worst to best; the path's grade is its worst link. */
    private static int rank(Grade grade) {
        return grade.ordinal();
    }

    public static final TagKey<Item> POOR_WIRE = TagKey.create(Registries.ITEM, Feedback.id("data_wire_poor"));
    public static final TagKey<Item> GOOD_WIRE = TagKey.create(Registries.ITEM, Feedback.id("data_wire_good"));

    private static final int MAX_HOPS = 16;

    /** Quench state is server memory: a line that cooled while the world was off is cool. */
    private static final Map<String, Long> QUENCHED_UNTIL = new HashMap<>();

    private DataWires() {
    }

    public static int range(Grade grade) {
        return grade == Grade.GOOD ? ControlTuning.WIRE_RANGE_GOOD : ControlTuning.WIRE_RANGE_POOR;
    }

    /** The reach of a wire of {@code grade} between ends whose amplifiers give {@code gain} (the larger of the two): the base range lengthened by the gain. */
    public static int range(Grade grade, float gain) {
        return (int) (range(grade) * (1f + io.github.soundgoodizerfan.feedback.process.ProcessTuning.CIRCUIT_WIRE_RANGE_GAIN * Math.max(0f, gain)));
    }

    /** The gain a node's circuit adds to a link that ends at it, 0 for a node that amplifies nothing. */
    public static float gainOf(DataNode node) {
        return node instanceof Amplifying a ? a.amplifierGain() : 0f;
    }

    private static TagKey<Item> tag(Grade grade) {
        return grade == Grade.GOOD ? GOOD_WIRE : POOR_WIRE;
    }

    /** Result of paying for a link: the grade used, or why not. */
    public record Paid(Grade grade, boolean enough, boolean inRange, int pieces, int critical) {
        public Paid(Grade grade, boolean enough, boolean inRange, int pieces) {
            this(grade, enough, inRange, pieces, ControlTuning.WIRE_CRITICAL_LINKS);
        }
    }

    /**
     * Takes wire for a link {@code blocks} long from {@code player}: the poor grade when it reaches and is enough,
     * else the good. Creative players pay nothing and get the good grade. Takes nothing when it fails.
     */
    public static Paid pay(Player player, int blocks) {
        return pay(player, blocks, 0f);
    }

    /** As above for a link whose ends' amplifiers give {@code gain}: the wire reaches further ({@link #range(Grade, float)}); the pieces are still one a block. */
    public static Paid pay(Player player, int blocks, float gain) {
        int pieces = Math.max(1, blocks);
        if (player.getAbilities().instabuild)
            return new Paid(Grade.GOOD, true, true, pieces);
        boolean anyInRange = false;
        for (Grade grade : Grade.values()) {
            if (pieces > range(grade, gain))
                continue;
            anyInRange = true;
            if (count(player, grade) >= pieces) {
                int critical = criticalOf(player, grade);
                take(player, grade, pieces);
                return new Paid(grade, true, true, pieces, critical);
            }
        }
        return new Paid(null, false, anyInRange, pieces);
    }

    private static int count(Player player, Grade grade) {
        int n = 0;
        for (ItemStack stack : player.getInventory().items)
            if (stack.is(tag(grade)))
                n += stack.getCount();
        return n;
    }

    /**
     * Links per end a wire of this grade carries before its line quenches: from the material of the first wire held, its ampacity (the critical
     * current of the alloy) times {@link ControlTuning#WIRE_LINKS_PER_AMPACITY}, scaled by the volume of its form against a plain wire's (a thicker wire
     * carries more). A wire that states no ampacity carries the default.
     */
    private static int criticalOf(Player player, Grade grade) {
        for (ItemStack stack : player.getInventory().items)
            if (stack.is(tag(grade))) {
                var found = io.github.soundgoodizerfan.feedback.process.Material.find(io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials(), stack.getItem());
                if (found.isPresent() && found.get().material().ampacity().orElse(0f) > 0f) {
                    float thick = io.github.soundgoodizerfan.feedback.registry.FormItems.volume(found.get().form())
                            / (float) Math.max(1, io.github.soundgoodizerfan.feedback.registry.FormItems.volume("wire"));
                    return Math.max(1, Math.round(found.get().material().ampacity().orElse(0f) * ControlTuning.WIRE_LINKS_PER_AMPACITY * thick));
                }
                break;
            }
        return ControlTuning.WIRE_CRITICAL_LINKS;
    }

    private static void take(Player player, Grade grade, int pieces) {
        for (ItemStack stack : player.getInventory().items) {
            if (pieces <= 0)
                return;
            if (stack.is(tag(grade))) {
                int used = Math.min(pieces, stack.getCount());
                stack.shrink(used);
                pieces -= used;
            }
        }
    }

    // --- which wire joins which pair ---------------------------------------------------------------------------

    private static String key(Level level, DataNodeRef a, DataNodeRef b) {
        String x = a.toString(), y = b.toString();
        return level.dimension().location() + "|" + (x.compareTo(y) <= 0 ? x + "|" + y : y + "|" + x);
    }

    /** Remembers the grade of the wire that now joins {@code a} and {@code b}. */
    public static void record(Level level, DataNodeRef a, DataNodeRef b, Grade grade) {
        record(level, a, b, grade, ControlTuning.WIRE_CRITICAL_LINKS);
    }

    /** As above with the links per end the wire carries ({@link #criticalOf}). */
    public static void record(Level level, DataNodeRef a, DataNodeRef b, Grade grade, int critical) {
        if (level.getServer() != null)
            Book.get(level.getServer()).put(key(level, a, b), new Wire(grade, 0f, critical));
    }

    /**
     * What a link carries now. A link with no recorded wire counts as the poor grade: nothing joins for free. A fibre is judged by its ends as
     * they are this moment (power, crystal, circuit): a dark end, or a loss past eight times the clean reach, gives {@code null}, a link that carries nothing.
     */
    static Grade gradeOf(Level level, DataNodeRef a, DataNodeRef b) {
        if (level.getServer() == null)
            return Grade.POOR;
        Wire wire = Book.get(level.getServer()).get(key(level, a, b));
        if (wire.grade() != Grade.FIBRE)
            return wire.grade();
        if (!(DataNode.resolveOrNull(level, a) instanceof OpticalEnd ea) || !(DataNode.resolveOrNull(level, b) instanceof OpticalEnd eb)
                || !ea.lit() || !eb.lit())
            return null;
        return fibreGrade(wire.lossDb(), budgetDb(ea, eb));
    }

    // --- reading through a path -----------------------------------------------------------------------------------

    /**
     * The worst grade on the best path of links from {@code from} to {@code to}, empty when no path reaches (or every one
     * is quenched). A good line whose far or near end carries more links than the critical current quenches and stays dark
     * until it has cooled.
     */
    public static Optional<Grade> reach(DataNode from, DataNode to) {
        Level level = from.getNodeLevel();
        if (level == null || level.getServer() == null)
            return Optional.empty();
        long now = level.getGameTime();
        DataNodeRef target = to.getNodeRef();
        Map<DataNodeRef, Grade> best = new HashMap<>();
        ArrayDeque<DataNodeRef> frontier = new ArrayDeque<>();
        Set<DataNodeRef> seen = new HashSet<>();
        DataNodeRef start = from.getNodeRef();
        best.put(start, Grade.FIBRE);
        frontier.add(start);
        seen.add(start);
        Grade found = null;
        // The pair crossing the path to each node used (if any), so a path that crosses is charged once when it is the one found.
        Map<DataNodeRef, io.github.soundgoodizerfan.feedback.control.endernet.Endernet.Crossing> via = new HashMap<>();
        io.github.soundgoodizerfan.feedback.control.endernet.Endernet.Crossing foundVia = null;
        for (int hop = 0; hop < MAX_HOPS && !frontier.isEmpty() && (found == null || rank(found) < rank(Grade.GOOD)); hop++) {
            ArrayDeque<DataNodeRef> next = new ArrayDeque<>();
            for (DataNodeRef at : frontier) {
                DataNode node = DataNode.resolveOrNull(level, at);
                if (node == null)
                    continue;
                // A live endernet pair is one more hop, of the best grade (control/endernet); it is paid for once, if the path found uses it.
                Set<DataNodeRef> outs = new HashSet<>(node.getNodeLinks());
                var crossing = io.github.soundgoodizerfan.feedback.control.endernet.Endernet.peek(node);
                DataNodeRef crossRef = crossing == null ? null : crossing.to().getNodeRef();
                if (crossRef != null)
                    outs.add(crossRef);
                for (DataNodeRef other : outs) {
                    boolean cross = other.equals(crossRef);
                    Grade wire = cross ? Grade.GOOD : gradeOf(level, at, other);
                    if (wire == null)
                        continue;
                    if (wire == Grade.GOOD && !cross && quenched(level, node, other, now))
                        continue;
                    Grade here = worse(best.get(at), wire);
                    var through = cross ? crossing : via.get(at);
                    if (other.equals(target)) {
                        Grade was = found;
                        found = found == null ? here : better(found, here);
                        if (was == null || found != was)
                            foundVia = through;
                    }
                    if (seen.add(other)) {
                        best.put(other, here);
                        if (through != null)
                            via.put(other, through);
                        next.add(other);
                    }
                }
            }
            frontier = next;
        }
        if (found != null && foundVia != null && !foundVia.pay())
            return Optional.empty();
        return Optional.ofNullable(found);
    }

    /**
     * Every node a path of links (or a live Endernet pair) leads to from {@code from}, nearest first, without paying for any
     * crossing: the set a Controller binds. {@link #reach} still decides, per reading, whether a path carries.
     */
    public static java.util.List<DataNode> reachable(DataNode from) {
        Level level = from.getNodeLevel();
        java.util.List<DataNode> found = new java.util.ArrayList<>();
        if (level == null)
            return found;
        Set<DataNodeRef> seen = new HashSet<>();
        ArrayDeque<DataNodeRef> frontier = new ArrayDeque<>();
        seen.add(from.getNodeRef());
        frontier.add(from.getNodeRef());
        for (int hop = 0; hop < MAX_HOPS && !frontier.isEmpty(); hop++) {
            ArrayDeque<DataNodeRef> next = new ArrayDeque<>();
            for (DataNodeRef at : frontier) {
                DataNode node = DataNode.resolveOrNull(level, at);
                if (node == null)
                    continue;
                Set<DataNodeRef> outs = new HashSet<>(node.getNodeLinks());
                var crossing = io.github.soundgoodizerfan.feedback.control.endernet.Endernet.peek(node);
                if (crossing != null)
                    outs.add(crossing.to().getNodeRef());
                for (DataNodeRef other : outs)
                    if (seen.add(other)) {
                        DataNode far = DataNode.resolveOrNull(level, other);
                        if (far != null)
                            found.add(far);
                        next.add(other);
                    }
            }
            frontier = next;
        }
        return found;
    }

    private static boolean quenched(Level level, DataNode node, DataNodeRef other, long now) {
        String k = key(level, node.getNodeRef(), other);
        Long until = QUENCHED_UNTIL.get(k);
        if (until != null) {
            if (now < until)
                return true;
            QUENCHED_UNTIL.remove(k);
        }
        DataNode far = DataNode.resolveOrNull(level, other);
        // An end quenches its line when it is carrying all it can: more than the critical count, and at its own cap (a Controller takes more).
        int critical = Book.get(level.getServer()).get(k).critical();
        boolean overloaded = node.getNodeLinks().size() > Math.max(critical, node.getMaxNodeLinks() - 1)
                || far != null && far.getNodeLinks().size() > Math.max(critical, far.getMaxNodeLinks() - 1);
        if (overloaded) {
            QUENCHED_UNTIL.put(k, now + ControlTuning.WIRE_QUENCH_TICKS);
            return true;
        }
        return false;
    }

    private static Grade worse(Grade a, Grade b) {
        return rank(a) <= rank(b) ? a : b;
    }

    private static Grade better(Grade a, Grade b) {
        return rank(a) >= rank(b) ? a : b;
    }

    /** A reading as the wire delivers it: exact on good wire and fibre, two significant figures on a coarse fibre, one on poor. */
    public static float deliver(float reading, Grade grade) {
        if (grade == Grade.GOOD || grade == Grade.FIBRE || reading == 0f || Float.isNaN(reading) || Float.isInfinite(reading))
            return reading;
        double step = Math.pow(10, Math.floor(Math.log10(Math.abs(reading))) - (grade == Grade.COARSE ? 1 : 0));
        return (float) (Math.round(reading / step) * step);
    }

    // --- fibre ----------------------------------------------------------------------------------------------------

    /** The fibre of a glass or polymer item: its loss in dB per metre, or empty when the item is no fibre or its material carries no light. */
    public static java.util.Optional<Float> attenuationOf(ItemStack stack) {
        return io.github.soundgoodizerfan.feedback.process.Material.find(
                        io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials(), stack.getItem())
                .filter(l -> l.form().equals("fibre"))
                .flatMap(l -> l.material().attenuation());
    }

    /** The loss budget in dB of a fibre between two ends: the base budget scaled by the weaker crystal and lengthened by the larger circuit gain. */
    public static float budgetDb(OpticalEnd a, OpticalEnd b) {
        float gain = Math.max(0f, Math.max(a.gain(), b.gain()));
        return ControlTuning.FIBRE_LOSS_BUDGET_DB * Math.min(a.budgetShare(), b.budgetShare())
                * (1f + io.github.soundgoodizerfan.feedback.process.ProcessTuning.CIRCUIT_WIRE_RANGE_GAIN * gain);
    }

    /** How far a link's loss is past what the budget allows, 1 being exactly the clean reach; the cap on a clean run shows as a smaller loss, never a bigger budget. */
    public static float lossRatio(float lossDb, float budgetDb) {
        return budgetDb <= 0f ? Float.POSITIVE_INFINITY : lossDb / budgetDb;
    }

    /** The grade a fibre of this loss gives over this budget: exact inside the reach, then two figures, then one, then lost (null). */
    public static Grade fibreGrade(float lossDb, float budgetDb) {
        float ratio = lossRatio(lossDb, budgetDb);
        if (ratio <= 1f)
            return Grade.FIBRE;
        if (ratio <= ControlTuning.FIBRE_ONE_FIGURE_RATIO)
            return Grade.COARSE;
        return ratio < ControlTuning.FIBRE_DEAD_RATIO ? Grade.POOR : null;
    }

    /** The loss in dB of {@code blocks} of a fibre of {@code attenuation} dB per metre, one block being one metre; a glass so clear its reach would pass the cap is held to the cap. */
    public static float lossDb(float attenuation, int blocks) {
        float floor = ControlTuning.FIBRE_LOSS_BUDGET_DB / ControlTuning.FIBRE_CAP_BLOCKS;
        return Math.max(attenuation, floor) * Math.max(1, blocks);
    }

    /** Result of paying for a fibre link: what was spent, or why not. */
    public record PaidFibre(boolean enough, float lossDb, int pieces, boolean dead) {
    }

    /**
     * Takes fibre for a link of {@code blocks} between ends whose loss budget is {@code budgetDb}: the poorest fibre carried that still keeps
     * the link clean, else the best carried (the link then degrades, and is refused only when even that is lost). Creative pays nothing and
     * gets the clearest glass in the registry (a perfect fibre). Takes nothing when it fails.
     */
    public static PaidFibre payFibre(Player player, int blocks, float budgetDb) {
        int pieces = Math.max(1, blocks);
        if (player.getAbilities().instabuild)
            return new PaidFibre(true, lossDb(0f, blocks), pieces, false);
        ItemStack poorestClean = null, best = null;
        float cleanLoss = 0f, bestLoss = Float.MAX_VALUE;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty() || stack.getCount() < pieces)
                continue;
            var att = attenuationOf(stack);
            if (att.isEmpty())
                continue;
            float loss = lossDb(att.get(), blocks);
            if (loss <= budgetDb && (poorestClean == null || loss > cleanLoss)) {
                poorestClean = stack;
                cleanLoss = loss;
            }
            if (loss < bestLoss) {
                best = stack;
                bestLoss = loss;
            }
        }
        ItemStack use = poorestClean != null ? poorestClean : best;
        if (use == null)
            return new PaidFibre(false, 0f, pieces, false);
        float loss = poorestClean != null ? cleanLoss : bestLoss;
        if (fibreGrade(loss, budgetDb) == null)
            return new PaidFibre(false, loss, pieces, true);
        use.shrink(pieces);
        return new PaidFibre(true, loss, pieces, false);
    }

    /** Remembers the fibre that now joins {@code a} and {@code b}: its total loss in dB. */
    public static void recordFibre(Level level, DataNodeRef a, DataNodeRef b, float lossDb) {
        if (level.getServer() != null)
            Book.get(level.getServer()).put(key(level, a, b), new Wire(Grade.FIBRE, lossDb, ControlTuning.WIRE_CRITICAL_LINKS));
    }

    /** The loss in dB of the fibre between two nodes, or empty when the link is not a fibre. */
    public static java.util.Optional<Float> fibreLoss(Level level, DataNodeRef a, DataNodeRef b) {
        if (level.getServer() == null)
            return java.util.Optional.empty();
        Wire w = Book.get(level.getServer()).wires.get(key(level, a, b));
        return w != null && w.grade() == Grade.FIBRE ? java.util.Optional.of(w.lossDb()) : java.util.Optional.empty();
    }

    /** What a wire is: its grade, and for a fibre its total loss in dB. */
    record Wire(Grade grade, float lossDb, int critical) {
    }

    /** The recorded wire between two nodes as it was made (a fibre keeps its loss), for carrying a link to a new position. */
    static Wire wireOf(Level level, DataNodeRef a, DataNodeRef b) {
        return level.getServer() == null ? new Wire(Grade.POOR, 0f, ControlTuning.WIRE_CRITICAL_LINKS) : Book.get(level.getServer()).get(key(level, a, b));
    }

    static void recordWire(Level level, DataNodeRef a, DataNodeRef b, Wire wire) {
        if (level.getServer() != null)
            Book.get(level.getServer()).put(key(level, a, b), wire);
    }

    /** Which wire joins which pair, saved in the overworld's data. */
    private static final class Book extends SavedData {
        private final Map<String, Wire> wires = new HashMap<>();

        static Book get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(Book::new, Book::load), "feedback_data_wires");
        }

        void put(String key, Wire wire) {
            if (!wire.equals(wires.put(key, wire)))
                setDirty();
        }

        Wire get(String key) {
            return wires.getOrDefault(key, new Wire(Grade.POOR, 0f, ControlTuning.WIRE_CRITICAL_LINKS));
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            CompoundTag all = new CompoundTag();
            wires.forEach((k, w) -> {
                CompoundTag one = new CompoundTag();
                one.putString("Grade", w.grade().name());
                one.putFloat("Loss", w.lossDb());
                one.putInt("Critical", w.critical());
                all.put(k, one);
            });
            tag.put("Wires2", all);
            return tag;
        }

        static Book load(CompoundTag tag, HolderLookup.Provider registries) {
            Book book = new Book();
            CompoundTag all = tag.getCompound("Wires2");
            for (String k : all.getAllKeys()) {
                CompoundTag one = all.getCompound(k);
                try {
                    book.wires.put(k, new Wire(Grade.valueOf(one.getString("Grade")), one.getFloat("Loss"), one.contains("Critical") ? one.getInt("Critical") : ControlTuning.WIRE_CRITICAL_LINKS));
                } catch (IllegalArgumentException ignored) {
                    book.wires.put(k, new Wire(Grade.POOR, 0f, ControlTuning.WIRE_CRITICAL_LINKS));
                }
            }
            return book;
        }
    }
}
