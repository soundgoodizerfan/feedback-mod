// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.crucible;

import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.process.ThermalProcessTable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The arc hearth's fluid face: a look at the tank like {@link io.github.soundgoodizerfan.feedback.process.MoltenReadout}, and it
 * takes the one thing a plain crucible cannot, the inert gas a sintering row names as its {@code atmosphere} (nitrogen, argon), held
 * in the tank under the arc head. Nothing drains: a melt is the mold's, the gas is spent by the batch that needs it.
 * A plain crucible keeps the read-only view.
 */
public record HearthFluidFace(CrucibleBlockEntity hearth) implements IFluidHandler {

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return hearth.getTank().getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return hearth.getTank().getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        var id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        return hearth.isArcHearth() && ThermalProcessTable.get().entries().stream()
                .anyMatch(p -> p.atmosphere().filter(a -> a.fluid().equals(id)).isPresent());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return isFluidValid(0, resource) ? hearth.getTank().fill(FluidHeat.bare(resource), action) : 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
