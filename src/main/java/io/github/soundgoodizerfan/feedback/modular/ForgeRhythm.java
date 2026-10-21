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
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.item.HandSwing;
import io.github.soundgoodizerfan.feedback.process.Deformation;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

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
 *   <li><b>Work done against work required moves the as-built geometry</b> ({@link #work}): every
 *       blow that lands puts {@code St / hardness} Fu into the part ({@link Deformation#landed}, the
 *       hammer's own rule), a clean run of the pattern is the Fu the pattern asks, and the ratio
 *       is stored on the part. Under it the part is thick and heavy (a blunt, unfinished bar); over
 *       it, thin and light (drawn out, scaled away). A blow that cannot land -- cold metal, or under
 *       the material's hardness -- goes into the striker, not the part.</li>
 * </ul>
 *
 * <p><b>Trap, found while writing this:</b> flaws alone were tempting, because the judge already
 * counts misses and strays and a flaw is one integer on the part. But a flaw is a verdict on the
 * <em>timing</em>, and says nothing about how much metal was moved: a player who hammers twice as
 * much as the pattern asks and one who stops halfway can both be "a little rough". Work is the
 * conserved quantity (the hammer's {@code Fu}), so it is the one that can say overrun from short.
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
    /**
     * The key on a part's {@code dims} for the work ratio the forge left ({@link #work}). The ratio is kept
     * between these two: a bar nobody hit is still a bar (thick, but not unbounded), and a part
     * hammered past twice what it asked is scrap thinner than the model can say anything about.
     */
    public static final String WORK = "work";
    public static final float MIN_WORK = 0.5f, MAX_WORK = 2f;

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

    /** The blows a striker lands, tap and heavy, in St: a player's hand, or the Mechanical Hammer's two throws. */
    public record Strikes(St tap, St heavy) {

        /** A player's: the blow swing for a tap and the heavy swing for a heavy, a mighty one under Strength. */
        public static Strikes hand(Player player) {
            return new Strikes(HandSwing.BLOW.strength(), HandSwing.MIGHTY.within(player).strength());
        }

        St of(boolean heavy) {
            return heavy ? this.heavy : tap;
        }
    }

    /**
     * Whether any note of {@code rhythm} lands on {@code material} with these blows: the hard gate. A
     * pattern none of whose blows reach the material's hardness cannot be played at all (philosophy 7:
     * a genuine impossibility, not a slow version); one that merely has some blows too light is
     * playable and comes out short.
     */
    public static boolean movable(String rhythm, Strikes strikes, Material material) {
        for (int i = 0; i < rhythm.length(); i++)
            if (note(rhythm.charAt(i)) && Deformation.landed(strikes.of(rhythm.charAt(i) == 'h'), material.hardness()) > 0)
                return true;
        return false;
    }

    /**
     * What the blows did to the metal.
     *
     * @param done     Fu the blows that landed put in, each with the noise floor's jitter.
     * @param required Fu a clean run of the pattern puts in: one blow per note, at least one Fu each, so a
     *                 note whose blow cannot land is a shortfall and not a note that costs nothing.
     * @param wasted   blows that went into the striker: struck cold, still too hot, or under the hardness.
     * @param absorbed their St summed, which is what a machine wears by ({@code MechanicalHammerBlockEntity#absorb}).
     */
    public record Work(float done, float required, int wasted, float absorbed) {

        /** Done over required, clamped: below 1 the part is short, above it overrun. */
        public float ratio() {
            return required <= 0 ? 1f : Math.max(MIN_WORK, Math.min(MAX_WORK, done / required));
        }
    }

    /** Fu of the blows in {@code hits} against {@code rhythm}, by the same live window the judge used. */
    public static Work work(String rhythm, Strikes strikes, Material material, List<Hit> hits, int[] live, RandomSource noise) {
        float required = 0;
        for (int i = 0; i < rhythm.length(); i++)
            if (note(rhythm.charAt(i)))
                required += Math.max(1, Deformation.landed(strikes.of(rhythm.charAt(i) == 'h'), material.hardness()));
        float done = 0;
        int wasted = 0;
        float absorbed = 0;
        for (Hit hit : hits) {
            int fu = hit.tick() < live[0] || hit.tick() > live[1] ? 0 : Deformation.landed(strikes.of(hit.heavy()), material.hardness());
            if (fu == 0) {
                wasted++;
                absorbed += strikes.of(hit.heavy()).value();
            } else
                done += fu * (1f + (noise.nextFloat() * 2f - 1f) * NOISE);
        }
        return new Work(done, required, wasted, absorbed);
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
