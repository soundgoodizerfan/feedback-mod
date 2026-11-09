// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.data;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;

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
        /** Short and lossy. */
        POOR,
        /** Long and exact, but quenches under load. */
        GOOD
    }

    public static final TagKey<Item> POOR_WIRE = TagKey.create(Registries.ITEM, Feedback.id("data_wire_poor"));
    public static final TagKey<Item> GOOD_WIRE = TagKey.create(Registries.ITEM, Feedback.id("data_wire_good"));

    private static final int MAX_HOPS = 16;

    /** Quench state is server memory: a line that cooled while the world was off is cool. */
    private static final Map<String, Long> QUENCHED_UNTIL = new HashMap<>();

    private DataWires() {
    }

    public static int range(Grade grade) {
        return grade == Grade.GOOD ? FTuning.WIRE_RANGE_GOOD : FTuning.WIRE_RANGE_POOR;
    }

    private static TagKey<Item> tag(Grade grade) {
        return grade == Grade.GOOD ? GOOD_WIRE : POOR_WIRE;
    }

    /** Result of paying for a link: the grade used, or why not. */
    public record Paid(Grade grade, boolean enough, boolean inRange, int pieces) {
    }

    /**
     * Takes wire for a link {@code blocks} long from {@code player}: the poor grade when it reaches and is enough,
     * else the good. Creative players pay nothing and get the good grade. Takes nothing when it fails.
     */
    public static Paid pay(Player player, int blocks) {
        int pieces = Math.max(1, blocks);
        if (player.getAbilities().instabuild)
            return new Paid(Grade.GOOD, true, true, pieces);
        boolean anyInRange = false;
        for (Grade grade : Grade.values()) {
            if (pieces > range(grade))
                continue;
            anyInRange = true;
            if (count(player, grade) >= pieces) {
                take(player, grade, pieces);
                return new Paid(grade, true, true, pieces);
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
        if (level.getServer() != null)
            Book.get(level.getServer()).put(key(level, a, b), grade);
    }

    /** A link with no recorded wire counts as the poor grade: nothing joins for free. */
    private static Grade gradeOf(Level level, DataNodeRef a, DataNodeRef b) {
        if (level.getServer() == null)
            return Grade.POOR;
        return Book.get(level.getServer()).get(key(level, a, b));
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
        best.put(start, Grade.GOOD);
        frontier.add(start);
        seen.add(start);
        Grade found = null;
        for (int hop = 0; hop < MAX_HOPS && !frontier.isEmpty() && found != Grade.GOOD; hop++) {
            ArrayDeque<DataNodeRef> next = new ArrayDeque<>();
            for (DataNodeRef at : frontier) {
                DataNode node = DataNode.resolveOrNull(level, at);
                if (node == null)
                    continue;
                for (DataNodeRef other : Set.copyOf(node.getNodeLinks())) {
                    Grade wire = gradeOf(level, at, other);
                    if (wire == Grade.GOOD && quenched(level, node, other, now))
                        continue;
                    Grade here = worse(best.get(at), wire);
                    if (other.equals(target))
                        found = found == null ? here : better(found, here);
                    if (seen.add(other)) {
                        best.put(other, here);
                        next.add(other);
                    }
                }
            }
            frontier = next;
        }
        return Optional.ofNullable(found);
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
        int load = Math.max(node.getNodeLinks().size(), far == null ? 0 : far.getNodeLinks().size());
        if (load > FTuning.WIRE_CRITICAL_LINKS) {
            QUENCHED_UNTIL.put(k, now + FTuning.WIRE_QUENCH_TICKS);
            return true;
        }
        return false;
    }

    private static Grade worse(Grade a, Grade b) {
        return a == Grade.POOR || b == Grade.POOR ? Grade.POOR : Grade.GOOD;
    }

    private static Grade better(Grade a, Grade b) {
        return a == Grade.GOOD || b == Grade.GOOD ? Grade.GOOD : Grade.POOR;
    }

    /** A reading as the wire delivers it: exact on good wire, rounded to one significant figure on poor. */
    public static float deliver(float reading, Grade grade) {
        if (grade == Grade.GOOD || reading == 0f || Float.isNaN(reading) || Float.isInfinite(reading))
            return reading;
        double step = Math.pow(10, Math.floor(Math.log10(Math.abs(reading))));
        return (float) (Math.round(reading / step) * step);
    }

    /** Which wire joins which pair, saved in the overworld's data. */
    private static final class Book extends SavedData {
        private final Map<String, Grade> wires = new HashMap<>();

        static Book get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(Book::new, Book::load), "feedback_data_wires");
        }

        void put(String key, Grade grade) {
            if (wires.put(key, grade) != grade)
                setDirty();
        }

        Grade get(String key) {
            return wires.getOrDefault(key, Grade.POOR);
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            CompoundTag all = new CompoundTag();
            wires.forEach((k, g) -> all.putBoolean(k, g == Grade.GOOD));
            tag.put("Wires", all);
            return tag;
        }

        static Book load(CompoundTag tag, HolderLookup.Provider registries) {
            Book book = new Book();
            CompoundTag all = tag.getCompound("Wires");
            for (String k : all.getAllKeys())
                book.wires.put(k, all.getBoolean(k) ? Grade.GOOD : Grade.POOR);
            return book;
        }
    }
}
