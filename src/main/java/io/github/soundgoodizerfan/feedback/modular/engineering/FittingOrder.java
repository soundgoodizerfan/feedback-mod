// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;

/**
 * The fitting minigame's pure model, shared by the screen (which draws it) and any test: which order the parts of an assembly seat in, and how
 * a play scores. The order is derived from physical dependency, never written per recipe: a part that sits inside or under another goes first.
 *
 * <p>From a type's roles, in tiers: the <b>spine</b> (a rod: handle, haft, shaft) first, everything else mounts to it; then the parts placed
 * along it, the biggest first and, of equal size, the one further from the hand first (a blade before its guard before its pommel); then the
 * layered parts with no placement, by sprite layer, which is the order they stack from the bottom up (a liner under a cover seats before it);
 * then the small fixings placed on a part (a binding, a pivot pin), which lock what is already seated; last the hard pieces set into one
 * (a tip). A type's {@link ModularType#fitOrder} names letters that seat before all of that, for a type the derivation orders badly.
 * Parts of one role, and roles the derivation cannot tell apart, share a rank and seat in any order.
 *
 * <p>A wrong click is a mis-seat: it costs score and nothing else. The server never sees the order; it receives the score.
 */
public final class FittingOrder {

    /** Grade words, by {@link #grade}: botched, sloppy, fair, clean, flawless. */
    public static final int GRADES = 5;

    private FittingOrder() {
    }

    /**
     * The seating rank of each piece, whose role letter is {@code roles[i]}: 0 seats first, equal ranks seat in either order.
     * A letter {@code type} does not know is ranked after all others.
     */
    public static int[] ranks(ModularType type, String[] roles) {
        float maxLitres = 0f;
        for (ModularType.Role r : type.roles().values())
            if (!r.set())
                maxLitres = Math.max(maxLitres, r.litres());
        Map<String, float[]> keys = new HashMap<>();
        for (String letter : roles) {
            if (letter == null || keys.containsKey(letter))
                continue;
            keys.put(letter, key(type, letter, maxLitres));
        }
        Integer[] idx = new Integer[roles.length];
        for (int i = 0; i < idx.length; i++)
            idx[i] = i;
        Comparator<Integer> byKey = (a, b) -> compare(keys.get(roles[a]), keys.get(roles[b]));
        java.util.Arrays.sort(idx, byKey);
        int[] out = new int[roles.length];
        int rank = 0;
        for (int i = 0; i < idx.length; i++) {
            if (i > 0 && byKey.compare(idx[i - 1], idx[i]) != 0)
                rank++;
            out[idx[i]] = rank;
        }
        return out;
    }

    private static int compare(float[] a, float[] b) {
        if (a == null || b == null)
            return a == b ? 0 : a == null ? 1 : -1;
        for (int i = 0; i < a.length; i++) {
            int c = Float.compare(a[i], b[i]);
            if (c != 0)
                return c;
        }
        return 0;
    }

    /** The sort key of a role: tier, then two size and placement figures, then the layer. */
    private static float[] key(ModularType type, String letter, float maxLitres) {
        ModularType.Role r = type.roles().get(letter);
        if (r == null)
            return new float[] {99f, 0f, 0f, 0f};
        int listed = type.fitOrder().indexOf(letter);
        if (listed >= 0)
            return new float[] {-1f, listed, 0f, 0f};
        boolean placed = r.at() != 0f || r.rides();
        if (r.set())
            return new float[] {5f, 0f, 0f, r.layer()};
        if (r.rod())
            return new float[] {0f, 0f, 0f, r.layer()};
        if (placed && r.litres() <= maxLitres * 0.1f)
            return new float[] {4f, 0f, 0f, r.layer()};
        if (placed)
            return new float[] {1f, -r.litres(), -r.at(), r.layer()};
        return new float[] {2f, r.layer(), 0f, 0f};
    }

    /**
     * The ranks for pieces that are plain items (a recipe's result is not modular): those whose id is in {@code order} seat in that order,
     * the rest after them in the order given (the grid's reading order).
     */
    public static int[] ranksByItem(String[] ids, List<String> order) {
        int[] out = new int[ids.length];
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < ids.length; i++)
            idx.add(i);
        idx.sort(Comparator.comparingInt((Integer i) -> {
            int at = order.indexOf(ids[i]);
            return at < 0 ? order.size() + i : at;
        }));
        for (int r = 0; r < idx.size(); r++)
            out[idx.get(r)] = r;
        return out;
    }

    /** Whether seating {@code piece} now is right: no piece of a lower rank is still unseated. */
    public static boolean fits(int[] ranks, boolean[] seated, int piece) {
        for (int i = 0; i < ranks.length; i++)
            if (!seated[i] && ranks[i] < ranks[piece])
                return false;
        return !seated[piece];
    }

    /** The score of a play of {@code pieces} pieces with {@code misseats} wrong clicks, 0..1: 1 for a clean play, falling with each slip. */
    public static float score(int pieces, int misseats) {
        if (pieces <= 0)
            return 1f;
        return pieces / (pieces + ModularTuning.FIT_MISSEAT_WEIGHT * Math.max(0, misseats));
    }

    /** The word for a score, 0 (botched) to 4 (flawless). */
    public static int grade(float score) {
        return score < 0.3f ? 0 : score < 0.55f ? 1 : score < 0.78f ? 2 : score < 0.99f ? 3 : 4;
    }

    /** The piece list the server hands the client: {@code slot:rank} pairs, comma separated, in grid slot order. */
    public static String encode(int[] slots, int[] ranks) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < slots.length; i++) {
            if (i > 0)
                b.append(',');
            b.append(slots[i]).append(':').append(ranks[i]);
        }
        return b.toString();
    }

    /** The pairs of {@link #encode}, as {@code [slot, rank]} rows; malformed or out-of-range entries are dropped. */
    public static int[][] decode(String data) {
        List<int[]> rows = new ArrayList<>();
        for (String pair : data.split(",")) {
            String[] p = pair.split(":");
            if (p.length != 2)
                continue;
            try {
                int slot = Integer.parseInt(p[0].trim()), rank = Integer.parseInt(p[1].trim());
                if (slot >= 0 && slot < EngineeringInput.GRID && rank >= 0 && rows.size() < ModularTuning.FIT_MAX_PIECES)
                    rows.add(new int[] {slot, rank});
            } catch (NumberFormatException ignored) {
            }
        }
        return rows.toArray(new int[0][]);
    }
}
