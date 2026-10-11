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
package io.github.soundgoodizerfan.feedback.control.bimetallic;

import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A piece of bent metal that snaps over at one temperature.
 *
 * <h2>Why there is no controller in this slice</h2>
 * Beat 2 has exactly one condition to watch, and a single condition needs no logic. Putting a
 * programmable controller here would waste it -- and worse, it would teach the player that
 * automation means programming, when the thing they actually need to learn first is that
 * automation means <em>closing a loop</em>. A strip wired to a clutch is a thermostat, and it
 * closes the loop with no logic block anywhere.
 * <p>
 * Slice 2's controller then arrives at the moment §13 says it should: when one condition stops
 * being enough, because a bellows and a damper are two actuators and two conditions and a strip
 * cannot express that. It <em>replaces</em> the strip rather than appearing beside it.
 *
 * <h2>It has no setting, and that is the design</h2>
 * Philosophy 3. The trip point is where this strip's two metals say it is. Wanting a different one
 * means crafting a different strip, which is how the family grows -- and it is why the strip never
 * becomes the thing a player tunes their way out of a problem with.
 *
 * <h2>Nothing in the system knows that 1425 Tu matters</h2>
 * The firebox does not, the bellows does not, the crucible does not, and the thermometer certainly
 * does not. The player built the only thing that knows (§8), out of a part they chose because of
 * what it trips at. That is the whole of what automation is in this mod.
 *
 * <h2>The bug it ships with is the lesson</h2>
 * The loop overshoots, and it is meant to. A small crucible climbs at roughly 8 Tu/t under air, so
 * by the time this has snapped over and the clutch has let go, the vessel has sailed past the
 * window. Good enough information, and still unable to hold the condition -- philosophy 8's second
 * half delivered as a bug the player fixes rather than a paragraph they read. Every available fix
 * is physical (§4): a bigger vessel, more insulation, a shorter throw on the bellows, or a gear
 * train that strokes it less often.
 */
public class BimetallicStripBlockEntity extends BlockEntity {

    /**
     * How often it looks.
     *
     * <h3>Why a strip does not read every tick</h3>
     * It is a lump of metal that has to warm up before it can bend, and a sensor with no lag would
     * make the loop tighter than any physical part could be -- which would quietly remove the
     * problem this block exists to hand the player. Response time is one of §8's six apparatus
     * properties, and this is the first place it has cost anything.
     */
    private static final int READ_INTERVAL = 20;

    private int sinceRead;

    public BimetallicStripBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.BIMETALLIC_STRIP.get(), pos, state);
    }

    /** Read off the blockstate, which is saved -- a separate field was not, and came back false. */
    public boolean isTripped() {
        return getBlockState().getValue(BimetallicStripBlock.TRIPPED);
    }

    public Tu getTripPoint() {
        return FTuning.BIMETALLIC_TRIP_TU;
    }

    /** The vessel it is clipped to, if any. */
    @Nullable
    public ThermalBody getWatched() {
        if (level == null)
            return null;
        for (Direction face : Direction.values()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(face));
            // A sealed vessel cannot be got at, which is the wall the whole beat is built around.
            if (be instanceof ThermalBody body && body.hasThermowell())
                return body;
        }
        return null;
    }

    public void tickServer() {
        if (level == null || ++sinceRead < READ_INTERVAL)
            return;
        sinceRead = 0;

        ThermalBody watched = getWatched();
        if (watched == null)
            return;

        boolean nowTripped =
                watched.getTemperature().value() >= FTuning.BIMETALLIC_TRIP_TU.value();
        if (nowTripped == isTripped())
            return;

        boolean tripped = nowTripped;
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(BimetallicStripBlock.TRIPPED, tripped));

        // Cold calls for heat; hot stops asking. The strip has no idea what it is switching, and
        // whatever it is switching has no idea a temperature was involved.
        for (Direction face : Direction.values())
            if (level.getBlockEntity(worldPosition.relative(face)) instanceof Switchable switchable)
                switchable.setEngaged(!tripped);
    }
}
