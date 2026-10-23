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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * Enchanting as poling ({@code docs/feedback_enchanting_spec.md} 3): a hot part cools through its poling window
 * under a field the player steers; the polarization it freezes in is the enchant. This is the pure half --
 * the model, the judge and the server's record of a session -- so the server judges exactly what the screen
 * showed and nothing here touches a world, as {@link ForgeRhythm} does for the forge.
 *
 * <p>Polarization {@code P} (-1..1) does not move while the field is under the material's coercive field. Over
 * it, {@code P} relaxes toward {@code tanh((|E| - Ec) / w) * sign(E)} with a first-order lag: back the field off
 * and {@code P} stays where it was. <b>The clock is heat</b>: the field acts only while the part is under its
 * poling point and over its lock temperature; under the lock the result is judged as it stands. A noise floor
 * (philosophy 8) is seeded by the samples, so screen and server agree.
 *
 * <p>The field is a continuous scalar, sent as an integer in {@code -SCALE..SCALE} so the replay is exact.
 * That is allowed here: the rule against free scalars is for machine settings, and the <em>result</em> stays
 * discrete (a level and a flaw).
 */
public final class PoleGame {

    private PoleGame() {
    }

    /** What a session came to: the level it takes at (0 for none) and the flaws it cost the part. */
    public record Verdict(int level, int flaw) {
        public boolean none() {
            return level <= 0;
        }
    }

    /** A pole begun: which effect, when, and how hot the part was. */
    public record Session(ResourceLocation effect, long started, float temperature) {}

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    public static void begin(UUID player, Session session) {
        SESSIONS.put(player, session);
    }

    public static Session end(UUID player) {
        return SESSIONS.remove(player);
    }

    /** The field a sample stands for. */
    public static float field(int sample) {
        return Math.max(-1f, Math.min(1f, sample / (float) ModularTuning.POLE_FIELD_SCALE));
    }

    /** The most a part of {@code material} takes: 1 plus its enchantability over a step, capped. */
    public static int level(Material material) {
        return Math.min(ModularTuning.POLE_MAX_LEVEL,
                1 + material.enchantability().orElse(ModularTuning.ENCHANTABILITY_FALLBACK) / ModularTuning.POLE_ENCHANT_PER_LEVEL);
    }

    /** Ticks from a start at {@code temperature} until the part locks, capped at a session's length. */
    public static int lockTick(Material.Poling p, float temperature) {
        float rate = FTuning.ITEM_COOLING_TU_PER_TICK.tuPerTick();
        return Math.min(ModularTuning.POLE_MAX_TICKS, (int) Math.ceil(Math.max(0, temperature - p.lock().value()) / rate));
    }

    /** Whether a part at {@code temperature} can be poled at all: it has not already locked. */
    public static boolean hot(Material.Poling p, float temperature) {
        return temperature > p.lock().value();
    }

    /** Heat at tick {@code i} of a session started at {@code temperature}, Tu. */
    public static float heat(float temperature, int i) {
        return temperature - FTuning.ITEM_COOLING_TU_PER_TICK.tuPerTick() * i;
    }

    /** The polarization one tick later: P moves only if the field is over the coercive field. */
    public static float step(Material.Poling p, float polarization, float field) {
        float over = Math.abs(field) - p.coercive();
        if (over <= 0)
            return polarization;
        return polarization + ((float) Math.tanh(over / p.loopWidth()) * Math.signum(field) - polarization) / p.lag();
    }

    /**
     * Where the polarization ended up. Replays {@code samples} (one per tick, the first at the start) from
     * {@code temperature}; {@code reached[0]} says whether the field ever acted.
     */
    public static float run(Material.Poling p, float temperature, List<Integer> samples, boolean[] reached) {
        RandomSource noise = RandomSource.create(samples.hashCode());
        float poling = p.temperature().value(), lock = p.lock().value();
        float polarization = 0f;
        for (int i = 0; i < Math.min(samples.size(), ModularTuning.POLE_MAX_TICKS); i++) {
            float t = heat(temperature, i);
            if (t <= lock)
                break;
            float field = field(samples.get(i));
            if (t > poling || Math.abs(field) <= p.coercive())
                continue;
            reached[0] = true;
            polarization = step(p, polarization, field);
            // Jitter is larger in the hot end of the window: a hot start is forgiving of time and noisy.
            polarization += (noise.nextFloat() * 2f - 1f) * ModularTuning.POLE_NOISE * (0.5f + (t - lock) / Math.max(1f, poling - lock));
            polarization = Math.max(-1f, Math.min(1f, polarization));
        }
        return polarization;
    }

    /**
     * Judge a final polarization against the effect. In the band and near the middle is the full level; at its edge
     * one lower; outside it but not past twice the band, two lower with one or two flaws; overshoot, the wrong
     * sign or a field that never acted is nothing (the part is never destroyed, philosophy 7).
     */
    public static Verdict judge(PoleEffects.Effect effect, int top, float polarization, boolean reached) {
        float d = Math.abs(polarization - effect.target());
        if (!reached || polarization <= 0f || d > 2f * effect.band())
            return new Verdict(0, 0);
        if (d <= effect.band() / 2f)
            return new Verdict(top, 0);
        if (d <= effect.band())
            return new Verdict(Math.max(1, top - 1), 0);
        return new Verdict(Math.max(1, top - 2), d <= 1.5f * effect.band() ? 1 : 2);
    }

    /** The samples of a finish payload as the server will replay them: bounded. */
    public static List<Integer> bounded(List<Integer> samples) {
        return samples.subList(0, Math.min(ModularTuning.POLE_MAX_TICKS, samples.size()));
    }
}
