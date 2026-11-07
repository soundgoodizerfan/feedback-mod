// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.pump;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A flat Su load that moves fluid at a rate set by how fast it is turned: {@link
 * FTuning#PUMP_MB_PER_RPM} mB a tick per RPM, back face to front face. Any fluid -- it is a pump,
 * not a sorter, and never asks what it is moving.
 *
 * <h2>What it pushes against: the block it lifts</h2>
 * Fluids have no pressure beyond the air-only {@code Pu}, but lift costs (user ruling, 2026-10-23): a pump
 * facing up pays {@link FTuning#PUMP_LIFT_SU_PER_BLOCK} on top of its flat load. It moves fluid one block,
 * so a column is a stack of pumps, each paying; level and down cost nothing extra.
 *
 * <h2>Where it draws from</h2>
 * A fluid handler behind it (a condenser, a tank, a boiler's steam), or a source block behind it
 * of a fluid that renews itself, which it draws from without draining -- the deal vanilla already
 * makes with two water sources and a bucket. Lava does not renew, so a pump will not make it
 * from nothing; that is the fluid's own property, not a check on which fluid it is.
 */
public class PumpBlockEntity extends RotationNode {

    /** Fraction of an mB owed from earlier ticks, so a slow pump still moves something. */
    private float owed;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.PUMP.get(), pos, state);
    }

    @Override
    public Su getLoadSu() {
        return getBlockState().getValue(PumpBlock.FACING) == Direction.UP
                ? new Su(FTuning.PUMP_LOAD_SU.value() + FTuning.PUMP_LIFT_SU_PER_BLOCK.value())
                : FTuning.PUMP_LOAD_SU;
    }

    public void tickServer() {
        if (level == null)
            return;
        owed += Math.abs(getRpm().value()) * FTuning.PUMP_MB_PER_RPM;
        int amount = (int) owed;
        if (amount <= 0)
            return;
        owed -= amount;

        Direction out = getBlockState().getValue(PumpBlock.FACING);
        IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK,
                worldPosition.relative(out), out.getOpposite());
        if (target == null)
            return;

        BlockPos behind = worldPosition.relative(out.getOpposite());
        IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, behind, out);
        if (source != null) {
            FluidStack offered = source.drain(amount, IFluidHandler.FluidAction.SIMULATE);
            int fits = offered.isEmpty() ? 0 : target.fill(offered, IFluidHandler.FluidAction.SIMULATE);
            if (fits > 0)
                target.fill(source.drain(offered.copyWithAmount(fits), IFluidHandler.FluidAction.EXECUTE),
                        IFluidHandler.FluidAction.EXECUTE);
            return;
        }
        FluidState pool = level.getFluidState(behind);
        if (pool.isSource() && pool.canConvertToSource(level, behind))
            target.fill(new FluidStack(pool.getType(), amount), IFluidHandler.FluidAction.EXECUTE);
    }
}
