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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.util.RandomSource;

/**
 * Hand forging as a rhythm (spec 4.1): stock, hot, is struck in the pattern of the variant wanted
 * at the Tool Table, and comes out that variant with as many flaws as the playing was sloppy. This is
 * the pure half -- what the beat is, what a hit is worth, how heat limits it -- so the server
 * judges exactly what the screen showed and nothing here touches a world.
 *
 * <ul>
 *   <li><b>Beats sit on a tick grid</b> ({@link #step}): a note is one step of ticks from the last,
 *       so the window is a tick either side, as a redstone repeater's delay is quantised.</li>
 *   <li><b>Heat is the clock.</b> The part cools on the item curve ({@code ItemHeat}); a note
 *       struck while it is under its hot-working window, or still over it, is dead.</li>
 *   <li><b>A miss never fails the part</b> (philosophy 7): sloppiness is flaws, and flaws cost
 *       toughness ({@link ModularTuning#FLAW_TOUGHNESS_LOSS}).</li>
 *   <li><b>There is a noise floor</b> (philosophy 8): a clean hit is sometimes only loose, whatever
 *       the player does.</li>
 * </ul>
 */
public final class ForgeRhythm {

    private ForgeRhythm() {
    }

    /** Metronome beats counted in before the first step, so the player has the tempo before a note is due. */
    public static final int COUNT_IN = 4;
    /** A hit this close to its note is clean (a tick is 50 ms: three is a third of a second wide). */
    public static final int CLEAN_TICKS = 3;
    /** A hit this close is loose; further is a miss. */
    public static final int LOOSE_TICKS = 5;
    /** How much of a clean hit the noise floor takes back. */
    public static final float NOISE = 0.04f;
    /** A loose hit and a stray one count for this much of a miss. */
    public static final float LOOSE_WEIGHT = 0.4f, STRAY_WEIGHT = 0.5f;
    /** Share of the notes lost, below which the part has no flaw, one, two; beyond is three. */
    public static final float[] FLAW_AT = {0.12f, 0.30f, 0.55f};
    public static final int MAX_FLAW = 3;

    /** One blow the player struck, in ticks from the start of the pattern. */
    public record Hit(int tick, boolean heavy) {}

    /**
     * What a pattern came to.
     *
     * @param dead  notes struck while the part was out of its window: lost like a miss.
     * @param stray blows that matched no note.
     */
    public record Result(int clean, int loose, int missed, int dead, int stray, int flaw) {
        /** Every note struck clean, nothing lost and nothing stray. */
        public boolean perfect() {
            return loose == 0 && missed == 0 && dead == 0 && stray == 0 && clean > 0;
        }
    }

    /**
     * The noise floor for a set of blows: seeded by the blows themselves, so the screen and the
     * server judge the same hits to the same grade and the player is never told something the
     * server then undoes.
     */
    public static RandomSource noiseFor(List<Hit> hits) {
        return RandomSource.create(hits.hashCode());
    }

    /** Ticks per step, which is one metronome click: a harder metal is worked slower. */
    public static int step(Material material) {
        return Math.max(6, Math.min(10, 6 + Math.round(material.hardness().value() / 10f)));
    }

    /** The tick a note is due: after the count-in. */
    public static int noteTick(int index, int step) {
        return (COUNT_IN + index) * step;
    }

    /** The tick the last note is due. */
    public static int end(String rhythm, int step) {
        return noteTick(Math.max(0, rhythm.length() - 1), step);
    }

    /** Whether the pattern character is a note and not a rest. */
    public static boolean note(char c) {
        return c == 't' || c == 'h';
    }

    /**
     * The ticks from the start of the pattern the part is workable for: from when it has cooled
     * under its window's top to when it falls under its bottom. A cold-workable material is always
     * live. {@code temperature} is the part's heat at the start, in Tu.
     *
     * @return {@code {from, to}}.
     */
    public static int[] live(Material material, float temperature) {
        float min = material.minTemperature().value();
        if (min <= 0)
            return new int[] {0, Integer.MAX_VALUE};
        float rate = FTuning.ITEM_COOLING_TU_PER_TICK.tuPerTick();
        float max = material.maxTemperature().value();
        int from = temperature > max ? (int) Math.ceil((temperature - max) / rate) : 0;
        int to = temperature <= min ? -1 : (int) Math.floor((temperature - min) / rate);
        return new int[] {from, to};
    }

    /** Whether the part is workable now, at all: in the window at the start. */
    public static boolean workable(Material material, float temperature) {
        int[] live = live(material, temperature);
        return live[0] <= 0 && live[1] >= 0;
    }

    /** Judge {@code hits} against {@code rhythm}; {@code noise} supplies the floor. */
    public static Result judge(String rhythm, int step, List<Hit> hits, int[] live, RandomSource noise) {
        boolean[] used = new boolean[hits.size()];
        int clean = 0, loose = 0, missed = 0, dead = 0, notes = 0;
        for (int i = 0; i < rhythm.length(); i++) {
            char c = rhythm.charAt(i);
            if (!note(c))
                continue;
            notes++;
            int tick = noteTick(i, step);
            if (tick < live[0] || tick > live[1]) {
                dead++;
                continue;
            }
            int best = -1;
            for (int h = 0; h < hits.size(); h++)
                if (!used[h] && hits.get(h).heavy() == (c == 'h') && Math.abs(hits.get(h).tick() - tick) <= LOOSE_TICKS
                        && (best < 0 || Math.abs(hits.get(h).tick() - tick) < Math.abs(hits.get(best).tick() - tick)))
                    best = h;
            if (best < 0) {
                missed++;
                continue;
            }
            used[best] = true;
            if (Math.abs(hits.get(best).tick() - tick) <= CLEAN_TICKS && noise.nextFloat() >= NOISE)
                clean++;
            else
                loose++;
        }
        int stray = 0;
        for (boolean u : used)
            if (!u)
                stray++;
        float lost = notes == 0 ? 0 : (missed + dead + LOOSE_WEIGHT * loose + STRAY_WEIGHT * stray) / notes;
        int flaw = 0;
        while (flaw < MAX_FLAW && lost >= FLAW_AT[flaw])
            flaw++;
        return new Result(clean, loose, missed, dead, stray, flaw);
    }

    // --- the server's record of a pattern begun ------------------------------------------------

    /** A pattern a player began: when, which, and how hot the part was. */
    public record Session(String variant, long started, float temperature) {}

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    public static void begin(UUID player, Session session) {
        SESSIONS.put(player, session);
    }

    public static Session end(UUID player) {
        return SESSIONS.remove(player);
    }

    /** The hits as a list, bounded. */
    public static List<Hit> hits(List<Integer> ticks, List<Boolean> heavy) {
        List<Hit> out = new ArrayList<>();
        for (int i = 0; i < Math.min(256, Math.min(ticks.size(), heavy.size())); i++)
            out.add(new Hit(ticks.get(i), heavy.get(i)));
        return out;
    }
}
