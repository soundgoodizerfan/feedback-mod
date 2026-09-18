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
package io.github.soundgoodizerfan.feedback.machine.heatexchanger;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A wall between two {@link ThermalBody}s. Bridges whatever it is placed against on its own axis
 * -- {@link HeatExchangerBlock#FACING} and its opposite -- and moves heat between them every tick,
 * hot side to cold side, at {@link FTuning#HEAT_EXCHANGER_CONDUCTANCE}.
 *
 * <h2>No identity check, and no direction to pick</h2>
 * It does not ask what either side is -- a Crucible, a Boiler, a future block that has not been
 * written yet, all answer the same {@link ThermalBody} contract and this reads it the same way
 * {@code Heat.tick} already reads a vessel's own fire. Flow direction is not a setting either: {@link
 * Heat#exchange} already runs hot to cold by construction, the same way a vessel already cannot
 * pass its own fire's temperature (see that method's own doc). {@code
 * feedback_philosophy.md} §10 names a genuine <em>pumped</em> exchanger -- forcing heat uphill on
 * borrowed power, with the failure mode of overrunning both ends at once -- as a real later
 * mechanic; this is the passive half only, the wall itself, built first per the same
 * infrastructure-before-necessity corollary as {@code Instrument} and {@code fitting/}.
 *
 * <h2>Placement is the whole decision</h2>
 * Nothing here is powered and nothing is switched. The player's only choice is which two things
 * to stand it between -- philosophy 3's "shafts and pipes are still real blocks... a genuine
 * spatial and budgetary puzzle" applied to a wall instead of a pipe.
 */
public class HeatExchangerBlockEntity extends BlockEntity {

    public HeatExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.HEAT_EXCHANGER.get(), pos, state);
    }

    public void tickServer() {
        if (level == null)
            return;

        Direction axis = getBlockState().getValue(HeatExchangerBlock.FACING);

        if (!(level.getBlockEntity(worldPosition.relative(axis)) instanceof ThermalBody a))
            return;
        if (!(level.getBlockEntity(worldPosition.relative(axis.getOpposite())) instanceof ThermalBody b))
            return;

        Heat.exchange(a, b, FTuning.HEAT_EXCHANGER_CONDUCTANCE);
    }
}
