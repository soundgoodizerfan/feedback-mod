// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.electric;

import java.util.Arrays;
import java.util.List;

import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;


/**
 * One connected run of wire, sources and sinks, and the arithmetic that says who gets what.
 *
 * <h2>The shape of the work</h2>
 * {@link ElectricPropagator} floods the world once when the topology changes and hands this class
 * a member list and an adjacency list. This class then builds a <em>routing tree</em> -- every
 * reachable member's parent on its shortest path back to the supply -- and caches it. Each tick
 * after that is arithmetic over arrays: no search, no allocation, no world access
 * ("per-packet routing evaluated per tick per wire block per sink would be the expensive way").
 * It is the {@code RotationPropagator} pattern: rebuild a whole run, never patch one.
 *
 * <h2>What a tick does</h2>
 * <ol>
 *   <li><b>Who is supplying.</b> The highest band among sources that are offering anything sets
 *       the run's band. Sources of exactly that band form the <em>pool</em> and share by rating; a
 *       source of a lower band is <em>blocked</em> and says so. Different bands do not blend (6.2). A
 *       store ({@link ElectricMember#reserve}) in the pool is <em>reserve</em>: generation carries the load, the store
 *       supplies only the shortfall and charges from any surplus.</li>
 *   <li><b>Shares.</b> Every reachable sink wants some current. If the pool can cover the total
 *       (up to {@link ElectricTuning#SOURCE_OVERDRAW_LIMIT} times its rating) every sink gets what it
 *       wants; if not, every sink gets the same fraction of what it wants. There is no
 *       nearest-first starvation, so a starved sink is starved visibly and in proportion.</li>
 *   <li><b>Loss.</b> The packet leaves the supply at the band's size in Eu and each wire block takes
 *       its {@link ElectricMember#lossPerPacket} off it, whatever the current. The band arrives
 *       unchanged and a sink receives what is left (6.4). The longer the path, the less arrives;
 *       nothing cliffs. Current accumulates from sinks back up the tree for overload.</li>
 *   <li>Everyone is told, zeros included.</li>
 * </ol>
 *
 * <h2>What re-routes the tree</h2>
 * A topology change is a new run. Separately, the <em>pool</em> can change under a standing
 * topology -- a pulse source starts, a lower-band source is outbid -- and that rebuilds the
 * tree here, detected by comparing which sources are pooled, not by watching the world.
 *
 * <h2>Current is routed along each sink's shortest path to the pool</h2>
 * With several pooled sources that is an approximation of how parallel supplies really share a
 * wire. It is chosen because it is deterministic, loop-free and cheap, and because the figure the
 * player acts on -- one wire carrying too much -- is exactly what it reports.
 */
public final class ElectricRun {

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
    private final float[] eu;
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
        eu = new float[n];
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

        // 1. Who is supplying: the top band among active sources; exactly that band pools.
        VoltageBand top = null;
        for (int i = 0; i < n; i++) {
            active[i] = false;
            if (members[i].role() != ElectricMember.Role.SOURCE)
                continue;
            VoltageBand band = members[i].sourceBand();
            if (band != null && members[i].sourceAmps() > 0) {
                active[i] = true;
                if (top == null || band.compareTo(top) > 0)
                    top = band;
            }
        }
        float genCap = 0, resCap = 0;
        for (int i = 0; i < n; i++) {
            pooledNow[i] = active[i] && members[i].sourceBand() == top;
            if (pooledNow[i]) {
                if (members[i].reserve())
                    resCap += members[i].sourceAmps();
                else
                    genCap += members[i].sourceAmps();
            }
        }
        float capacity = genCap + resCap;
        VoltageBand runBand = top == null ? VoltageBand.LV : top;
        float rootEu = top == null ? 0f : ElectricRules.packetEu(top);
        if (!built || !Arrays.equals(pooledNow, pooled)) {
            System.arraycopy(pooledNow, 0, pooled, 0, n);
            route();
        }

        // 2. Shares. Only a sink the pool can reach asks for anything.
        float demand = 0;
        // A wire asks for nothing unless it is momentarily loaded (a fence taking a hit): its
        // wantedAmps is a transient draw, counted like a sink's and routed from the wire itself.
        for (int i = 0; i < n; i++)
            if (reached[i] && members[i].role() != ElectricMember.Role.SOURCE)
                demand += members[i].wantedAmps();
        float limit = capacity * ElectricTuning.SOURCE_OVERDRAW_LIMIT;
        float share = demand <= limit || demand <= 0 ? 1f : limit / demand;

        Arrays.fill(through, 0f);
        for (int i = 0; i < n; i++)
            if (reached[i] && members[i].role() != ElectricMember.Role.SOURCE)
                through[i] = members[i].wantedAmps() * share;

        // Current accumulates from the leaves back toward the supply. `order` is breadth first,
        // so walking it backwards visits every child before its parent.
        float drawn = demand * share;
        // Stores are reserve: generation carries the load first, a reserve covers only the shortfall, and a surplus charges it.
        float resDrawn = genCap <= 0 ? drawn : Math.min(resCap, Math.max(0f, drawn - genCap));
        float genDrawn = drawn - resDrawn;
        float chargeShare = 0;
        float chargeWant = 0;
        if (genCap > 0 && resDrawn <= 0 && genDrawn < genCap) {
            for (int i = 0; i < n; i++)
                if (pooledNow[i] && members[i].reserve())
                    chargeWant += members[i].reserveWantedAmps();
            chargeShare = chargeWant <= 0 ? 0 : Math.min(1f, (genCap - genDrawn) / chargeWant);
            genDrawn += chargeWant * chargeShare; // [OPEN] charge current is not routed through wire loss or counted on wires
        }
        for (int k = order.length - 1; k >= 0; k--) {
            int i = order[k];
            if (parent[i] >= 0)
                through[parent[i]] += through[i];
        }

        // 3. Loss, outward from the supply: the band holds, the packet shrinks.
        for (int k = 0; k < order.length; k++) {
            int i = order[k];
            if (parent[i] < 0)
                eu[i] = rootEu;
            else if (members[i].role() == ElectricMember.Role.WIRE)
                eu[i] = Math.max(0f, eu[parent[i]] - members[i].lossPerPacket());
            else
                eu[i] = eu[parent[i]];
        }

        // 4. Tell everyone.
        for (int i = 0; i < n; i++) {
            ElectricMember m = members[i];
            switch (m.role()) {
                case SOURCE -> {
                    if (pooledNow[i] && m.reserve()) {
                        float taken = resDrawn > 0 ? resDrawn * m.sourceAmps() / resCap : -m.reserveWantedAmps() * chargeShare;
                        m.onResolved(runBand, rootEu, taken, 0f, false);
                    } else if (pooledNow[i])
                        m.onResolved(runBand, rootEu, genDrawn * m.sourceAmps() / genCap, 0f, false);
                    else
                        m.onResolved(runBand, 0f, 0f, 0f, active[i]);
                }
                case SINK -> {
                    if (reached[i])
                        m.onResolved(runBand, eu[i], through[i], m.wantedAmps(), false);
                    else
                        m.onResolved(runBand, 0f, 0f, m.wantedAmps(), false);
                }
                case WIRE -> {
                    if (reached[i])
                        m.onResolved(runBand, eu[i], through[i], 0f, false);
                    else
                        m.onResolved(runBand, 0f, 0f, 0f, false);
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
