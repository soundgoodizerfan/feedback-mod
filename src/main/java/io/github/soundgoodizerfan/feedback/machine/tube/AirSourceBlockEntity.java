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
package io.github.soundgoodizerfan.feedback.machine.tube;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.machine.creative.CreativeSource;
import io.github.soundgoodizerfan.feedback.machine.gastank.GasTankBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.pipe.PipeBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FFluids;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Air at a set temperature and pressure, with no compressor behind it: each tick it compresses air
 * into any tube run, pipe run or gas tank beside it that reads less than the target pressure. The
 * operator-tab pressure source for tubes (not the Creative Fluid block).
 */
public class AirSourceBlockEntity extends BlockEntity implements CreativeSource {

    private boolean on = true;
    private float tu = FTuning.AMBIENT_TU.value();
    private float rate = 200f;
    private float pressure = 2f;

    public AirSourceBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CREATIVE_AIR.get(), pos, state);
    }

    public void tickServer() {
        if (level == null || !on)
            return;
        int budget = Math.round(rate);
        for (Direction d : Direction.values()) {
            if (budget <= 0)
                return;
            BlockEntity be = level.getBlockEntity(worldPosition.relative(d));
            FluidStack air = FluidHeat.stamp(new FluidStack(FFluids.AIR.get(), budget), tu);
            if (be instanceof TubeBlockEntity t && t.getPressure().value() < pressure)
                budget -= t.compressedFill(air, IFluidHandler.FluidAction.EXECUTE);
            else if (be instanceof PipeBlockEntity p && p.getPressure().value() < pressure)
                budget -= p.compressedFill(air, IFluidHandler.FluidAction.EXECUTE);
            else if (be instanceof GasTankBlockEntity g && g.getPressure().value() < pressure)
                budget -= g.compressedFill(air, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    public boolean on() {
        return on;
    }

    @Override
    public List<Param> params() {
        return List.of(new Param("temperature", tu, 50f, 0f, 10000f), new Param("flow", rate, 50f, 0f, 10000f),
                new Param("pressure", pressure, 0.5f, 0f, 20f));
    }

    @Override
    public void configure(boolean on, float[] values) {
        this.on = on;
        tu = CreativeSource.clamp(params().get(0), values[0]);
        rate = CreativeSource.clamp(params().get(1), values[1]);
        pressure = CreativeSource.clamp(params().get(2), values[2]);
        setChanged();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("On", on);
        tag.putFloat("Tu", tu);
        tag.putFloat("Rate", rate);
        tag.putFloat("Pressure", pressure);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        on = tag.getBoolean("On");
        tu = tag.getFloat("Tu");
        rate = tag.getFloat("Rate");
        pressure = tag.getFloat("Pressure");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
