// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.flare;

import io.github.soundgoodizerfan.feedback.machine.column.PetrochemTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Burns off gas at a fixed rate (spec 8.3): the safe vent for gas nobody wants. While it burns the
 * block is lit, which makes it a light and an ignition source (a lit block is one to {@code
 * HotIgnition}). A line that outruns it fills the tank and then backs up, not into the flare.
 */
public class FlareBlockEntity extends BlockEntity {

    private final HeatedTank gas = new HeatedTank(PetrochemTuning.FLARE_TANK_MB, this::setChanged);

    public FlareBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.FLARE.get(), pos, state);
    }

    public HeatedTank getGas() {
        return gas;
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server))
            return;
        boolean lit = !gas.isEmpty();
        if (lit)
            gas.drain(PetrochemTuning.FLARE_MB_PER_TICK, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        BlockState state = getBlockState();
        if (state.getValue(FlareBlock.LIT) != lit)
            server.setBlock(worldPosition, state.setValue(FlareBlock.LIT, lit), 3);
        if (lit && server.getGameTime() % 4 == 0)
            server.sendParticles(ParticleTypes.FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5, 2, 0.1, 0.2, 0.1, 0.02);
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
