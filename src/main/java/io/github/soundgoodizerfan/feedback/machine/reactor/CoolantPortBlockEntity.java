// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.reactor;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The core-side coolant port: a wall block of the reactor shell with two tanks. Water goes in by pipe or bucket and the core
 * feeds its coolant channels from it; the steam the channels make comes out the same block for a pipe to carry to a turbine.
 * It ticks nothing; the core drives it.
 */
public class CoolantPortBlockEntity extends BlockEntity {

    private final FluidTank water = new FluidTank(FTuning.REACTOR_PORT_TANK_MB, s -> s.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank steam = new FluidTank(FTuning.REACTOR_PORT_TANK_MB, s -> s.is(FTags.STEAM)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public CoolantPortBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.COOLANT_PORT.get(), pos, state);
    }

    public int water() {
        return water.getFluidAmount();
    }

    public int steamSpace() {
        return steam.getSpace();
    }

    public int steam() {
        return steam.getFluidAmount();
    }

    /** Takes {@code mb} of water out and puts {@code mb} of {@code gas} in (stamped by the caller); returns the mB that moved. */
    public int boil(int mb, FluidStack gas) {
        int moved = Math.min(mb, Math.min(water.getFluidAmount(), steam.getSpace()));
        if (moved <= 0)
            return 0;
        water.drain(moved, IFluidHandler.FluidAction.EXECUTE);
        steam.fill(gas.copyWithAmount(moved), IFluidHandler.FluidAction.EXECUTE);
        return moved;
    }

    /** The pipe-facing handler: water in, steam out. */
    public IFluidHandler handler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 2;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return tank == 0 ? water.getFluid() : steam.getFluid();
            }

            @Override
            public int getTankCapacity(int tank) {
                return FTuning.REACTOR_PORT_TANK_MB;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return tank == 0 && water.isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return water.fill(resource, action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return steam.isFluidValid(resource) ? steam.drain(resource, action) : FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return steam.drain(maxDrain, action);
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("Steam", steam.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water.readFromNBT(registries, tag.getCompound("Water"));
        steam.readFromNBT(registries, tag.getCompound("Steam"));
    }
}
