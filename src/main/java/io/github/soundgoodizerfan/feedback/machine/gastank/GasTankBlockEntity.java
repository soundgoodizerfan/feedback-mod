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
package io.github.soundgoodizerfan.feedback.machine.gastank;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.fluid.FluidNetwork;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureBurst;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureHolder;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A large pressure vessel for gas (spec 8.3). Pressure is what it holds over its volume; held over
 * the vessel danger line it is a {@link PressureBurst} holder like the boiler: nothing under the
 * line, a climbing chance past it, certain at critical, and the gas goes with it. A flare or a
 * consumer is the safe way down; the hazard is only reached by compressing more in than is drawn off.
 */
public class GasTankBlockEntity extends BlockEntity implements PressureHolder {

    private final HeatedTank gas = new HeatedTank(FTuning.GAS_TANK_MB, this::setChanged);

    public GasTankBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.GAS_TANK.get(), pos, state);
    }

    public HeatedTank getGas() {
        return gas;
    }

    /**
     * What a pipe sees: a tank takes passive flow only up to 80% of its danger line, so a plain well or
     * pipe never over-fills it; a compressor feeds it through {@link #compressedFill} and can.
     */
    public net.neoforged.neoforge.fluids.capability.IFluidHandler getPort() {
        return new net.neoforged.neoforge.fluids.capability.IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return gas.getFluid();
            }

            @Override
            public int getTankCapacity(int tank) {
                return gas.getCapacity();
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return gas.isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                int room = (int) (FTuning.PRESSURE_VESSEL_DANGER_PU.value() * 0.8f * FTuning.GAS_TANK_MB_PER_PU) - gas.getFluidAmount();
                return room <= 0 ? 0 : gas.fill(resource.copyWithAmount(Math.min(room, resource.getAmount())), action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return gas.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return gas.drain(maxDrain, action);
            }
        };
    }

    public int compressedFill(FluidStack stack, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action) {
        return gas.fill(stack, action);
    }

    @Override
    public Pu getPressure() {
        return new Pu(gas.getFluidAmount() / FTuning.GAS_TANK_MB_PER_PU);
    }

    public void tickServer() {
        if (level == null || gas.isEmpty())
            return;
        gas.relax(AmbientTable.at(level));
        if (PressureBurst.fails(getPressure(), FTuning.PRESSURE_VESSEL_DANGER_PU, FTuning.PRESSURE_VESSEL_CRITICAL_PU,
                level.random)) {
            gas.setFluid(FluidStack.EMPTY);
            PressureBurst.burst(level, worldPosition, FTuning.BOILER_BURST_POWER);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Gas", gas.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Gas"))
            gas.readFromNBT(registries, tag.getCompound("Gas"));
    }
}
