// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.boiler;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Lets a bucket, a hopper, or a future pipe fill the boiler's water and drain its Steam, without
 * either side ever touching the other tank.
 * <p>
 * Same argument as {@code CrucibleItemHandler}: this is the one place "outside world" rules get
 * applied, so the boiler's own tanks stay simple.
 */
public class BoilerFluidHandler implements IFluidHandler {

    private final BoilerBlockEntity boiler;

    public BoilerFluidHandler(BoilerBlockEntity boiler) {
        this.boiler = boiler;
    }

    @Override
    public int getTanks() {
        return 2;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank == 0 ? boiler.getWaterTank().getFluid() : boiler.getSteamTank().getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? boiler.getWaterTank().getCapacity() : boiler.getSteamTank().getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 && boiler.getWaterTank().isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return boiler.getWaterTank().fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (!boiler.getSteamTank().isFluidValid(resource))
            return FluidStack.EMPTY;
        return boiler.getSteamTank().drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return boiler.getSteamTank().drain(maxDrain, action);
    }
}
