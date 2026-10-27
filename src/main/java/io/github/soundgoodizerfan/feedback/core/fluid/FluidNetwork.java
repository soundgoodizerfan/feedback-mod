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

    FluidNetwork(int size) {
        ledger = new HeatedTank(size * FTuning.PIPE_VOLUME_MB, () -> changed = true);
    }

    void tick(Level level) {
        if (ledger.isEmpty())
            return;
        int budget = FTuning.PIPE_FLOW_MB_PER_TICK;
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
}
