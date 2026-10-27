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
package io.github.soundgoodizerfan.feedback.machine.pipe;

import io.github.soundgoodizerfan.feedback.core.fluid.FluidNetwork;
import io.github.soundgoodizerfan.feedback.core.fluid.FluidNetworks;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * One segment of a pipe run. It holds no fluid: the whole run's ledger is on its
 * {@link FluidNetwork}, and this block only joins the run and answers handlers for it, so pumps,
 * valves and tanks reach the run through any segment.
 */
public class PipeBlockEntity extends BlockEntity {

    @Nullable
    public FluidNetwork network;

    private boolean unloaded;

    private final IFluidHandler handler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return network == null ? FluidStack.EMPTY : network.ledger.getFluidInTank(0);
        }

        @Override
        public int getTankCapacity(int tank) {
            return network == null ? 0 : network.ledger.getTankCapacity(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return network != null;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return network == null ? 0 : network.ledger.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return network == null ? FluidStack.EMPTY : network.ledger.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return network == null ? FluidStack.EMPTY : network.ledger.drain(maxDrain, action);
        }
    };

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.PIPE.get(), pos, state);
    }

    public IFluidHandler getHandler() {
        return handler;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide)
            FluidNetworks.of(level).queue(worldPosition);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        unloaded = true;
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            if (unloaded)
                FluidNetworks.of(level).unloaded(this);
            else
                FluidNetworks.of(level).removed(this);
        }
        super.setRemoved();
    }
}
