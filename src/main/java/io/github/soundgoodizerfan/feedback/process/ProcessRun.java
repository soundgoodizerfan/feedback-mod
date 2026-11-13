// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import java.util.List;
import java.util.function.ObjIntConsumer;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The steps every runner of a {@code thermal_process} row shares, whatever drives it: a vessel's hold timer (thermal vessel and
 * reactor), a reactor's tank tick, a right-click dip, an electrolyser's electrical tick. A driver keeps its own trigger, its own
 * energy and its own tanks; it calls these to ask how far a row has come and to take and put fluid.
 */
public final class ProcessRun {

    private ProcessRun() {
    }

    /** A row's rate for this tick: zero if the temperature moved faster than the row tolerates. */
    public static float limited(ThermalProcess row, float rate, float lastDelta) {
        return Math.abs(lastDelta) > row.maxRateTuPerTick().tuPerTick() ? 0f : rate;
    }

    /** Progress after a tick at {@code rate}: it climbs by the rate, and decays rather than resetting when the rate is zero. */
    public static float hold(float progress, float rate) {
        return rate > 0f ? progress + rate : Math.max(0f, progress - FTuning.TPU_DECAY_PER_TICK);
    }

    /** mB of {@code fluid} held across {@code tanks}. */
    public static int held(List<? extends FluidTank> tanks, ResourceLocation fluid) {
        int total = 0;
        for (FluidTank tank : tanks)
            if (!tank.isEmpty() && BuiltInRegistries.FLUID.getKey(tank.getFluid().getFluid()).equals(fluid))
                total += tank.getFluidAmount();
        return total;
    }

    /** Takes {@code mb} of {@code fluid} from {@code tanks}; {@code seen} is told each stack and the mB about to be taken from it. */
    public static void draw(List<? extends FluidTank> tanks, ResourceLocation fluid, int mb, ObjIntConsumer<FluidStack> seen) {
        int left = mb;
        for (FluidTank tank : tanks) {
            if (left <= 0 || tank.isEmpty() || !BuiltInRegistries.FLUID.getKey(tank.getFluid().getFluid()).equals(fluid))
                continue;
            seen.accept(tank.getFluid(), Math.min(left, tank.getFluidAmount()));
            left -= tank.drain(left, IFluidHandler.FluidAction.EXECUTE).getAmount();
        }
    }

    /** mB of {@code made} a tank has room for: it is empty or already holds that fluid. */
    public static int room(FluidTank tank, FluidStack made) {
        return tank.isEmpty() || FluidStack.isSameFluid(tank.getFluid(), made) ? tank.getSpace() : 0;
    }

    /** Puts {@code product} in the tank already holding that fluid, else the first empty one; returns what did not fit. */
    public static int deposit(List<? extends FluidTank> tanks, FluidStack product) {
        int left = product.getAmount();
        for (FluidTank tank : tanks)
            if (!tank.isEmpty() && tank.getFluid().getFluid() == product.getFluid()) {
                left -= tank.fill(product, IFluidHandler.FluidAction.EXECUTE);
                break;
            }
        if (left == product.getAmount())
            for (FluidTank tank : tanks)
                if (tank.isEmpty()) {
                    left -= tank.fill(product, IFluidHandler.FluidAction.EXECUTE);
                    break;
                }
        return left;
    }
}
