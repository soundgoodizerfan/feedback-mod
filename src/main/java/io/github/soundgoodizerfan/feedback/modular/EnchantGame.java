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

/**
 * Enchanting as sigil inscription ({@code docs/specs/feedback_enchanting_spec.md} 3): a hot part cools through its attuning
 * window while the player writes the effect's name in the Standard Galactic Alphabet on a pad, dragging a pen through the
 * lit stars of each letter. This is the pure half -- the heat clock, the judge and the server's record of a session -- so
 * the server judges exactly what the screen showed and nothing here touches a world, as {@link ForgeRhythm} does for the forge.
 *
 * <p>The pen follows the mouse; the material's lag, coercive field and loop width are the pen's inertia, bite and reach
 * ({@link Sigil.Pen}). <b>The clock is heat</b>: the pen marks only while the part is under its attuning point and over
 * its lock temperature; under the lock the result is judged as it stands. The screen and the server run the same
 * {@code Sigil.Pen} over the same samples, one packed pen sample per tick ({@link Sigil#pack}); the result stays
 * discrete (a level and a flaw).
 */
public final class EnchantGame {

    private EnchantGame() {
    }

    /** What a session came to: the level it takes at (0 for none, negative on the mirror side) and the flaws it cost the part. */
    public record Verdict(int level, int flaw) {
        public boolean none() {
            return level == 0;
        }
    }

    /** An attunement begun: which effect, when, and how hot the part was. */
    public record Session(ResourceLocation effect, long started, float temperature) {}

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    public static void begin(UUID player, Session session) {
        SESSIONS.put(player, session);
    }

    public static Session end(UUID player) {
        return SESSIONS.remove(player);
    }

    /** The most a part of {@code material} takes: 1 plus its enchantability over a step, capped. */
    public static int level(Material material) {
        return Math.min(ModularTuning.ENCHANT_MAX_LEVEL,
                1 + material.enchantability().orElse(ModularTuning.ENCHANTABILITY_FALLBACK) / ModularTuning.ENCHANT_PER_LEVEL);
    }

    /** How many attunements a part of {@code material} takes: one per step of enchantability, rounded up, at least one. */
    public static int capacity(Material material) {
        int e = material.enchantability().orElse(ModularTuning.ENCHANTABILITY_FALLBACK);
        return Math.max(1, (e + ModularTuning.ENCHANT_PER_SLOT - 1) / ModularTuning.ENCHANT_PER_SLOT);
    }

    /** Ticks from a start at {@code temperature} until the part locks, capped at a session's length. */
    public static int lockTick(Material.Attuning p, float temperature) {
        float rate = FTuning.ITEM_COOLING_TU_PER_TICK.tuPerTick();
        return Math.min(ModularTuning.ENCHANT_MAX_TICKS, (int) Math.ceil(Math.max(0, temperature - p.lock().value()) / rate));
    }

    /**
     * Whether a session at {@code temperature} is <em>held</em>: the window stays open for all of it and the part does not
     * cool. A cold-worked material is, and so is any part already at or under its lock: the session is slow and clean
     * and costs the full price. A part hotter than its lock is not held: it cools through the window as it is written on.
     */
    public static boolean held(Material.Attuning p, float temperature) {
        return p.cold() || temperature <= p.lock().value();
    }

    /** Heat at tick {@code i} of a session started at {@code temperature}, Tu; a held session does not cool. */
    public static float heat(Material.Attuning p, float temperature, int i) {
        return held(p, temperature) ? temperature : temperature - FTuning.ITEM_COOLING_TU_PER_TICK.tuPerTick() * i;
    }

    /** Whether the part has cooled to its lock by tick {@code i}: the session is over. Never for a held session. */
    public static boolean locked(Material.Attuning p, float temperature, int i) {
        return !held(p, temperature) && heat(p, temperature, i) <= p.lock().value();
    }

    /** Whether the pen marks at tick {@code i}: a held session always, else while the part is inside its window. */
    public static boolean live(Material.Attuning p, float temperature, int i) {
        if (held(p, temperature))
            return true;
        float heat = heat(p, temperature, i);
        return heat <= p.temperature().value() && heat > p.lock().value();
    }

    /** The pen's jitter factor at tick {@code i}: the minimum for a held session, rising to 1.5 at the hot end of the window. */
    public static float jitter(Material.Attuning p, float temperature, int i) {
        if (held(p, temperature))
            return 0.5f;
        return 0.5f + (heat(p, temperature, i) - p.lock().value()) / Math.max(1f, p.temperature().value() - p.lock().value());
    }

    /**
     * Experience a session costs, whatever the outcome: the full {@link ModularTuning#ENCHANT_XP_COST} for a held session,
     * and for a hot part less by up to {@link ModularTuning#ENCHANT_HOT_DISCOUNT} of it, the hotter the cheaper (a part at
     * its attuning point or above is the cheapest). The price of a hot part is a window that closes as it cools.
     */
    public static int cost(Material.Attuning p, float temperature) {
        int full = ModularTuning.ENCHANT_XP_COST.value();
        if (held(p, temperature))
            return full;
        float h = Math.max(0f, Math.min(1f, (temperature - p.lock().value()) / Math.max(1f, p.temperature().value() - p.lock().value())));
        return Math.max(1, Math.round(full * (1f - ModularTuning.ENCHANT_HOT_DISCOUNT * h)));
    }

    /** The heat a session starts at: the part's own, or for a cold-worked material the attuning point, whatever it held. */
    public static float start(Material.Attuning p, float partHeat) {
        return p.cold() ? p.temperature().value() : partHeat;
    }

    /**
     * Judge a written sigil against the effect. The accuracy (0..1) becomes a distance from the perfect word, scaled
     * to the effect's band: inside half a band is the full level; at the band one lower; outside it but not past twice
     * the band, two lower with one or two flaws; a pen that never marked is nothing (the part is never destroyed,
     * philosophy 7). A word written reversed signs the level negative, the effect's mirror; an effect with no mirror
     * takes nothing there.
     */
    public static Verdict judge(Attunements.Effect effect, int top, float accuracy, boolean negative, boolean reached) {
        float d = (1f - accuracy) * ModularTuning.ENCHANT_GRADE_SCALE;
        if (!reached || accuracy <= 0f || (negative && !effect.hasMirror()) || d > 2f * effect.band())
            return new Verdict(0, 0);
        int sign = negative ? -1 : 1;
        if (d <= effect.band() / 2f)
            return new Verdict(sign * top, 0);
        if (d <= effect.band())
            return new Verdict(sign * Math.max(1, top - 1), 0);
        return new Verdict(sign * Math.max(1, top - 2), d <= 1.5f * effect.band() ? 1 : 2);
    }

    /** The samples of a finish payload as the server will replay them: bounded. */
    public static List<Integer> bounded(List<Integer> samples) {
        return samples.subList(0, Math.min(ModularTuning.ENCHANT_MAX_TICKS, samples.size()));
    }
}
