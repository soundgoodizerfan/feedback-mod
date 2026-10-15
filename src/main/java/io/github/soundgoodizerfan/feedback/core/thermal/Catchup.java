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
package io.github.soundgoodizerfan.feedback.core.thermal;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * Bringing a block that was not loaded back up to date.
 *
 * <h2>What the gap is, and why it needs saying</h2>
 * Workpieces cool in an unloaded chunk for free (see {@link ItemHeat}): their temperature is a
 * stamp and a tick and is read when anybody asks. A block is not like that -- it holds a live
 * temperature and updates it by ticking, and a chunk that is not loaded does not tick. Left
 * alone, a crucible you walked away from froze at whatever it was and resumed from there, hot or
 * cold, as though the world had paused around it. TerraFirmaCraft has a calendar and burns fuel
 * across time skips; this is the part of that worth taking, without a calendar.
 *
 * <h2>How it works</h2>
 * A block records the game time it was saved at. On its first tick after a load it asks how long
 * it was away. If that was long enough to matter ({@link FTuning#CATCHUP_MIN_TICKS}) it is
 * advanced across the whole gap at once, in closed form: {@link Heat#relax} is the same
 * difference-driven law {@link Heat#tick} applies one tick at a time, solved for any number of
 * ticks, so a gap of a million ticks costs the same as a gap of twenty. Fuel burns for as much of
 * the gap as it had, and the body relaxes towards the fire for the lit part and towards the room
 * for the rest.
 *
 * <h2>What it deliberately does not do</h2>
 * Process progress is not advanced: an ore in a crucible is not cooked across a gap. Doing it
 * honestly means integrating {@code ThermalProcess} suitability along a temperature curve, which
 * is a second simulation for a stretch nobody saw, and a player who walked away from a process
 * has not earned its completion. It resumes where it left off, at the temperature it now has.
 * Air banked by a bellows does not survive a gap either: nobody was working the bellows.
 */
public final class Catchup {

    private static final String KEY = "SavedAt";

    private long savedAt = -1L;

    /** Read what {@link #save} wrote. Absent in a block that has never been saved. */
    public void load(CompoundTag tag) {
        savedAt = tag.contains(KEY) ? tag.getLong(KEY) : -1L;
    }

    public void save(CompoundTag tag, Level level) {
        if (level != null)
            tag.putLong(KEY, level.getGameTime());
    }

    /**
     * How many ticks this block was away, once: asking again returns {@code 0}. {@code 0} too
     * when it was not saved before or was away less than {@link FTuning#CATCHUP_MIN_TICKS}.
     */
    public long take(Level level) {
        long saved = savedAt;
        savedAt = -1L;
        if (saved < 0)
            return 0L;
        long gap = level.getGameTime() - saved;
        return gap >= FTuning.CATCHUP_MIN_TICKS ? gap : 0L;
    }

    /**
     * Advance {@code body} across {@code gap} ticks: towards {@code fire} while it burned, then
     * towards the room for the rest.
     */
    public static void settle(ThermalBody body, FireSpan fire, Tu ambient, long gap) {
        long lit = Math.min(fire.litTicks(), gap);
        Heat.relax(body, new Tu(fire.tu()), ambient, true, lit);
        Heat.relax(body, ambient, ambient, false, gap - lit);
    }
}
