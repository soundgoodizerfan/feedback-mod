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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The client's picture of a frame's spin ({@link io.github.soundgoodizerfan.feedback.net.FrameSpinPayload}):
 * a rate per thing that turns, and the angle it had reached when the rate last changed, so a new
 * rate carries on from where the old one left the part rather than jumping.
 */
public final class FrameSpin {

    private float chain;
    private double chainPhase;
    private Set<Long> tubes = Set.of();
    private final Map<Integer, float[]> hubs = new HashMap<>(); // rate, phase
    private double since;

    public void set(float chain, Set<Long> tubes, Map<Integer, Float> hubs, long gameTime) {
        double dt = (gameTime - since) / 20.0;
        chainPhase += this.chain * dt;
        this.hubs.values().forEach(h -> h[1] += (float) (h[0] * dt));
        this.chain = chain;
        this.tubes = Set.copyOf(tubes);
        this.hubs.keySet().retainAll(hubs.keySet());
        hubs.forEach((v, rate) -> this.hubs.computeIfAbsent(v, k -> new float[2])[0] = rate);
        since = gameTime;
    }

    /** The angle of tube {@code key} at {@code time} (game ticks, partial included); 0 if it is not driven. */
    public double tube(long key, double time) {
        return tubes.contains(key) ? chainPhase + chain * (time - since) / 20.0 : 0;
    }

    /** The angle of the tire at {@code vertex} at {@code time}. */
    public double hub(int vertex, double time) {
        float[] h = hubs.get(vertex);
        return h == null ? 0 : h[1] + h[0] * (time - since) / 20.0;
    }
}
