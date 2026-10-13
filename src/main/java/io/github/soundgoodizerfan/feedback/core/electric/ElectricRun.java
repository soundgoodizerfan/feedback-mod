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
package io.github.soundgoodizerfan.feedback.core.electric;

import java.util.Arrays;
import java.util.List;

import io.github.soundgoodizerfan.feedback.core.FTuning;

/**
 * One connected run of wire, sources and sinks, and the arithmetic that says who gets what.
 *
 * <h2>The shape of the work</h2>
 * {@link ElectricPropagator} floods the world once when the topology changes and hands this class
 * a member list and an adjacency list. This class then builds a <em>routing tree</em> -- every
 * reachable member's parent on its shortest path back to the supply -- and caches it. Each tick
 * after that is arithmetic over arrays: no search, no allocation, no world access (mechanics 6.2,
 * "per-packet routing evaluated per tick per wire block per sink would be the expensive way").
 * It is the {@code RotationPropagator} pattern: rebuild a whole run, never patch one.
 *
 * <h2>What a tick does</h2>
 * <ol>
 *   <li><b>Who is supplying.</b> The highest nominal voltage among sources that are offering
 *       anything sets the run's voltage. Sources at that voltage form the <em>pool</em>; a source
 *       at a lower voltage is <em>blocked</em> and says so. They do not blend (6.2).</li>
 *   <li><b>Shares.</b> Every reachable sink wants some current. If the pool can cover the total
 *       (up to {@link FTuning#SOURCE_OVERDRAW_LIMIT} times its rating) every sink gets what it
 *       wants; if not, every sink gets the same fraction of what it wants. There is no
 *       nearest-first starvation, so a starved sink is starved visibly and in proportion.</li>
 *   <li><b>Sag.</b> The pool's voltage falls as it is drawn past its rating ({@link
 *       ElectricRules#sagFactor}). Never destroyed.</li>
 *   <li><b>Drop.</b> Current accumulates from sinks back up the tree, and each wire block takes
 *       {@code dropPerAmp x current} off the voltage passing through it. A sink sees what is left
 *       (6.4). The longer or more loaded the path, the less arrives; nothing cliffs.</li>
 *   <li>Everyone is told, zeros included.</li>
 * </ol>
 *
 * <h2>What re-routes the tree</h2>
 * A topology change is a new run. Separately, the <em>pool</em> can change under a standing
 * topology -- a pulse source starts, a lower-voltage source is outbid -- and that rebuilds the
 * tree here, detected by comparing which sources are pooled, not by watching the world.
 *
 * <h2>Current is routed along each sink's shortest path to the pool</h2>
 * With several pooled sources that is an approximation of how parallel supplies really share a
 * wire. It is chosen because it is deterministic, loop-free and cheap, and because the figure the
 * player acts on -- one wire carrying too much -- is exactly what it reports.
 */
public final class ElectricRun {

    private static final float SAME_VOLTAGE = 1e-3f;

    private final ElectricMember[] members;
    private final int[][] adjacent;

    // The cache: valid until the pool changes.
    private final boolean[] pooled;
    private int[] order = new int[0];
    private final int[] parent;
    private final boolean[] reached;
    private boolean built;
    private int rebuilds;

    // Scratch, reused every tick.
    private final float[] through;
    private final float[] volts;
    private final boolean[] active;
    private final boolean[] pooledNow;

    /**
     * @param members  every member of the run, indexed
     * @param adjacent for each member, the indices it is electrically adjacent to
     */
    public ElectricRun(List<? extends ElectricMember> members, int[][] adjacent) {
        int n = members.size();
        this.members = members.toArray(new ElectricMember[0]);
        this.adjacent = adjacent;
        pooled = new boolean[n];
        parent = new int[n];
        reached = new boolean[n];
        through = new float[n];
        volts = new float[n];
        active = new boolean[n];
        pooledNow = new boolean[n];
    }

    public int size() {
        return members.length;
    }

    public ElectricMember member(int index) {
        return members[index];
    }

    /** How many times the routing tree has been built. For tests, and for diagnosing churn. */
    public int rebuilds() {
        return rebuilds;
    }

    public void tick() {
        int n = members.length;

        // 1. Who is supplying.
        float topVolts = 0;
        for (int i = 0; i < n; i++) {
            active[i] = false;
            if (members[i].role() != ElectricMember.Role.SOURCE)
                continue;
            float v = members[i].sourceVolts();
            if (v > 0 && members[i].sourceAmps() > 0) {
                active[i] = true;
                topVolts = Math.max(topVolts, v);
            }
        }
        float capacity = 0;
        for (int i = 0; i < n; i++) {
            pooledNow[i] = active[i] && Math.abs(members[i].sourceVolts() - topVolts) < SAME_VOLTAGE;
            if (pooledNow[i])
                capacity += members[i].sourceAmps();
        }
        if (!built || !Arrays.equals(pooledNow, pooled)) {
            System.arraycopy(pooledNow, 0, pooled, 0, n);
            route();
        }

        // 2. Shares. Only a sink the pool can reach asks for anything.
        float demand = 0;
        for (int i = 0; i < n; i++)
            if (reached[i] && members[i].role() == ElectricMember.Role.SINK)
                demand += members[i].wantedAmps();
        float limit = capacity * FTuning.SOURCE_OVERDRAW_LIMIT;
        float share = demand <= limit || demand <= 0 ? 1f : limit / demand;

        Arrays.fill(through, 0f);
        for (int i = 0; i < n; i++)
            if (reached[i] && members[i].role() == ElectricMember.Role.SINK)
                through[i] = members[i].wantedAmps() * share;

        // Current accumulates from the leaves back toward the supply. `order` is breadth first,
        // so walking it backwards visits every child before its parent.
        float drawn = demand * share;
        for (int k = order.length - 1; k >= 0; k--) {
            int i = order[k];
            if (parent[i] >= 0)
                through[parent[i]] += through[i];
        }

        // 3. Sag.
        float rootVolts = topVolts * ElectricRules.sagFactor(capacity > 0 ? drawn / capacity : 0f);

        // 4. Drop, outward from the supply.
        for (int k = 0; k < order.length; k++) {
            int i = order[k];
            if (parent[i] < 0)
                volts[i] = rootVolts;
            else if (members[i].role() == ElectricMember.Role.WIRE)
                volts[i] = Math.max(0f, volts[parent[i]] - members[i].dropPerAmp() * through[i]);
            else
                volts[i] = volts[parent[i]];
        }

        // 5. Tell everyone.
        for (int i = 0; i < n; i++) {
            ElectricMember m = members[i];
            switch (m.role()) {
                case SOURCE -> {
                    if (pooledNow[i])
                        m.onResolved(rootVolts, drawn * m.sourceAmps() / capacity, 0f, false);
                    else
                        m.onResolved(0f, 0f, 0f, active[i]);
                }
                case SINK -> {
                    if (reached[i])
                        m.onResolved(volts[i], through[i], m.wantedAmps(), false);
                    else
                        m.onResolved(0f, 0f, m.wantedAmps(), false);
                }
                case WIRE -> {
                    if (reached[i])
                        m.onResolved(volts[i], through[i], 0f, false);
                    else
                        m.onResolved(0f, 0f, 0f, false);
                }
            }
        }
    }

    /**
     * Breadth-first from every pooled source. Roots and wires expand; a sink is a leaf, and a
     * source that is not a root is never reached (it is blocked, or idle, and conducts nothing).
     */
    private void route() {
        int n = members.length;
        rebuilds++;
        built = true;
        Arrays.fill(parent, -1);
        Arrays.fill(reached, false);

        int[] queue = new int[n];
        int head = 0, tail = 0;
        for (int i = 0; i < n; i++)
            if (pooled[i]) {
                reached[i] = true;
                queue[tail++] = i;
            }
        while (head < tail) {
            int i = queue[head++];
            if (members[i].role() == ElectricMember.Role.SINK)
                continue;
            for (int j : adjacent[i]) {
                if (reached[j] || members[j].role() == ElectricMember.Role.SOURCE)
                    continue;
                reached[j] = true;
                parent[j] = i;
                queue[tail++] = j;
            }
        }
        order = Arrays.copyOf(queue, tail);
    }
}
