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
package io.github.soundgoodizerfan.feedback.core.xp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * One connected run of {@link XpNode}s. The containers on it settle toward one level ({@link XpStore#join}), points
 * conserved, at a rate the weakest wire allows: {@code conductance} points per tick per level of difference, so a thick
 * run is quick and a paper one slow. A wire's {@code leak} share of what moves escapes as orbs at the wire, collected
 * and dropped once a second so a leak is never an orb a tick. A creative source pins the whole run to its level instead
 * and gives or takes whatever that needs. A run that has settled does nothing until it is rebuilt ({@link XpNetworks}).
 */
public final class XpNetwork {

    public final Set<XpNode> members = new HashSet<>();
    private boolean settled;
    private float leakAcc;
    private int leaked;
    private int flow;

    /** Points the run moved last tick. For instruments. */
    public int flow() {
        return flow;
    }

    private int lastLeak;

    /** Points that escaped last tick it moved any. For instruments. */
    public int lastLeak() {
        return lastLeak;
    }

    /** Total points held in the run's containers. */
    public int points() {
        int sum = 0;
        for (XpNode n : members)
            if (n.xpStore() != null)
                sum += n.xpStore().points();
        return sum;
    }

    /** The run's level: the pinned one, else total points over total points-per-level. */
    public float level() {
        float pin = pinned();
        if (!Float.isNaN(pin))
            return pin;
        float perLevel = 0;
        for (XpNode n : members)
            if (n.xpStore() != null)
                perLevel += n.xpStore().perLevel();
        return perLevel == 0 ? 0 : points() / perLevel;
    }

    private float pinned() {
        for (XpNode n : members)
            if (!Float.isNaN(n.pinnedLevel()))
                return n.pinnedLevel();
        return Float.NaN;
    }

    void tick(Level level) {
        if (settled && leaked == 0)
            return;
        if (!settled)
            equalise();
        if (leaked > 0 && level.getGameTime() % 20 == 0 && level instanceof ServerLevel server) {
            List<XpNode> leaky = new ArrayList<>();
            for (XpNode n : members)
                if (n.leak() > 0)
                    leaky.add(n);
            if (!leaky.isEmpty()) {
                BlockPos at = leaky.get(level.random.nextInt(leaky.size())).entity().getBlockPos();
                ExperienceOrb.award(server, Vec3.atCenterOf(at), leaked);
            }
            leaked = 0;
        }
    }

    private void equalise() {
        List<XpNode> held = new ArrayList<>();
        float cond = Float.MAX_VALUE;
        float leak = 0;
        for (XpNode n : members) {
            if (n.xpStore() != null)
                held.add(n);
            cond = Math.min(cond, n.conductance());
            leak = Math.max(leak, n.leak());
        }
        int[] delta = new int[held.size()];
        float pin = pinned();
        flow = Float.isNaN(pin) ? joinDeltas(held, delta, cond) : pinnedDeltas(held, delta, cond, pin);
        if (flow == 0) {
            settled = true;
            return;
        }
        // shortcut: the leak is taken off the first gainers; fine while a run's containers are alike
        int gains = 0;
        for (int d : delta)
            gains += Math.max(0, d);
        leakAcc += gains * leak;
        int lost = (int) leakAcc;
        leakAcc -= lost;
        lastLeak = lost;
        leaked += lost;
        for (int i = 0; i < delta.length && lost > 0; i++)
            if (delta[i] > 0) {
                int take = Math.min(lost, delta[i]);
                delta[i] -= take;
                lost -= take;
            }
        for (int i = 0; i < delta.length; i++) {
            XpStore s = held.get(i).xpStore();
            held.get(i).xpSet(s.with(s.points() + delta[i]));
        }
    }

    private static int cap(float conductance, float spread) {
        if (conductance <= 0)
            return 0;
        return conductance >= Float.MAX_VALUE ? Integer.MAX_VALUE : Math.max(1, (int) Math.ceil(conductance * spread));
    }

    /** Containers settling toward each other: gainers and losers scaled to the rate cap, points conserved. Returns points moved. */
    private static int joinDeltas(List<XpNode> held, int[] delta, float cond) {
        int n = held.size();
        List<XpStore> in = new ArrayList<>(n);
        for (XpNode node : held)
            in.add(node.xpStore());
        List<XpStore> out = XpStore.join(in).stores();
        int[] want = new int[n];
        int gain = 0, loss = 0;
        float lo = Float.MAX_VALUE, hi = 0;
        for (int i = 0; i < n; i++) {
            want[i] = out.get(i).points() - in.get(i).points();
            if (want[i] > 0)
                gain += want[i];
            else
                loss -= want[i];
            lo = Math.min(lo, in.get(i).level());
            hi = Math.max(hi, in.get(i).level());
        }
        if (gain == 0 || loss == 0)
            return 0;
        int moved = Math.min(cap(cond, hi - lo), Math.min(gain, loss));
        share(want, delta, moved, gain, true);
        share(want, delta, moved, loss, false);
        return moved;
    }

    /** Spreads {@code moved} points over the gainers (or the losers) in proportion to what each wants. */
    private static void share(int[] want, int[] delta, int moved, int total, boolean gainers) {
        int given = 0;
        for (int i = 0; i < want.length; i++)
            if (gainers ? want[i] > 0 : want[i] < 0) {
                int part = (int) ((long) Math.abs(want[i]) * moved / total);
                delta[i] += gainers ? part : -part;
                given += part;
            }
        for (int i = 0; i < want.length && given < moved; i++)
            if (gainers ? want[i] > delta[i] : -want[i] > -delta[i]) {
                delta[i] += gainers ? 1 : -1;
                given++;
            }
    }

    /** Containers pulled to a pinned level, each at its own rate cap. Returns points that went into containers. */
    private static int pinnedDeltas(List<XpNode> held, int[] delta, float cond, float pin) {
        int gained = 0;
        boolean any = false;
        for (int i = 0; i < delta.length; i++) {
            XpStore s = held.get(i).xpStore();
            int want = Math.min(s.maxPoints(), Math.round(s.perLevel() * pin)) - s.points();
            int cap = cap(cond, Math.abs(pin - s.level()));
            delta[i] = Math.max(-cap, Math.min(cap, want));
            any |= delta[i] != 0;
            gained += Math.max(0, delta[i]);
        }
        return any ? Math.max(1, gained) : 0;
    }
}
