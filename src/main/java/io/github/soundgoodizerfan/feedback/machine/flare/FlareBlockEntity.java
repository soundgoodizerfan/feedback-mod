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
package io.github.soundgoodizerfan.feedback.machine.flare;

import io.github.soundgoodizerfan.feedback.core.FTuning;
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

    private final HeatedTank gas = new HeatedTank(FTuning.FLARE_TANK_MB, stack -> stack.getFluidType().getDensity() < 0,
            this::setChanged);

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
            gas.drain(FTuning.FLARE_MB_PER_TICK, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
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
