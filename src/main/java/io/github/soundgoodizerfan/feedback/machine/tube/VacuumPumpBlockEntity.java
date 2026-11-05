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
package io.github.soundgoodizerfan.feedback.machine.tube;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Draws air out of the tube run in front of it at {@link FTuning#VACUUM_MB_PER_RPM} a tick per RPM. An
 * evacuated tube holds its vacuum only while this keeps up with the seals' leak; the air goes to the
 * open atmosphere.
 */
public class VacuumPumpBlockEntity extends RotationNode {

    private float owed;

    public VacuumPumpBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.VACUUM_PUMP.get(), pos, state);
    }

    @Override
    public Su getLoadSu() {
        return FTuning.VACUUM_LOAD_SU;
    }

    public void tickServer() {
        if (level == null)
            return;
        owed += Math.abs(getRpm().value()) * FTuning.VACUUM_MB_PER_RPM;
        int amount = (int) owed;
        if (amount <= 0)
            return;
        owed -= amount;
        if (level.getBlockEntity(worldPosition.relative(getBlockState().getValue(VacuumPumpBlock.FACING))) instanceof TubeBlockEntity t
                && t.network() != null && !t.network().ledger.isEmpty())
            t.network().ledger.drain(amount, IFluidHandler.FluidAction.EXECUTE);
    }
}
