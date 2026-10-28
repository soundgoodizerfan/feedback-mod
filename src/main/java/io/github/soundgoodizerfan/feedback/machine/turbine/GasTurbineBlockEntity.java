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
package io.github.soundgoodizerfan.feedback.machine.turbine;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTanks;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A continuous-combustion rotation source (spec 8.4). It burns a fuel gas, whatever gas fluid has a
 * material with a flame temperature, and its output follows {@code spool}: zero from cold, rising
 * while fuel burns, falling slowly when it does not (a long coast). It cannot start itself: spool
 * only climbs from rest while something else on the network is already turning it past {@link
 * FTuning#TURBINE_STARTER_RPM} (a starter motor, a hand crank), after which it sustains itself.
 * A hotter fuel gives more per mB.
 * shortcut: no blade material limit, flameout flood, or overspeed trip yet.
 */
public class GasTurbineBlockEntity extends RotationNode implements HeatedTanks {

    private final HeatedTank fuel = new HeatedTank(FTuning.TURBINE_TANK_MB, stack -> flameOf(stack) > 0f
            && stack.getFluidType().getDensity() < 0, this::setChanged);
    private float spool;
    private float owed;
    private float power;

    public GasTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.GAS_TURBINE.get(), pos, state);
    }

    private static float flameOf(FluidStack stack) {
        Material m = MaterialTable.get().materials().get(BuiltInRegistries.FLUID.getKey(stack.getFluid()));
        return m == null || m.combustion().isEmpty() ? 0f : m.combustion().get().flameTemperature().map(t -> t.value()).orElse(0f);
    }

    public HeatedTank getFuel() {
        return fuel;
    }

    @Override
    public java.util.List<HeatedTank> heatedTanks() {
        return java.util.List.of(fuel);
    }

    public float spool() {
        return spool;
    }

    @Override
    public Rpm getGeneratedRpm() {
        return spool > 0f ? new Rpm(FTuning.TURBINE_RPM.value() * spool) : ZERO_RPM;
    }

    @Override
    public Su getCapacitySu() {
        return spool > 0f ? new Su(FTuning.TURBINE_SU.value() * spool * power) : NO_SU;
    }

    public void tickServer() {
        if (level == null)
            return;
        pull();
        float flame = fuel.isEmpty() ? 0f : flameOf(fuel.getFluid());
        boolean burning = flame > 0f && (spool >= FTuning.TURBINE_SELF_SUSTAIN
                || Math.abs(getRpm().value()) >= FTuning.TURBINE_STARTER_RPM);
        float before = spool;
        if (burning) {
            power = flame / FTuning.ENGINE_REFERENCE_FLAME_TU;
            spool = Math.min(1f, spool + FTuning.TURBINE_SPOOL_UP_PER_TICK);
            owed += FTuning.TURBINE_FUEL_MB_PER_TICK * Math.max(spool, 0.1f) / power;
            int drawn = (int) owed;
            owed -= drawn;
            if (drawn > 0)
                fuel.drain(drawn, IFluidHandler.FluidAction.EXECUTE);
        } else {
            spool = Math.max(0f, spool - FTuning.TURBINE_COAST_PER_TICK);
        }
        // A band, not a continuous figure: a source is only re-read on a rebuild, which must stay rare.
        if (Math.round(before * 10) != Math.round(spool * 10) || (before > 0f) != (spool > 0f))
            RotationPropagator.rebuildFrom(level, worldPosition);
    }

    private void pull() {
        int needed = fuel.getSpace();
        if (needed <= 0)
            return;
        for (Direction face : Direction.values()) {
            IFluidHandler neighbour = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(face),
                    face.getOpposite());
            if (neighbour == null)
                continue;
            FluidStack offered = neighbour.drain(needed, IFluidHandler.FluidAction.SIMULATE);
            int fits = offered.isEmpty() ? 0 : fuel.fill(offered, IFluidHandler.FluidAction.SIMULATE);
            if (fits <= 0)
                continue;
            fuel.fill(neighbour.drain(offered.copyWithAmount(fits), IFluidHandler.FluidAction.EXECUTE),
                    IFluidHandler.FluidAction.EXECUTE);
            needed -= fits;
            if (needed <= 0)
                return;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Spool", spool);
        tag.put("Fuel", fuel.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        spool = tag.getFloat("Spool");
        if (tag.contains("Fuel"))
            fuel.readFromNBT(registries, tag.getCompound("Fuel"));
        power = fuel.isEmpty() ? 1f : Math.max(0.1f, flameOf(fuel.getFluid()) / FTuning.ENGINE_REFERENCE_FLAME_TU);
    }
}
