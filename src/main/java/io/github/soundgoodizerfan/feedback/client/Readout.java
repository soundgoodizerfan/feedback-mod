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
package io.github.soundgoodizerfan.feedback.client;

import java.util.Locale;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * The only place the mod turns a simulated figure into something a player is allowed to read.
 *
 * <h2>Why this is one class</h2>
 * Philosophy 8's rule -- senses give adjectives, numbers cost an instrument -- is a rule about
 * <em>wording</em>, and wording duplicated across a tooltip, a HUD and whatever comes next drifts
 * apart until one of them quietly starts printing a figure. Keeping the vocabulary in one place
 * makes the rule enforceable by reading a single file.
 *
 * <h2>What belongs here and what does not</h2>
 * Only <em>state</em>: what a machine is doing right now, which is the half of the information
 * layer that has to be bought. Requirements and equipment specs are free and exact (§8, <em>what
 * you need is free</em>), so they are formatted at their call sites with no ceremony -- there is no
 * rule to enforce about them.
 *
 * <h2>The instrument seam</h2>
 * {@link #instrumented()} is currently "is the player wearing the debug helmet", which is a cheat
 * standing in for a technology that does not exist yet. When calipers and thermometers arrive they
 * do not replace this -- they widen it, because the real question is per-quantity and per-accuracy:
 * <em>what resolution can this player resolve this variable to, here?</em> A boolean is the
 * degenerate case of that question and the call sites are already asking it.
 */
public final class Readout {

    private Readout() {
    }

    /**
     * Whether the player may read this quantity as a figure rather than an adjective.
     * <p>
     * Reads the local player rather than taking one, because every caller is a client display and
     * there is only ever one player looking.
     * <p>
     * The debug helmet answers yes to everything — it is perfect instrumentation, which §8 says a
     * player may never actually buy, and it is a creative-only cheat precisely so that stays true.
     */
    public static boolean instrumented(Quantity quantity) {
        return source(quantity) != null;
    }

    /**
     * Which instrument, if any, is answering for this quantity.
     *
     * <h3>Why every figure is signed</h3>
     * A bare number is unfalsifiable. §8 promises that instruments never lie but says nothing
     * about them being accurate -- they drift, they have limited resolution, and eventually they
     * need recalibrating. The moment any of that is real, a player looking at a bad batch needs to
     * know <em>which instrument</em> to distrust, and two instruments of different tiers
     * disagreeing about the same quantity has to read as informative rather than broken.
     * <p>
     * So a reading is attributed to the thing that produced it, always. It also quietly teaches
     * where the number came from: not from the world, but from a device the player chose to own.
     */
    @Nullable
    public static Component source(Quantity quantity) {
        Instrument instrument = Instruments.reading(Minecraft.getInstance().player, quantity);
        return instrument == null ? null : instrument.label();
    }

    /** Whether the player reads this quantity at the truth tier: the exact held figure. */
    public static boolean exact(Quantity quantity) {
        return Instruments.exact(Minecraft.getInstance().player, quantity);
    }

    /**
     * A value as finely as the player's best instrument for it resolves, exact at the truth tier.
     * Every figure display goes through this rather than a fixed format, so no display can show a
     * finer instrument less than it reads. Gate on {@link #instrumented} first: with no instrument
     * this formats at the truth tier.
     */
    public static String figure(Quantity quantity, float value) {
        Instrument instrument = Instruments.reading(Minecraft.getInstance().player, quantity);
        return Instruments.format(value, instrument == null ? Instrument.TRUTH : instrument.resolution(quantity));
    }

    /**
     * An adjective with what it means appended while Shift is held: {@code head-heavy -- balance
     * over 0.85}. The thresholds are passed in from the constants that decide them, so the gloss
     * cannot drift from the rule. Saying what a word means is not a reading: it hands over no
     * state, only the definition of a free sense's bands (philosophy 8).
     */
    public static Component gloss(Component word, String key, Object... args) {
        if (!net.minecraft.client.gui.screens.Screen.hasShiftDown())
            return word;
        return word.copy().append(Component.literal(" - ").append(Component.translatable(key, args))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * A reading, signed by the instrument that took it: {@code Calipers: 9 / 14 Fu}.
     */
    public static Component reading(Quantity quantity, String key, Object... args) {
        Instrument instrument = Instruments.reading(Minecraft.getInstance().player, quantity);
        Component figure = Component.translatable(key, args);
        return instrument == null ? figure : Instruments.signed(instrument, figure);
    }

    /**
     * How fast a run is turning, in words. The bands are in {@link FTuning} because they are a
     * balance decision about what is distinguishable, not a display detail.
     */
    public static Component speed(Rpm rpm) {
        float magnitude = Math.abs(rpm.value());
        float stopped = FTuning.STOPPED_RPM_THRESHOLD.value();
        float turning = FTuning.TURNING_RPM.value();
        float fast = FTuning.SPINNING_FAST_RPM.value();
        if (magnitude < stopped)
            return banded(adjective("feedback.readout.stopped"), 0, stopped, "RPM");
        if (magnitude < turning)
            return banded(adjective("feedback.readout.turning_slowly"), stopped, turning, "RPM");
        if (magnitude < fast)
            return banded(adjective("feedback.readout.turning"), turning, fast, "RPM");
        return banded(adjective("feedback.readout.spinning_fast"), fast, Float.POSITIVE_INFINITY, "RPM");
    }

    /**
     * Whether a network is in trouble, in words, or null when it is not.
     *
     * <h3>Why this is free</h3>
     * It hands over no figures -- it does not say how far over, or by how much, or what the ledger
     * is. It says only <em>this one, and it is struggling</em>, which is exactly what the smoke and
     * the creak already say at the sources. A player who cannot diagnose a stalled factory at all
     * has no problem to solve, only a mod that appears broken.
     */
    @Nullable
    public static Component strain(float loadSu, float capacitySu, boolean overstressed) {
        if (overstressed)
            return gloss(Component.translatable("feedback.readout.overloaded").withStyle(ChatFormatting.RED),
                    "feedback.gloss.overloaded");
        if (capacitySu > 0 && loadSu > capacitySu * FTuning.STRAINING_LOAD_FRACTION)
            return gloss(Component.translatable("feedback.readout.straining").withStyle(ChatFormatting.GOLD),
                    "feedback.gloss.straining", number(FTuning.STRAINING_LOAD_FRACTION * 100));
        return null;
    }

    /**
     * How hot something is, in words.
     *
     * <h3>Why these read as colours, and why the scale runs out</h3>
     * They are the bands a blacksmith works to, which is the honest answer to what a player's own
     * senses give them. You cannot feel 1450 Tu; you can see that iron has gone from bright red to
     * orange, and you can be badly wrong about which.
     * <p>
     * The scale <em>ends</em>, and that is the important part. Past brilliant white the eye stops
     * distinguishing anything, and iron's spoil point sits above that line -- so a player watching
     * the glow cannot see themselves crossing the temperature that ruins the batch. A free sense
     * has a range, exactly as a bought one does (§8), and this is the range of the one apparatus
     * every player already owns.
     */
    public static Component temperature(Tu tu) {
        float bottom = 0;
        for (int band = 0; band < FTuning.HEAT_BAND_TOPS.size(); band++) {
            float top = FTuning.HEAT_BAND_TOPS.get(band).value();
            if (tu.value() < top)
                return banded(adjective("feedback.readout.heat." + band), bottom, top, "Tu");
            bottom = top;
        }
        return banded(adjective("feedback.readout.heat.beyond"), FTuning.MAX_VISIBLE_TU.value(), Float.POSITIVE_INFINITY, "Tu");
    }

    /**
     * A temperature as either a word or a figure, whichever the player has paid for.
     * <p>
     * The quantising is the whole of what an instrument tier buys (§8). The world is never
     * consulted differently and the reading is never <em>wrong</em> -- it is coarser, which is why
     * a poor instrument costs reproducibility rather than success.
     */
    public static Component temperatureReading(Tu tu) {
        if (!instrumented(Quantity.TEMPERATURE))
            return temperature(tu);
        // Unwrapped at the instrument seam, which is quantity-agnostic by design: quantise
        // answers for Fu as readily as Tu, so it cannot take a temperature.
        return reading(Quantity.TEMPERATURE, "feedback.readout.tu", figure(Quantity.TEMPERATURE, tu.value()));
    }

    /**
     * How battered a machine is, in words, or null when it is sound.
     *
     * <h3>Free, and silent until there is something to say</h3>
     * Damage is visible in a way a temperature is not -- a mushroomed head can be seen from across
     * the room -- so charging an instrument for it would be hiding a sense the player already has
     * (§8). What it withholds is the figure. And a sound machine says nothing at all, because a
     * line reading "fine" on every hammer in the factory is a line nobody reads by the third one.
     *
     * <h3>Why this is a diagnosis and not a chore</h3>
     * Wear here is caused only by misuse, so this line appearing is information: it means force is
     * being spent on something that cannot take it, and which mistake it was is a question the
     * player can now go and answer. A correctly built line never shows it.
     */
    @Nullable
    public static Component condition(float condition) {
        float sound = FTuning.CONDITION_SOUND * 100;
        float marked = FTuning.CONDITION_MARKED * 100;
        float battered = FTuning.CONDITION_BATTERED * 100;
        return switch (FTuning.conditionBand(condition)) {
            case 0 -> null;
            case 1 -> banded(adjective("feedback.readout.condition.marked"), marked, sound, "% condition");
            case 2 -> banded(Component.translatable("feedback.readout.condition.battered")
                    .withStyle(ChatFormatting.GOLD), battered, marked, "% condition");
            default -> banded(Component.translatable("feedback.readout.condition.spent")
                    .withStyle(ChatFormatting.RED), 0, battered, "% condition");
        };
    }

    /**
     * Condition as a word, or as the figure at the truth tier. Calipers read it too, but only by
     * the act of measuring a stopped machine (philosophy 8), so a passive display gives the figure
     * at the truth tier alone.
     */
    @Nullable
    public static Component conditionReading(float condition) {
        if (exact(Quantity.CONDITION))
            return reading(Quantity.CONDITION, "feedback.readout.condition.figure", figure(Quantity.CONDITION, condition * 100));
        return condition(condition);
    }

    /**
     * How far along a workpiece is, in words.
     * <p>
     * Tempting and wrong: a progress bar. That is a number wearing a picture.
     */
    public static Component progress(int worked, int required) {
        float fraction = worked / (float) required;
        if (fraction < 0.25f)
            return banded(adjective("feedback.workpiece.barely_marked"), 0, 25, "% of the work");
        if (fraction < 0.5f)
            return banded(adjective("feedback.workpiece.taking_shape"), 25, 50, "% of the work");
        if (fraction < 0.75f)
            return banded(adjective("feedback.workpiece.visibly_worked"), 50, 75, "% of the work");
        return banded(adjective("feedback.workpiece.nearly_there"), 75, Float.POSITIVE_INFINITY, "% of the work");
    }

    /**
     * How charged a jar round is (mechanics 6.6), as a word or, with an electroscope, a figure.
     * The words are against the drive a coil is built for, because that is the question the
     * player is asking of a round: will this push, or is it flat.
     */
    public static Component chargeReading(float volts) {
        if (instrumented(Quantity.VOLTAGE))
            return reading(Quantity.VOLTAGE, "feedback.readout.volts", figure(Quantity.VOLTAGE, volts));
        float v = FTuning.GUN_COIL_NOMINAL_VOLTS;
        float drive = volts / v;
        if (drive <= 0.05f)
            return banded(adjective("feedback.readout.charge.flat"), 0, 0.05f * v, "V");
        if (drive < 0.5f)
            return banded(adjective("feedback.readout.charge.weak"), 0.05f * v, 0.5f * v, "V");
        if (drive < 1.25f)
            return banded(adjective("feedback.readout.charge.charged"), 0.5f * v, 1.25f * v, "V");
        return banded(adjective("feedback.readout.charge.crackling"), 1.25f * v, Float.POSITIVE_INFINITY, "V");
    }

    /** A figure with one decimal place, and without one when it would read ".0". */
    public static String number(float value) {
        return value == Math.rint(value)
                ? String.format(Locale.ROOT, "%.0f", value)
                : String.format(Locale.ROOT, "%.1f", value);
    }

    /** {@link #gloss} for a word that names a band of one figure: under, between, or over. */
    public static Component banded(Component word, float low, float high, String unit) {
        if (low <= 0)
            return gloss(word, "feedback.gloss.below", number(high), unit);
        if (Float.isInfinite(high))
            return gloss(word, "feedback.gloss.above", number(low), unit);
        return gloss(word, "feedback.gloss.band", number(low), number(high), unit);
    }

    /** Index 0..2 (low, middling, high) of a material figure against its {@link FTuning#statEdges}, or -1 if unbanded. */
    public static int bandIndex(String stat, float value) {
        float[] e = FTuning.statEdges(stat);
        return e == null ? -1 : value < e[0] ? 0 : value < e[1] ? 1 : 2;
    }

    /** The band word for a material figure, glossed on Shift; beside the number, never instead of it. Null if unbanded. */
    public static Component rating(String stat, float value, String unit) {
        int i = bandIndex(stat, value);
        if (i < 0)
            return null;
        float[] e = FTuning.statEdges(stat);
        return banded(adjective("feedback.band." + i), i == 0 ? 0 : e[i - 1], i == 2 ? Float.POSITIVE_INFINITY : e[i], unit);
    }

    private static Component adjective(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    }
}
