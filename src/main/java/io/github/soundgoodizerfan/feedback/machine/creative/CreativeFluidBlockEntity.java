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
package io.github.soundgoodizerfan.feedback.machine.creative;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Any fluid, at any temperature, with no well behind it -- or, with Sink set, a drain that
 * swallows whatever is put in. The fluid is chosen by using a bucket on the block; the rest is
 * the usual settings screen. As a source it pushes into every neighbour each tick and also lets
 * a pump draw from it. A gas can be picked by number in the settings (gas 0 is the bucket's fluid),
 * and a pressure above 0 compresses it into a gas tank or pipe run beside it until that run reaches
 * the pressure, the way a compressor would; at 0 it pours at plain flow.
 */
public class CreativeFluidBlockEntity extends BlockEntity implements CreativeSource {

    private boolean on = true;
    private float tu = FTuning.AMBIENT_TU.value();
    private float rate = 100f;
    private boolean sink;
    /** 0 pours the fluid a bucket set; n pours the nth registered gas, by name. */
    private int gas;
    /** Pu to hold a gas tank or pipe run at by compressing into it; 0 pours at plain flow. */
    private float pressure;
    private FluidStack fluid = new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1);

    private final IFluidHandler handler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return on && !sink ? stamped(1000) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 1000;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return on && sink;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return on && sink ? resource.getAmount() : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.isSameFluid(resource, pouring(1)) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return on && !sink ? stamped(maxDrain) : FluidStack.EMPTY;
        }
    };

    public CreativeFluidBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CREATIVE_FLUID.get(), pos, state);
    }

    /** Every gas fluid (a source fluid of negative density), in registry order by name. */
    private static java.util.List<net.minecraft.world.level.material.Fluid> gases;

    private static java.util.List<net.minecraft.world.level.material.Fluid> gases() {
        if (gases == null) {
            gases = net.minecraft.core.registries.BuiltInRegistries.FLUID.stream()
                    .filter(f -> f.isSource(f.defaultFluidState()) && f.getFluidType().getDensity() < 0)
                    .sorted(java.util.Comparator.comparing(f -> net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(f)))
                    .toList();
        }
        return gases;
    }

    private FluidStack pouring(int amount) {
        var list = gases();
        if (gas > 0 && gas <= list.size())
            return new FluidStack(list.get(gas - 1), amount);
        return fluid.copyWithAmount(amount);
    }

    private FluidStack stamped(int amount) {
        return FluidHeat.stamp(pouring(amount), tu);
    }

    public IFluidHandler getHandler() {
        return handler;
    }

    public void setFluid(FluidStack stack) {
        fluid = stack.copyWithAmount(1);
        setChanged();
    }

    public FluidStack getFluid() {
        return fluid;
    }

    public void tickServer() {
        if (level == null || !on || sink)
            return;
        int budget = Math.round(rate);
        for (Direction d : Direction.values()) {
            if (budget <= 0)
                return;
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(d),
                    d.getOpposite());
            BlockPos at = worldPosition.relative(d);
            if (pressure > 0f && level.getBlockEntity(at) instanceof io.github.soundgoodizerfan.feedback.machine.gastank.GasTankBlockEntity tank) {
                if (tank.getPressure().value() < pressure)
                    budget -= tank.compressedFill(stamped(budget), IFluidHandler.FluidAction.EXECUTE);
            } else if (pressure > 0f && level.getBlockEntity(at) instanceof io.github.soundgoodizerfan.feedback.machine.pipe.PipeBlockEntity pipe) {
                if (pipe.getPressure().value() < pressure)
                    budget -= pipe.compressedFill(stamped(budget), IFluidHandler.FluidAction.EXECUTE);
            } else if (target != null)
                budget -= target.fill(stamped(budget), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    public boolean on() {
        return on;
    }

    @Override
    public List<Param> params() {
        return List.of(new Param("temperature", tu, 50f, 0f, 10000f), new Param("flow", rate, 50f, 0f, 10000f),
                new Param("sink", sink ? 1f : 0f, 1f, 0f, 1f),
                new Param("gas", gas, 1f, 0f, Math.max(1, gases().size())), new Param("pressure", pressure, 1f, 0f, 20f));
    }

    @Override
    public void configure(boolean on, float[] values) {
        this.on = on;
        this.tu = CreativeSource.clamp(params().get(0), values[0]);
        this.rate = CreativeSource.clamp(params().get(1), values[1]);
        this.sink = values[2] >= 0.5f;
        this.gas = Math.round(CreativeSource.clamp(params().get(3), values[3]));
        this.pressure = CreativeSource.clamp(params().get(4), values[4]);
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
        tag.putBoolean("Sink", sink);
        tag.putInt("Gas", gas);
        tag.putFloat("Pressure", pressure);
        tag.put("Fluid", fluid.saveOptional(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        on = tag.getBoolean("On");
        tu = tag.getFloat("Tu");
        rate = tag.getFloat("Rate");
        sink = tag.getBoolean("Sink");
        gas = tag.getInt("Gas");
        pressure = tag.getFloat("Pressure");
        FluidStack loaded = FluidStack.parseOptional(registries, tag.getCompound("Fluid"));
        if (!loaded.isEmpty())
            fluid = loaded;
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
