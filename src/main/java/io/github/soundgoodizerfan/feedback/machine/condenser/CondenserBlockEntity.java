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
package io.github.soundgoodizerfan.feedback.machine.condenser;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Catchup;
import io.github.soundgoodizerfan.feedback.core.thermal.FireSpan;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * A reverse boiler: steam in, water out, and the heat the boiler spent making the steam comes
 * back into this block's own body.
 *
 * <h2>Why it has a temperature at all</h2>
 * Condensing gives up exactly the {@link FTuning#BOILER_WORK_PER_MB_STEAM} the boiler paid, so
 * every mB condensed warms this {@link ThermalBody}, and it condenses only while it is below
 * {@link FTuning#BOILER_WORKING_TU} -- a condenser at boiling point condenses nothing. Left to its
 * own leak it settles at a trickle. Anything that takes heat out of a {@code ThermalBody} speeds it
 * up, and nothing here names one: a Heat Exchanger against a cold body works, and a Heat Exchanger
 * against the <em>boiler</em> hands the heat back to where it came from. Nobody wrote that loop.
 * <p>
 * It takes Steam or spent steam by tag (condensing live steam is allowed, and wasteful), pulling
 * from neighbours the way the engine pulls, and offers its water to anything that drains it -- a
 * pump, a bucket, a pipe.
 */
public class CondenserBlockEntity extends BlockEntity implements ThermalBody {

    private float temperature = FTuning.AMBIENT_TU.value();
    /** Away-time bookkeeping: see {@link Catchup}. */
    private final Catchup offline = new Catchup();

    private final FluidTank steamTank = new FluidTank(FTuning.CONDENSER_TANK_MB,
            stack -> stack.is(FTags.STEAM) || stack.is(FTags.SPENT_STEAM)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank waterTank = new FluidTank(FTuning.CONDENSER_TANK_MB) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public CondenserBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CONDENSER.get(), pos, state);
    }

    public FluidTank getSteamTank() {
        return steamTank;
    }

    public FluidTank getWaterTank() {
        return waterTank;
    }

    @Override
    public Tu getTemperature() {
        return new Tu(temperature);
    }

    @Override
    public void setTemperature(Tu tu) {
        temperature = tu.value();
    }

    @Override
    public ThermalMass getThermalMass() {
        return FTuning.CONDENSER_MASS;
    }

    @Override
    public Conductance getLeak() {
        return FTuning.CONDENSER_LEAK;
    }

    public void tickServer() {
        if (level == null)
            return;
        pull();

        Tu ambient = AmbientTable.at(level);
        long away = offline.take(level);
        if (away > 0L)
            Catchup.settle(this, FireSpan.NONE, ambient, away);
        Heat.tick(this, ambient, ambient, false);

        // Only the room left below boiling can take latent heat, and taking it spends that room.
        int affordable = (int) ((FTuning.BOILER_WORKING_TU.value() - temperature)
                * FTuning.CONDENSER_MASS.workPerTu() / FTuning.BOILER_WORK_PER_MB_STEAM);
        int amount = Math.min(Math.min(FTuning.CONDENSER_MB_PER_TICK, affordable),
                Math.min(steamTank.getFluidAmount(), waterTank.getSpace()));
        setChanged();
        if (amount <= 0)
            return;
        steamTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        waterTank.fill(new FluidStack(Fluids.WATER, amount), IFluidHandler.FluidAction.EXECUTE);
        Heat.warm(this, amount * FTuning.BOILER_WORK_PER_MB_STEAM);
    }

    /** The engine's pull, for condensable steam. See {@code SteamEngineBlockEntity#pull}. */
    private void pull() {
        int needed = steamTank.getSpace();
        for (Direction face : Direction.values()) {
            if (needed <= 0)
                return;
            IFluidHandler neighbour = level.getCapability(Capabilities.FluidHandler.BLOCK,
                    worldPosition.relative(face), face.getOpposite());
            if (neighbour == null)
                continue;
            FluidStack offered = neighbour.drain(needed, IFluidHandler.FluidAction.SIMULATE);
            int fits = offered.isEmpty() ? 0 : steamTank.fill(offered, IFluidHandler.FluidAction.SIMULATE);
            if (fits <= 0)
                continue;
            FluidStack taken = neighbour.drain(offered.copyWithAmount(fits), IFluidHandler.FluidAction.EXECUTE);
            needed -= steamTank.fill(taken, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** Steam in, water out -- the boiler's handler the other way round. */
    public IFluidHandler getFluidHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 2;
            }

            @Override
            public FluidStack getFluidInTank(int t) {
                return t == 0 ? steamTank.getFluid() : waterTank.getFluid();
            }

            @Override
            public int getTankCapacity(int t) {
                return t == 0 ? steamTank.getCapacity() : waterTank.getCapacity();
            }

            @Override
            public boolean isFluidValid(int t, FluidStack stack) {
                return t == 0 && steamTank.isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return steamTank.fill(resource, action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return waterTank.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return waterTank.drain(maxDrain, action);
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.put("SteamTank", steamTank.writeToNBT(registries, new CompoundTag()));
        tag.put("WaterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        offline.save(tag, level);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        steamTank.readFromNBT(registries, tag.getCompound("SteamTank"));
        waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        offline.load(tag);
    }
}
