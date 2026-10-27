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
package io.github.soundgoodizerfan.feedback.machine.vessel;

import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.process.PhaseChangeTable;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A vessel's fluid face (petrochemistry spec 3.4): a graded crude goes in, the vapour above it comes
 * out, and nothing else moves. Tank 0 is the melt/crude (read, and filled only with something the
 * phase-change table boils by grade), tank 1 the vapour (drained only). The vessel names no crude.
 */
public record VesselFluidHandler(ThermalVesselBlockEntity vessel) implements IFluidHandler {

    /** Whether the table boils {@code stack} by its grade: a crude, not water. */
    static boolean boilsByGrade(FluidStack stack) {
        return PhaseChangeTable.get().boiling(stack).filter(p -> p.lightShare().isPresent()).isPresent();
    }

    @Override
    public int getTanks() {
        return 2;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank == 0 ? vessel.getTank().getFluid() : vessel.getVapourTank().getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? vessel.getTank().getCapacity() : vessel.getVapourTank().getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 && boilsByGrade(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        // Bare of temperature so a bucket's crude merges with what is there; a different grade does not merge (shortcut).
        return boilsByGrade(resource) ? vessel.getTank().fill(FluidHeat.bare(resource), action) : 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return vessel.getVapourTank().drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return vessel.getVapourTank().drain(maxDrain, action);
    }
}
