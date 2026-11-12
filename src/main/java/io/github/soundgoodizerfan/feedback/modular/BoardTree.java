// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tidy tree layout for the blueprint board: every node gets its own cell on a grid, so no two nodes (or a node's label and
 * the next node) can overlap whatever the type. Pure arithmetic, no client classes, so a game test can run it over every type.
 */
public final class BoardTree {
    /** Cell size in canvas units: wide enough for a node and the plus slot beside it, tall enough for a node and its label. */
    public static final float CELL_W = 44, CELL_H = 44;
    /** Most cells across before a new tree starts a new band underneath. */
    private static final int MAX_COLS = 8;

    private BoardTree() {
    }

    /**
     * Place {@code n} nodes. Trees are grown from the edges (and from {@code hint}, a forced parent per node or -1, which is how an
     * empty slot hangs off the part it fits); children are ordered by their seed x so a layout keeps the picture's left and right.
     *
     * @return one {x, y} per node, in canvas units
     */
    public static float[][] place(int n, List<int[]> edges, int[] hint, float[] seedX, float[] seedY) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++)
            adj.add(new ArrayList<>());
        for (int[] e : edges)
            if (e[0] != e[1]) {
                adj.get(e[0]).add(e[1]);
                adj.get(e[1]).add(e[0]);
            }
        for (int i = 0; i < n; i++)
            if (hint[i] >= 0 && hint[i] != i) {
                adj.get(i).add(hint[i]);
                adj.get(hint[i]).add(i);
            }
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++)
            order[i] = i;
        java.util.Arrays.sort(order, Comparator.<Integer>comparingDouble(i -> seedY[i]).thenComparingDouble(i -> seedX[i]));
        boolean[] seen = new boolean[n];
        List<List<Integer>> kids = new ArrayList<>();
        for (int i = 0; i < n; i++)
            kids.add(new ArrayList<>());
        List<Integer> roots = new ArrayList<>();
        for (int r : order) {
            if (seen[r])
                continue;
            roots.add(r);
            seen[r] = true;
            List<Integer> queue = new ArrayList<>(List.of(r));
            for (int q = 0; q < queue.size(); q++) {
                int at = queue.get(q);
                for (int to : adj.get(at))
                    if (!seen[to]) {
                        seen[to] = true;
                        kids.get(at).add(to);
                        queue.add(to);
                    }
                kids.get(at).sort(Comparator.comparingDouble(i -> seedX[i]));
            }
        }
        float[][] out = new float[n][2];
        int[] width = new int[n];
        for (int r : roots)
            width(r, kids, width);
        int col = 0, band = 0, bandRows = 0;
        for (int r : roots) {
            if (col > 0 && col + width[r] > MAX_COLS) {
                band += bandRows;
                col = bandRows = 0;
            }
            bandRows = Math.max(bandRows, put(r, col, 0, band, kids, width, out));
            col += width[r];
        }
        return out;
    }

    private static int width(int node, List<List<Integer>> kids, int[] width) {
        int w = 0;
        for (int k : kids.get(node))
            w += width(k, kids, width);
        return width[node] = Math.max(1, w);
    }

    /** Puts a subtree in the columns {@code col..col+width}, its root over the middle of its children; returns the rows it uses. */
    private static int put(int node, int col, int depth, int band, List<List<Integer>> kids, int[] width, float[][] out) {
        int rows = 1, c = col;
        float first = 0, last = 0;
        List<Integer> ks = kids.get(node);
        for (int i = 0; i < ks.size(); i++) {
            int k = ks.get(i);
            rows = Math.max(rows, 1 + put(k, c, depth + 1, band, kids, width, out));
            if (i == 0)
                first = out[k][0];
            last = out[k][0];
            c += width[k];
        }
        out[node][0] = ks.isEmpty() ? (col + 0.5f) * CELL_W : (first + last) / 2;
        out[node][1] = (band + depth + 0.5f) * CELL_H;
        return rows;
    }

    /** Smallest gap between any two nodes' cells, as a count of cells apart in x or y: below 1 means an overlap. For tests. */
    public static float minSeparation(float[][] at) {
        float worst = Float.MAX_VALUE;
        for (int i = 0; i < at.length; i++)
            for (int j = i + 1; j < at.length; j++)
                worst = Math.min(worst, Math.max(Math.abs(at[i][0] - at[j][0]) / CELL_W, Math.abs(at[i][1] - at[j][1]) / CELL_H));
        return worst;
    }
}
