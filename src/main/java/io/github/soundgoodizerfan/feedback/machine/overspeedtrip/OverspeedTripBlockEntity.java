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
package io.github.soundgoodizerfan.feedback.machine.overspeedtrip;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.machine.clutch.ClutchBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A centrifugal overspeed trip: a {@link ClutchBlockEntity} whose flyweights throw it open when
 * its own shaft turns faster than its fitted spring ({@link FTuning#OVERSPEED_TRIP_SPRINGS_RPM}).
 *
 * <h2>A safety cutoff, not regulation</h2>
 * It does not hold a speed and does not slow anything -- it is the bimetallic strip's bang-bang
 * shape one energy type up (philosophy 13; {@code TODO.md} 5a). Steady-state speed control stays
 * the player's loop. The only thing it adds to a clutch is the sensor and the trigger in one
 * part, with no logic anywhere, and the speed is the fitted spring's: three springs, swapped by hand (sneak and right-click), never a dial.
 *
 * <h2>It latches, and that is the same mechanism as the clutch</h2>
 * Once thrown it stays open until somebody closes it -- a hand, or a controller's Switch
 * card, through {@link #setEngaged}. The driving side it is bolted to is still turning too fast,
 * so a trip that re-closed itself would only re-open on the next tick: the latch is the honest
 * version of that, and it is what a real trip does. Closing it while the run is still over speed
 * trips it again within a tick, which is the part telling the player the cause is still there.
 *
 * <h2>It never wears</h2>
 * Wear is misuse only. A trip that never fires costs nothing, and one that fires has done its
 * job: letting go is the intended act, not force it failed to put into the work.
 */
public class OverspeedTripBlockEntity extends ClutchBlockEntity {

    /** Index into {@link FTuning#OVERSPEED_TRIP_SPRINGS_RPM}: which spring is fitted. */
    private int spring;

    public OverspeedTripBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.OVERSPEED_TRIP.get(), pos, state);
    }

    public float tripRpm() {
        return FTuning.OVERSPEED_TRIP_SPRINGS_RPM[spring];
    }

    /** Swap to the next spring, light to heavy and round again; returns the new trip speed. */
    public float cycleSpring() {
        spring = (spring + 1) % FTuning.OVERSPEED_TRIP_SPRINGS_RPM.length;
        setChanged();
        return tripRpm();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Spring", spring);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        spring = Math.max(0, Math.min(tag.getInt("Spring"), FTuning.OVERSPEED_TRIP_SPRINGS_RPM.length - 1));
    }

    /**
     * Read every tick: flyweights answer to speed as it happens, so unlike the strip, which has
     * to warm through before it bends, nothing here lags. Cheap -- one float compare, and
     * {@link #setEngaged} only rebuilds on the tick it actually trips.
     * <p>
     * A disengaged trip is still a member of the driving run (see
     * {@code ClutchBlock#hasShaftTowards}), so it reads the speed of the side that is too fast.
     */
    public void tickServer() {
        if (level != null && isEngaged()
                && Math.abs(getRpm().value()) > tripRpm())
            setEngaged(false);
    }
}
