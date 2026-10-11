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
        Instrument instrument = Instruments.best(Minecraft.getInstance().player, quantity);
        return instrument == null ? null : instrument.label();
    }

    /**
     * A reading, signed by the instrument that took it: {@code Calipers: 9 / 14 Fu}.
     */
    public static Component reading(Quantity quantity, String key, Object... args) {
        Instrument instrument = Instruments.best(Minecraft.getInstance().player, quantity);
        Component figure = Component.translatable(key, args);
        return instrument == null ? figure : Instruments.signed(instrument, figure);
    }

    /**
     * How fast a run is turning, in words. The bands are in {@link FTuning} because they are a
     * balance decision about what is distinguishable, not a display detail.
     */
    public static Component speed(Rpm rpm) {
        float magnitude = Math.abs(rpm.value());
        if (magnitude < FTuning.STOPPED_RPM_THRESHOLD.value())
            return adjective("feedback.readout.stopped");
        if (magnitude < FTuning.TURNING_RPM.value())
            return adjective("feedback.readout.turning_slowly");
        if (magnitude < FTuning.SPINNING_FAST_RPM.value())
            return adjective("feedback.readout.turning");
        return adjective("feedback.readout.spinning_fast");
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
            return Component.translatable("feedback.readout.overloaded").withStyle(ChatFormatting.RED);
        if (capacitySu > 0 && loadSu > capacitySu * FTuning.STRAINING_LOAD_FRACTION)
            return Component.translatable("feedback.readout.straining").withStyle(ChatFormatting.GOLD);
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
        if (tu.value() >= FTuning.MAX_VISIBLE_TU.value())
            return adjective("feedback.readout.heat.beyond");
        for (int band = 0; band < FTuning.HEAT_BAND_TOPS.size(); band++)
            if (tu.value() < FTuning.HEAT_BAND_TOPS.get(band).value())
                return adjective("feedback.readout.heat." + band);
        return adjective("feedback.readout.heat.beyond");
    }

    /**
     * A temperature as either a word or a figure, whichever the player has paid for.
     * <p>
     * The quantising is the whole of what an instrument tier buys (§8). The world is never
     * consulted differently and the reading is never <em>wrong</em> -- it is coarser, which is why
     * a poor instrument costs reproducibility rather than success.
     */
    public static Component temperatureReading(Tu tu) {
        Instrument instrument = Instruments.best(Minecraft.getInstance().player, Quantity.TEMPERATURE);
        if (instrument == null)
            return temperature(tu);
        // Unwrapped at the instrument seam, which is quantity-agnostic by design: quantise
        // answers for Fu as readily as Tu, so it cannot take a temperature.
        float shown = Instruments.quantise(tu.value(), instrument.resolution(Quantity.TEMPERATURE));
        return reading(Quantity.TEMPERATURE, "feedback.readout.tu", number(shown));
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
        return switch (FTuning.conditionBand(condition)) {
            case 0 -> null;
            case 1 -> adjective("feedback.readout.condition.marked");
            case 2 -> Component.translatable("feedback.readout.condition.battered")
                    .withStyle(ChatFormatting.GOLD);
            default -> Component.translatable("feedback.readout.condition.spent")
                    .withStyle(ChatFormatting.RED);
        };
    }

    /**
     * How far along a workpiece is, in words.
     * <p>
     * Tempting and wrong: a progress bar. That is a number wearing a picture.
     */
    public static Component progress(int worked, int required) {
        float fraction = worked / (float) required;
        if (fraction < 0.25f)
            return adjective("feedback.workpiece.barely_marked");
        if (fraction < 0.5f)
            return adjective("feedback.workpiece.taking_shape");
        if (fraction < 0.75f)
            return adjective("feedback.workpiece.visibly_worked");
        return adjective("feedback.workpiece.nearly_there");
    }

    /** A figure with one decimal place, and without one when it would read ".0". */
    public static String number(float value) {
        return value == Math.rint(value)
                ? String.format(Locale.ROOT, "%.0f", value)
                : String.format(Locale.ROOT, "%.1f", value);
    }

    private static Component adjective(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    }
}
