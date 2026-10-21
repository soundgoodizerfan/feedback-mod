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

/**
 * The client's picture of a frame's spin ({@link io.github.soundgoodizerfan.feedback.net.FrameSpinPayload}):
 * a rate per thing that turns, and the angle it had reached when the rate last changed, so a new
 * rate carries on from where the old one left the part rather than jumping.
 */
public final class FrameSpin {

    /** rate (rad/s) and phase (rad) per turning tube and per tire. */
    private final Map<Long, float[]> tubes = new HashMap<>();
    private final Map<Integer, float[]> hubs = new HashMap<>();
    private Map<Long, Float> su = Map.of();
    private double since;

    public void set(Map<Long, Float> tubes, Map<Long, Float> su, Map<Integer, Float> hubs, long gameTime) {
        double dt = (gameTime - since) / 20.0;
        this.tubes.values().forEach(t -> t[1] += (float) (t[0] * dt));
        this.hubs.values().forEach(h -> h[1] += (float) (h[0] * dt));
        this.tubes.keySet().retainAll(tubes.keySet());
        tubes.forEach((k, rate) -> this.tubes.computeIfAbsent(k, x -> new float[2])[0] = rate);
        this.hubs.keySet().retainAll(hubs.keySet());
        hubs.forEach((v, rate) -> this.hubs.computeIfAbsent(v, k -> new float[2])[0] = rate);
        this.su = Map.copyOf(su);
        since = gameTime;
    }

    /** A tube of a coupled drive line at vertex {@code v}, or null if none: its line's figures. */
    private Long tubeAt(int v) {
        for (long key : tubes.keySet())
            if (Frame.edgeA(key) == v || Frame.edgeB(key) == v)
                return key;
        return null;
    }

    /** Whether vertex {@code v} is on a coupled drive line: an end of one of its tubes. */
    public boolean onChain(int v) {
        return tubeAt(v) != null;
    }

    /** The speed of the drive line through vertex {@code v}, RPM. */
    public float rpm(int v) {
        Long t = tubeAt(v);
        return t == null ? 0 : (float) (tubes.get(t)[0] * 60 / (2 * Math.PI));
    }

    /** What the drive line through vertex {@code v} takes from its network, Su. */
    public float su(int v) {
        Long t = tubeAt(v);
        return t == null ? 0 : su.getOrDefault(t, 0f);
    }

    /** The angle of tube {@code key} at {@code time} (game ticks, partial included); 0 if it is not driven. */
    public double tube(long key, double time) {
        float[] t = tubes.get(key);
        return t == null ? 0 : t[1] + t[0] * (time - since) / 20.0;
    }

    /** The fastest tire's speed, rad/s; 0 if none turn. */
    public float fastestHub() {
        float fastest = 0;
        for (float[] h : hubs.values())
            fastest = Math.max(fastest, Math.abs(h[0]));
        return fastest;
    }

    /** The angle of the tire at {@code vertex} at {@code time}. */
    public double hub(int vertex, double time) {
        float[] h = hubs.get(vertex);
        return h == null ? 0 : h[1] + h[0] * (time - since) / 20.0;
    }
}
