// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.tube;

import io.github.soundgoodizerfan.feedback.core.tube.TubeTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Draws air out of the tube run in front of it at {@link TubeTuning#VACUUM_MB_PER_RPM} a tick per RPM. An
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
        return TubeTuning.VACUUM_LOAD_SU;
    }

    public void tickServer() {
        if (level == null)
            return;
        owed += Math.abs(getRpm().value()) * TubeTuning.VACUUM_MB_PER_RPM;
        int amount = (int) owed;
        if (amount <= 0)
            return;
        owed -= amount;
        if (level.getBlockEntity(worldPosition.relative(getBlockState().getValue(VacuumPumpBlock.FACING))) instanceof TubeBlockEntity t
                && t.network() != null && !t.network().ledger.isEmpty())
            t.network().ledger.drain(amount, IFluidHandler.FluidAction.EXECUTE);
    }
}
