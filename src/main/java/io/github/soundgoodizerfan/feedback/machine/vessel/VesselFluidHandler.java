// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.vessel;

import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.process.ThermalProcessTable;

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
        return ThermalProcessTable.get().boiling(stack).filter(p -> p.lightShare().isPresent()).isPresent();
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
        return tank == 0 && (boilsByGrade(stack) || isAtmosphere(stack));
    }

    /** A sealed chamber takes the gas a thermal process asks for (sintering in nitrogen); an open one would lose it. */
    private boolean isAtmosphere(FluidStack stack) {
        var id = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(stack.getFluid());
        return vessel.getKind().sealed() && io.github.soundgoodizerfan.feedback.process.ThermalProcessTable.get().entries().stream()
                .anyMatch(p -> p.atmosphere().filter(a -> a.fluid().equals(id)).isPresent());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        // Bare of temperature so a bucket's crude merges with what is there; a different grade does not merge (shortcut).
        return boilsByGrade(resource) || isAtmosphere(resource) ? vessel.getTank().fill(FluidHeat.bare(resource), action) : 0;
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
