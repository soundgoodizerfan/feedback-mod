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
package io.github.soundgoodizerfan.feedback.machine.steamengine;

import io.github.soundgoodizerfan.feedback.registry.FTags;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Steam in, Su out, and nothing else -- see {@code FTuning}'s {@code --- the boiler ---} section
 * for why this deliberately does not know what a Tu is, or what made the Steam it burns.
 * <p>
 * A plain {@link RotationNode} with one input tank. It has no opinion about what fills that tank
 * -- a boiler sitting against it, a hopper, a pipe a later mod adds -- because {@link #pull} asks
 * every neighbour's {@link Capabilities.FluidHandler} for Steam rather than reading a specific
 * block type the way {@code HeatSource.below} reads a firebox. That is the whole of the
 * puzzle-piece argument: this block cannot tell a coal-fired boiler from an electric one, on
 * purpose.
 * <p>
 * What it burns comes out as {@code spent_steam} in an exhaust tank that a neighbour may drain
 * (a condenser pulls it). A full exhaust vents to the air: the engine keeps running and the
 * water is simply lost, so condensing is a way to keep water, never a gate on running.
 */
public class SteamEngineBlockEntity extends RotationNode {

    private final FluidTank tank = new FluidTank(FTuning.STEAM_ENGINE_TANK_MB,
            stack -> stack.is(FTags.STEAM)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank exhaust = new FluidTank(FTuning.STEAM_ENGINE_TANK_MB,
            stack -> stack.is(FTags.SPENT_STEAM)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private boolean working;

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.STEAM_ENGINE.get(), pos, state);
    }

    public FluidTank getTank() {
        return tank;
    }

    public FluidTank getExhaust() {
        return exhaust;
    }

    /**
     * What the outside world sees: Steam in, exhaust out. Exposing the steam tank itself let two
     * engines side by side drain each other through {@link #pull}, steam sloshing back and forth
     * between them; the exhaust is not Steam, so a neighbouring engine never pulls it.
     */
    public IFluidHandler getInput() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 2;
            }

            @Override
            public FluidStack getFluidInTank(int t) {
                return t == 0 ? tank.getFluid() : exhaust.getFluid();
            }

            @Override
            public int getTankCapacity(int t) {
                return t == 0 ? tank.getCapacity() : exhaust.getCapacity();
            }

            @Override
            public boolean isFluidValid(int t, FluidStack stack) {
                return t == 0 && tank.isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return tank.fill(resource, action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return exhaust.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return exhaust.drain(maxDrain, action);
            }
        };
    }

    // --- rotation -----------------------------------------------------------------------------

    @Override
    public Rpm getGeneratedRpm() {
        return working ? FTuning.STEAM_ENGINE_RPM : ZERO_RPM;
    }

    @Override
    public Su getCapacitySu() {
        return working ? FTuning.STEAM_ENGINE_CAPACITY_SU : NO_SU;
    }

    @Override
    public Inertia getInertia() {
        return FTuning.STEAM_ENGINE_INERTIA;
    }

    // --- the tick ---------------------------------------------------------------------------

    public void tickServer() {
        if (level == null)
            return;

        pull();

        boolean nowWorking = tank.getFluidAmount() >= FTuning.STEAM_ENGINE_MB_PER_TICK;
        if (nowWorking) {
            tank.drain(FTuning.STEAM_ENGINE_MB_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
            // Whatever does not fit vents: lost water, not a stalled engine.
            exhaust.fill(new FluidStack(FFluids.SPENT_STEAM.get(), FTuning.STEAM_ENGINE_MB_PER_TICK),
                    IFluidHandler.FluidAction.EXECUTE);
        }

        if (nowWorking != working) {
            working = nowWorking;
            RotationPropagator.rebuildFrom(level, worldPosition);
        }
    }

    /**
     * Top the tank up from whatever neighbouring block exposes a fluid handler with Steam in it.
     * There is no pipe system yet, so this is the interim transport layer -- a machine that
     * reaches for what it needs rather than waiting for something to push it. A real pipe mod
     * plugs into the same {@link Capabilities.FluidHandler} capability this reads and this loop
     * becomes redundant rather than wrong, so it costs nothing to have written now.
     */
    private void pull() {
        int needed = tank.getSpace();
        if (needed <= 0)
            return;
        for (Direction face : Direction.values()) {
            IFluidHandler neighbour = level.getCapability(Capabilities.FluidHandler.BLOCK,
                    worldPosition.relative(face), face.getOpposite());
            if (neighbour == null)
                continue;
            // Whatever the neighbour offers, if it is steam by tag -- any mod's steam will do.
            FluidStack drained = neighbour.drain(needed, IFluidHandler.FluidAction.SIMULATE);
            if (drained.isEmpty() || !drained.is(FTags.STEAM) || tank.fill(drained, IFluidHandler.FluidAction.SIMULATE) <= 0)
                continue;
            drained.setAmount(tank.fill(drained, IFluidHandler.FluidAction.SIMULATE));
            FluidStack taken = neighbour.drain(drained, IFluidHandler.FluidAction.EXECUTE);
            tank.fill(taken, IFluidHandler.FluidAction.EXECUTE);
            needed -= taken.getAmount();
            if (needed <= 0)
                return;
        }
    }

    // --- persistence ------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.put("Exhaust", exhaust.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Tank"))
            tank.readFromNBT(registries, tag.getCompound("Tank"));
        if (tag.contains("Exhaust"))
            exhaust.readFromNBT(registries, tag.getCompound("Exhaust"));
        working = tank.getFluidAmount() >= FTuning.STEAM_ENGINE_MB_PER_TICK;
    }
}
