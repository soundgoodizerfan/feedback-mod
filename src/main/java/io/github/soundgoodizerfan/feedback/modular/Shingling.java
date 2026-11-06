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
package io.github.soundgoodizerfan.feedback.modular;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Deformation;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.process.Material;

/**
 * Shingling: the hand version of hammering a bloom into iron. The slag inside a bloom is liquid while the piece is hot, and a blow
 * squeezes the pockets near it outward until they leak out of the edge; hammer too long on a cooling piece and the slag freezes in place.
 * Pure model shared by the screen (which plays it) and the server (which replays the samples and decides).
 *
 * <p>The billet is a rectangle of {@link #LONG} by 1 in screen units, turned a quarter each {@link Kind#TURN}. A blow shifts each live
 * pocket within the {@link FTuning#SHINGLE_FOOTPRINT} away from the blow, the more the nearer, by the blow's landed work over the slag's
 * viscosity, faster the hotter the slag; the flow is wider along the screen's x than y, so turning the piece changes which way is short.
 * What the material changes: the slag's viscosity and melting point (speed, and how long the pockets stay free), the bloom's hardness
 * against the hammer (work per blow), its density (the split limit) and its hot-working window (when the blow lands at all), the
 * bloom's slag grade (how many pockets).
 */
public final class Shingling {

    public static final float LONG = 1.6f;

    public enum Kind { LIGHT, HEAVY, TURN }

    /** One input: when (ticks from the start), what, and where on the billet face as a fraction of its width and height. */
    public record Event(int tick, Kind kind, float fx, float fy) { }

    /** Everything the figures of the material and hammer decide; both sides build it from the same data. */
    public record Setup(long seed, int slag, float temperature, float hold, float slagMelt, float slagViscosity, float minWork, float density,
                        float hardness, St light, St heavy) {

        public static Setup of(long seed, int slag, float temperature, float hold, Material bloom, Material slagMaterial, ForgeRhythm.Strikes strikes) {
            float melt = slagMaterial.thermal().meltingPoint().map(t -> t.value()).orElse(1000f);
            float visc = slagMaterial.molten().viscosity().orElse(3f);
            return new Setup(seed, slag, temperature, hold, melt, visc, bloom.minTemperature().value(), bloom.density().muPerMb(),
                    bloom.hardness().value(), strikes.tap(), strikes.heavy());
        }
    }

    /** What is left of the piece. */
    public record Result(int pockets, int remaining, int leaked, int splits, boolean frozen) {

        public boolean done() {
            return remaining <= Math.floor(pockets * FTuning.SHINGLE_DONE_SHARE);
        }
    }

    private final Setup setup;
    private final float[] px, py;
    private final boolean[] gone;
    private float sw = LONG, sh = 1f;
    private int leaked, splits, last;

    public Shingling(Setup setup) {
        this.setup = setup;
        int n = FTuning.SHINGLE_POCKETS_BASE + FTuning.SHINGLE_POCKETS_PER_SLAG * Math.max(0, Math.min(3, setup.slag()));
        px = new float[n];
        py = new float[n];
        gone = new boolean[n];
        SplittableRandom rng = new SplittableRandom(setup.seed());
        for (int i = 0; i < n; i++) {
            px[i] = 0.12f + rng.nextFloat() * (LONG - 0.24f);
            py[i] = 0.12f + rng.nextFloat() * 0.76f;
        }
    }

    public int pockets() {
        return px.length;
    }

    public boolean gone(int i) {
        return gone[i];
    }

    /** Pocket position in screen units, within {@link #width()} by {@link #height()}. */
    public float x(int i) {
        return px[i];
    }

    public float y(int i) {
        return py[i];
    }

    public float width() {
        return sw;
    }

    public float height() {
        return sh;
    }

    /** The piece's heat at {@code tick}: it cools at the item rate down to what a hearth holds it at. */
    public float temperature(int tick) {
        return Math.max(setup.temperature() - FTuning.ITEM_COOLING_TU_PER_TICK.tuPerTick() * tick, Math.min(setup.temperature(), setup.hold()));
    }

    /** Whether the slag can still move at {@code tick}. */
    public boolean runny(int tick) {
        return temperature(tick) >= setup.slagMelt();
    }

    /** Whether a blow lands on the piece at all at {@code tick}: it must be inside the bloom's hot-working window. */
    public boolean workable(int tick) {
        return temperature(tick) >= setup.minWork();
    }

    /** Applies one input; events must come in tick order. Returns the pockets it let out. */
    public int apply(Event e) {
        if (e.tick() < last || e.tick() > FTuning.SHINGLE_MAX_TICKS)
            return 0;
        last = e.tick();
        if (e.kind() == Kind.TURN) {
            for (int i = 0; i < px.length; i++) {
                float x = px[i];
                px[i] = py[i];
                py[i] = sw - x;
            }
            float t = sw;
            sw = sh;
            sh = t;
            return 0;
        }
        if (!workable(e.tick()))
            return 0;
        boolean heavy = e.kind() == Kind.HEAVY;
        int push = Deformation.landed(heavy ? setup.heavy() : setup.light(), new St(setup.hardness()));
        if (push <= 0)
            return 0;
        if (heavy && push > setup.density() * FTuning.SHINGLE_SPLIT_PER_DENSITY)
            splits++;
        float cx = Math.max(0f, Math.min(1f, e.fx())) * sw, cy = Math.max(0f, Math.min(1f, e.fy())) * sh;
        float temp = temperature(e.tick());
        float speed = runny(e.tick()) ? Math.max(0.25f, Math.min(1f, (temp - setup.slagMelt()) / FTuning.SHINGLE_FLUID_RANGE_TU)) : 0f;
        speed /= Math.max(0.5f, setup.slagViscosity() / 3f);
        int out = 0;
        float r = FTuning.SHINGLE_FOOTPRINT;
        for (int i = 0; i < px.length; i++) {
            if (gone[i])
                continue;
            float dx = px[i] - cx, dy = py[i] - cy, d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d >= r || speed <= 0f)
                continue;
            if (d < 1e-3f) {
                dx = (i & 1) == 0 ? 1f : -1f;
                dy = 0f;
                d = 1f;
            }
            float shift = FTuning.SHINGLE_PUSH * push * speed * (1f - Math.min(d, r) / r) + 0.004f * push * speed;
            px[i] += dx / d * shift;
            py[i] += dy / d * shift * 0.5f;
            if (px[i] < 0f || px[i] > sw || py[i] < 0f || py[i] > sh) {
                gone[i] = true;
                leaked++;
                out++;
            }
        }
        return out;
    }

    public Result result() {
        int remaining = px.length - leaked;
        return new Result(px.length, remaining, leaked, splits, !runny(last) && remaining > 0);
    }

    /** Replays a whole list of inputs on a fresh piece: what the server does. */
    public static Result replay(Setup setup, List<Event> events) {
        Shingling s = new Shingling(setup);
        for (Event e : events)
            s.apply(e);
        return s.result();
    }

    /** The slag items a result drops: one for each few pockets let out, at least one if any. */
    public static int slagDropped(Result r) {
        return r.leaked() == 0 ? 0 : (r.leaked() + FTuning.SHINGLE_LEAK_PER_SLAG - 1) / FTuning.SHINGLE_LEAK_PER_SLAG;
    }

    /** The slag grade a bloom returned unfinished carries: a grade for each few pockets still in it. */
    public static int slagLeft(Result r) {
        return r.remaining() == 0 ? 0 : Math.min(3, (r.remaining() + FTuning.SHINGLE_POCKETS_PER_SLAG - 1) / FTuning.SHINGLE_POCKETS_PER_SLAG);
    }

    /** Millibuckets of iron a result keeps: a ninth lost to each split. */
    public static int metalMb(Result r) {
        return Math.max(0, 144 - 16 * r.splits());
    }

    public static List<Event> events(List<Integer> ticks, List<Integer> kinds, List<Integer> xs, List<Integer> ys) {
        List<Event> out = new ArrayList<>();
        int n = Math.min(Math.min(ticks.size(), kinds.size()), Math.min(xs.size(), ys.size()));
        for (int i = 0; i < n && i < 256; i++) {
            int k = Math.max(0, Math.min(2, kinds.get(i)));
            out.add(new Event(ticks.get(i), Kind.values()[k], xs.get(i) / 1000f, ys.get(i) / 1000f));
        }
        return out;
    }
}
