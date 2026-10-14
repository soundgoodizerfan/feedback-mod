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
 * <h2>What it pushes against: nothing, yet</h2>
 * There is no head. Fluids have no pressure beyond the air-only {@code Pu}, so the load does not
 * rise with lift or with how full the far tank is; it is the rate that costs, through the speed it
 * needs. If a pump against a column is ever a decision, the load is where it goes.
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
        return FTuning.PUMP_LOAD_SU;
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
