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
package io.github.soundgoodizerfan.feedback.potion;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringRepresentable;

/**
 * The curve a plant drags the marker along (potions spec 15): a closed-form function of {@code t} in 0..1 to a point {@code (u, v)} in the
 * segment's own frame (u along the heading, v to its left), from a small catalogue. Pure over primitives, so the server walks it and the
 * screen draws it with the same code. Every shape starts at (0, 0); all but {@code spiral} (non-integer turns) and {@code hook} end at (1, 0).
 *
 * <p>Parameters ride as two floats whose meaning follows the type: arc (bulge), wave (cycles, amp), zigzag (teeth, amp), loop (loops, r),
 * spiral (turns), hook (at, side). In data each has its own name and a default, so {@code {"type":"wave"}} alone is a wave.
 *
 * <p>Stirring walks the curve by arc length, so a table of cumulative length over {@link #STEPS} samples turns an arc fraction into {@code t}.
 */
public record PotionShape(Type type, float p1, float p2) {

    public enum Type implements StringRepresentable {
        LINE, ARC, WAVE, ZIGZAG, LOOP, SPIRAL, HOOK;

        static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final PotionShape LINE = new PotionShape(Type.LINE, 0f, 0f);

    /** Samples in the arc-length table. */
    private static final int STEPS = 96;
    private static final float TAU = (float) (2 * Math.PI);

    /** Cumulative unit-shape length at each sample, per shape: computed once, read by the server's tick and the screen's draw. */
    private static final ConcurrentHashMap<PotionShape, float[]> TABLES = new ConcurrentHashMap<>();

    /** The wire/JSON form: a type and every parameter by name, each optional. */
    private record Raw(Type type, Optional<Float> bulge, Optional<Float> cycles, Optional<Float> amp, Optional<Float> teeth, Optional<Float> loops,
                       Optional<Float> r, Optional<Float> turns, Optional<Float> at, Optional<Float> side) {}

    public static final Codec<PotionShape> CODEC = RecordCodecBuilder.<Raw>create(i -> i.group(
            Type.CODEC.optionalFieldOf("type", Type.LINE).forGetter(Raw::type),
            Codec.FLOAT.optionalFieldOf("bulge").forGetter(Raw::bulge),
            Codec.FLOAT.optionalFieldOf("cycles").forGetter(Raw::cycles),
            Codec.FLOAT.optionalFieldOf("amp").forGetter(Raw::amp),
            Codec.FLOAT.optionalFieldOf("teeth").forGetter(Raw::teeth),
            Codec.FLOAT.optionalFieldOf("loops").forGetter(Raw::loops),
            Codec.FLOAT.optionalFieldOf("r").forGetter(Raw::r),
            Codec.FLOAT.optionalFieldOf("turns").forGetter(Raw::turns),
            Codec.FLOAT.optionalFieldOf("at").forGetter(Raw::at),
            Codec.FLOAT.optionalFieldOf("side").forGetter(Raw::side)
    ).apply(i, Raw::new)).xmap(PotionShape::fromRaw, PotionShape::toRaw);

    /** A shape of this type with every parameter at its default, as a bare {@code {"type": ...}} reads. */
    public static PotionShape of(Type type) {
        return fromRaw(new Raw(type, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
    }

    private static PotionShape fromRaw(Raw r) {
        return switch (r.type) {
            case LINE -> LINE;
            case ARC -> new PotionShape(Type.ARC, r.bulge.orElse(0.25f), 0f);
            case WAVE -> new PotionShape(Type.WAVE, r.cycles.orElse(1f), r.amp.orElse(0.2f));
            case ZIGZAG -> new PotionShape(Type.ZIGZAG, r.teeth.orElse(3f), r.amp.orElse(0.15f));
            case LOOP -> new PotionShape(Type.LOOP, r.loops.orElse(1f), r.r.orElse(1.5f));
            case SPIRAL -> new PotionShape(Type.SPIRAL, r.turns.orElse(1.5f), 0f);
            case HOOK -> new PotionShape(Type.HOOK, Math.max(0.05f, Math.min(0.95f, r.at.orElse(0.7f))), r.side.orElse(1f) < 0f ? -1f : 1f);
        };
    }

    private static Raw toRaw(PotionShape s) {
        Optional<Float> none = Optional.empty(), a = Optional.of(s.p1), b = Optional.of(s.p2);
        return switch (s.type) {
            case LINE -> new Raw(s.type, none, none, none, none, none, none, none, none, none);
            case ARC -> new Raw(s.type, a, none, none, none, none, none, none, none, none);
            case WAVE -> new Raw(s.type, none, a, b, none, none, none, none, none, none);
            case ZIGZAG -> new Raw(s.type, none, none, b, a, none, none, none, none, none);
            case LOOP -> new Raw(s.type, none, none, none, none, a, b, none, none, none);
            case SPIRAL -> new Raw(s.type, none, none, none, none, none, none, a, none, none);
            case HOOK -> new Raw(s.type, none, none, none, none, none, none, none, a, b);
        };
    }

    /** The point at {@code t} (0..1) in the segment's frame, written into {@code out} (u, v). */
    public void point(float t, float[] out) {
        switch (type) {
            case LINE -> {
                out[0] = t;
                out[1] = 0f;
            }
            case ARC -> {
                out[0] = t;
                out[1] = p1 * 4f * t * (1f - t);
            }
            case WAVE -> {
                out[0] = t;
                out[1] = p2 * (float) Math.sin(TAU * p1 * t);
            }
            case ZIGZAG -> {
                out[0] = t;
                out[1] = p2 * tri(p1 * t);
            }
            case LOOP -> {
                // Prolate trochoid: it advances along u while circling, so a loop forms where r is over 1.
                float w = TAU * Math.max(p1, 0.01f);
                out[0] = t - p2 * (float) Math.sin(w * t) / w;
                out[1] = p2 * (1f - (float) Math.cos(w * t)) / w;
            }
            case SPIRAL -> {
                out[0] = t * (float) Math.cos(TAU * p1 * t);
                out[1] = t * (float) Math.sin(TAU * p1 * t);
            }
            case HOOK -> {
                // Straight to u = at, then a quarter circle of the remaining arc length (1 - at) bending to the side.
                if (t <= p1) {
                    out[0] = t;
                    out[1] = 0f;
                } else {
                    float rad = (1f - p1) * 2f / (float) Math.PI, phi = (float) (Math.PI / 2) * (t - p1) / (1f - p1);
                    out[0] = p1 + rad * (float) Math.sin(phi);
                    out[1] = p2 * rad * (1f - (float) Math.cos(phi));
                }
            }
        }
    }

    /** A triangle wave: 0 at integers, +1 a quarter on, -1 three quarters on. */
    private static float tri(float x) {
        float f = x - 0.25f;
        f -= (float) Math.floor(f);
        return 4f * Math.abs(f - 0.5f) - 1f;
    }

    private float[] table() {
        return TABLES.computeIfAbsent(this, s -> {
            float[] cum = new float[STEPS + 1], prev = new float[2], cur = new float[2];
            s.point(0f, prev);
            for (int i = 1; i <= STEPS; i++) {
                s.point((float) i / STEPS, cur);
                cum[i] = cum[i - 1] + (float) Math.hypot(cur[0] - prev[0], cur[1] - prev[1]);
                prev[0] = cur[0];
                prev[1] = cur[1];
            }
            return cum;
        });
    }

    /** Arc length of the unit shape (1 for a line). Multiply by the reach for the segment's length. */
    public float length() {
        return table()[STEPS];
    }

    /** The {@code t} at which a fraction {@code s} (0..1) of the arc length has been walked. */
    public float tAt(float s) {
        float[] cum = table();
        float target = Math.max(0f, Math.min(1f, s)) * cum[STEPS];
        int lo = 0, hi = STEPS;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (cum[mid] <= target)
                lo = mid;
            else
                hi = mid;
        }
        float span = cum[hi] - cum[lo];
        return (lo + (span <= 1e-9f ? 0f : (target - cum[lo]) / span)) / STEPS;
    }

    /** The point after walking an arc fraction {@code s} (0..1), in the segment's frame. */
    public void atArc(float s, float[] out) {
        point(tAt(s), out);
    }

    /** A self-check: {@code java PotionShape} (compile it with the sources) walks every shape and asserts the endpoints and a monotone arc. */
    public static void main(String[] args) {
        float[] o = new float[2];
        for (Type t : Type.values()) {
            PotionShape s = of(t);
            s.point(0f, o);
            assert Math.abs(o[0]) < 1e-5 && Math.abs(o[1]) < 1e-5 : t + " does not start at the origin";
            s.atArc(1f, o);
            if (t != Type.SPIRAL && t != Type.HOOK)
                assert Math.abs(o[0] - 1f) < 1e-4 && Math.abs(o[1]) < 1e-4 : t + " does not end at (1, 0): " + o[0] + ", " + o[1];
            assert s.length() >= 0.999f : t + " is shorter than its chord";
            float last = -1f;
            for (int i = 0; i <= 20; i++) {
                float tt = s.tAt(i / 20f);
                assert tt >= last : t + " arc table not monotone";
                last = tt;
            }
            System.out.println(t + " length " + s.length());
        }
    }
}
