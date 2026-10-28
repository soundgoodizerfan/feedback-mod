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
package io.github.soundgoodizerfan.feedback.core.fluid;

import java.util.HashSet;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureBurst;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank;
import io.github.soundgoodizerfan.feedback.machine.pipe.PipeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * One connected run of pipes, with one shared fluid ledger -- the {@code RotationNetwork} idea
 * applied to fluid ({@code spec_petrochemistry.md} 5.2). The fluid belongs to the run, not to any
 * pipe, so a long pipe is not a chain of tanks equalising one block at a time.
 * <p>
 * The ledger is a {@link HeatedTank}, so temperature merges by the one rule every tank uses and a
 * hot line is as hot as what was put in. Capacity is fixed at construction because any change to
 * membership builds a new network ({@link FluidNetworks#flush}).
 *
 * <h2>Flow</h2>
 * Pumps fill the run and draw from it through the pipes' ordinary handlers. The run itself only
 * pushes outward: each tick it offers up to {@link FTuning#PIPE_FLOW_MB_PER_TICK} to whatever
 * handler touches it, valves and tanks included. It never pulls from a neighbour.
 */
public class FluidNetwork {

    public final Set<PipeBlockEntity> members = new HashSet<>();
    public final HeatedTank ledger;
    private boolean changed;

    /** Liquid volume of the run; a gas may be compressed to {@link FTuning#PIPE_COMPRESSION_HEADROOM} times it. */
    private final int volume;

    FluidNetwork(int size) {
        volume = size * FTuning.PIPE_VOLUME_MB;
        ledger = new HeatedTank(volume * FTuning.PIPE_COMPRESSION_HEADROOM, () -> changed = true);
    }

    public static boolean isGas(FluidStack stack) {
        return stack.getFluidType().getDensity() < 0;
    }

    public int volume() {
        return volume;
    }

    /** Gas pressure: what is held over the run's volume. A liquid run reads zero (no thermal expansion yet). */
    public Pu pressure() {
        return new Pu(ledger.isEmpty() || !isGas(ledger.getFluid()) ? 0f
                : (float) ledger.getFluidAmount() / volume * FTuning.PIPE_PU_PER_VOLUME);
    }

    /**
     * Takes up to what the run holds: a liquid up to its volume, a gas up to its volume too unless
     * {@code compressed}, when it may be forced up to the headroom. Passive flow from a well or a
     * neighbouring tank never raises a run past one volume's worth; that takes a compressor.
     */
    public int accept(FluidStack resource, boolean compressed, IFluidHandler.FluidAction action) {
        int limit = compressed && isGas(resource) ? ledger.getCapacity() : volume;
        int room = Math.max(0, limit - ledger.getFluidAmount());
        return room <= 0 ? 0 : ledger.fill(resource.copyWithAmount(Math.min(room, resource.getAmount())), action);
    }

    void tick(Level level) {
        if (ledger.isEmpty())
            return;
        if (hazard(level))
            return;
        int budget = isGas(ledger.getFluid())
                ? Math.min(FTuning.PIPE_FLOW_MB_PER_TICK, Math.max(1, Math.round(pressure().value() * FTuning.PIPE_GAS_FLOW_PER_PU)))
                : FTuning.PIPE_FLOW_MB_PER_TICK;
        for (PipeBlockEntity pipe : members) {
            for (Direction d : Direction.values()) {
                if (budget <= 0 || ledger.isEmpty())
                    return;
                BlockPos beyond = pipe.getBlockPos().relative(d);
                if (!level.isLoaded(beyond) || level.getBlockEntity(beyond) instanceof PipeBlockEntity)
                    continue;
                IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, beyond, d.getOpposite());
                if (target == null)
                    continue;
                FluidStack offered = ledger.drain(budget, IFluidHandler.FluidAction.SIMULATE);
                int fits = target.fill(offered, IFluidHandler.FluidAction.SIMULATE);
                if (fits > 0)
                    budget -= target.fill(ledger.drain(fits, IFluidHandler.FluidAction.EXECUTE),
                            IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    /** A run held over the vessel danger line lets go the way a boiler does (spec 5.5): the lost fluid goes with it. True if it burst. */
    private boolean hazard(Level level) {
        if (level.getGameTime() % 10 != 0 || members.isEmpty()
                || !PressureBurst.fails(pressure(), FTuning.PRESSURE_VESSEL_DANGER_PU, FTuning.PRESSURE_VESSEL_CRITICAL_PU, level.random))
            return false;
        PipeBlockEntity weak = members.stream().skip(level.random.nextInt(members.size())).findFirst().get();
        ledger.setFluid(FluidStack.EMPTY);
        PressureBurst.burst(level, weak.getBlockPos(), FTuning.PIPE_BURST_POWER);
        return true;
    }
}
