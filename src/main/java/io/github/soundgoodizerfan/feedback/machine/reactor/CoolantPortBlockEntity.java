// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.reactor;

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

    private final FluidTank water = new FluidTank(ReactorTuning.REACTOR_PORT_TANK_MB, s -> s.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank steam = new FluidTank(ReactorTuning.REACTOR_PORT_TANK_MB, s -> s.is(FTags.STEAM)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    /** A liquid-metal coolant (the {@code liquid_metal_coolants} tag) comes in cold; the core heats it and it leaves hot, its temperature stamped on it. */
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid> METALS = net.minecraft.tags.FluidTags
            .create(io.github.soundgoodizerfan.feedback.Feedback.id("liquid_metal_coolants"));

    private final FluidTank metal = new FluidTank(ReactorTuning.REACTOR_PORT_TANK_MB, s -> s.is(METALS)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank hot = new FluidTank(ReactorTuning.REACTOR_PORT_TANK_MB, s -> s.is(METALS)) {
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

    /** The cold liquid-metal coolant waiting in the port, mB. */
    public int metal() {
        return metal.getFluidAmount();
    }

    /**
     * Carries {@code work} of heat off a core standing at {@code coreTu} into the liquid metal in the port: each mB that passes takes its mass
     * times its specific heat times the rise from the inlet's temperature, and leaves in the hot tank stamped at the core's temperature. Limited
     * by the metal waiting and the room in the hot tank. Returns the work taken, 0 with no coolant.
     */
    public float carry(float work, float coreTu) {
        FluidStack cold = metal.getFluid();
        if (cold.isEmpty() || work <= 0f)
            return 0f;
        var materials = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials();
        var coolant = io.github.soundgoodizerfan.feedback.process.Mixture.fluidMaterial(materials, cold.getFluid()).map(materials::get).orElse(null);
        if (coolant == null)
            return 0f;
        float inlet = io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat.of(cold).value();
        float rise = coreTu - inlet;
        float perMb = coolant.thermal().specificHeat().map(h -> h.workPerMuPerTu()).orElse(0f)
                * coolant.molten().density().orElse(coolant.density()).muPerMb() * rise;
        if (rise <= 0f || perMb <= 0f)
            return 0f;
        int mb = Math.min((int) Math.ceil(work / perMb), Math.min(metal.getFluidAmount(), hot.getSpace()));
        if (mb <= 0)
            return 0f;
        metal.drain(mb, IFluidHandler.FluidAction.EXECUTE);
        hot.fill(io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat.stamp(cold.copyWithAmount(mb), coreTu), IFluidHandler.FluidAction.EXECUTE);
        return mb * perMb;
    }

    /**
     * A water-reactive metal meeting water: sodium and water in one port. It blasts, empties every tank and the water goes with it; the core
     * calls this each tick. True if it went off.
     */
    public boolean meet(net.minecraft.server.level.ServerLevel server) {
        if (water.isEmpty() && steam.isEmpty())
            return false;
        var materials = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials();
        boolean reactive = false;
        for (FluidTank tank : new FluidTank[] {metal, hot}) {
            if (tank.isEmpty())
                continue;
            var m = io.github.soundgoodizerfan.feedback.process.Mixture.fluidMaterial(materials, tank.getFluid().getFluid()).map(materials::get).orElse(null);
            reactive |= m != null && m.traits().contains(io.github.soundgoodizerfan.feedback.process.Material.Trait.WATER_REACTIVE);
        }
        if (!reactive)
            return false;
        float power = ReactorTuning.SODIUM_WATER_BLAST * Math.min(1f, (water.getFluidAmount() + steam.getFluidAmount()) / 1000f + 0.5f);
        water.setFluid(FluidStack.EMPTY);
        steam.setFluid(FluidStack.EMPTY);
        metal.setFluid(FluidStack.EMPTY);
        hot.setFluid(FluidStack.EMPTY);
        setChanged();
        server.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, power,
                net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        return true;
    }

    /** The pipe-facing handler: water or liquid-metal coolant in; steam or hot coolant out. */
    public IFluidHandler handler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 4;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return switch (tank) {
                    case 0 -> water.getFluid();
                    case 1 -> steam.getFluid();
                    case 2 -> metal.getFluid();
                    default -> hot.getFluid();
                };
            }

            @Override
            public int getTankCapacity(int tank) {
                return ReactorTuning.REACTOR_PORT_TANK_MB;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return (tank == 0 && water.isFluidValid(stack)) || (tank == 2 && metal.isFluidValid(stack));
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return metal.isFluidValid(resource) ? metal.fill(resource, action) : water.fill(resource, action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (hot.isFluidValid(resource))
                    return hot.drain(resource, action);
                return steam.isFluidValid(resource) ? steam.drain(resource, action) : FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                FluidStack got = hot.drain(maxDrain, action);
                return got.isEmpty() ? steam.drain(maxDrain, action) : got;
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("Steam", steam.writeToNBT(registries, new CompoundTag()));
        tag.put("Metal", metal.writeToNBT(registries, new CompoundTag()));
        tag.put("Hot", hot.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water.readFromNBT(registries, tag.getCompound("Water"));
        steam.readFromNBT(registries, tag.getCompound("Steam"));
        metal.readFromNBT(registries, tag.getCompound("Metal"));
        hot.readFromNBT(registries, tag.getCompound("Hot"));
    }
}
