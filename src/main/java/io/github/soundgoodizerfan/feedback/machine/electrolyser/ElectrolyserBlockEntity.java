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
package io.github.soundgoodizerfan.feedback.machine.electrolyser;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.process.Electrolysis;
import io.github.soundgoodizerfan.feedback.process.ElectrolysisTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Runs current through a liquid and collects what comes out at each electrode. It knows no liquid by name: any
 * fluid with a row in the {@code electrolysis} table can be fed in, the row gives the voltage it comes apart at and
 * what leaves at the anode (the front face) and the cathode (the back face), and every other face takes the feed.
 * Current is the rate and the row's voltage the gate; with the feed out or an electrode's tank full it asks for no
 * current. Voltage over the rating costs condition, never uptime.
 * <p>
 * shortcut: one fluid per electrode tank, so a row with two products at one electrode makes only the first that
 * fits; upgrade when a row needs it.
 */
public class ElectrolyserBlockEntity extends ElectricNode {

    private final FluidTank feed = new FluidTank(FTuning.ELECTROLYSER_FEED_MB,
            f -> ElectrolysisTable.get().of(f).isPresent());
    private final FluidTank anode = new FluidTank(FTuning.ELECTROLYSER_GAS_MB);
    private final FluidTank cathode = new FluidTank(FTuning.ELECTROLYSER_GAS_MB);
    private float condition = 1f;
    private float owed;

    public ElectrolyserBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ELECTROLYSER.get(), pos, state);
    }

    public float condition() {
        return condition;
    }

    private FluidTank tank(Electrolysis.Electrode electrode) {
        return electrode == Electrolysis.Electrode.ANODE ? anode : cathode;
    }

    /** Whole millibuckets of feed the electrode tanks have room for, given what {@code row} makes. */
    private int roomFor(Electrolysis row) {
        int room = Integer.MAX_VALUE;
        for (Electrolysis.Product p : row.products()) {
            FluidTank t = tank(p.electrode());
            FluidStack made = new FluidStack(BuiltInRegistries.FLUID.get(p.fluid()), 1);
            room = Math.min(room, t.isEmpty() || FluidStack.isSameFluid(t.getFluid(), made) ? t.getSpace() / p.mb() : 0);
        }
        return room;
    }

    // --- sink ---------------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.LV;
    }

    @Override
    public float wantedAmps() {
        var row = ElectrolysisTable.get().of(feed.getFluid());
        return row.isPresent() && feed.getFluidAmount() > 0 && roomFor(row.get()) > 0 ? FTuning.ELECTROLYSER_AMPS : 0f;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        var row = ElectrolysisTable.get().of(feed.getFluid());
        if (row.isPresent() && amps > 0f && volts >= row.get().volts()) {
            owed += amps * FTuning.ELECTROLYSER_MB_PER_AMP_TICK * condition;
            int used = Math.min((int) owed, Math.min(feed.getFluidAmount(), roomFor(row.get())));
            if (used > 0) {
                owed -= used;
                feed.drain(used, IFluidHandler.FluidAction.EXECUTE);
                for (Electrolysis.Product p : row.get().products())
                    tank(p.electrode()).fill(new FluidStack(BuiltInRegistries.FLUID.get(p.fluid()), used * p.mb()),
                            IFluidHandler.FluidAction.EXECUTE);
                setChanged();
            }
        }
        float excess = ElectricRules.overVoltage(volts, ratedBand());
        if (excess > 0f) {
            condition = Math.max(FTuning.ELECTROLYSER_CONDITION_FLOOR,
                    condition - FTuning.ELECTROLYSER_WEAR_PER_EXCESS * excess);
            setChanged();
        }
    }

    // --- fluid faces --------------------------------------------------------------------------

    private Direction front() {
        return getBlockState().getValue(ElectrolyserBlock.FACING);
    }

    /** What a neighbour on {@code side} sees: the front gives the anode's product, the back the cathode's, any other face takes the feed. */
    public IFluidHandler port(Direction side) {
        if (side == front())
            return out(anode);
        if (side == front().getOpposite())
            return out(cathode);
        return feed;
    }

    private static IFluidHandler out(FluidTank tank) {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int t) {
                return tank.getFluid();
            }

            @Override
            public int getTankCapacity(int t) {
                return tank.getCapacity();
            }

            @Override
            public boolean isFluidValid(int t, FluidStack stack) {
                return false;
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return 0;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return tank.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return tank.drain(maxDrain, action);
            }
        };
    }

    /** Pushes each product into whatever sits on its face; a pipe or tank takes it, a dish pulls it itself. */
    public void tickServer() {
        if (level == null)
            return;
        push(front(), anode);
        push(front().getOpposite(), cathode);
    }

    private void push(Direction face, FluidTank tank) {
        if (tank.isEmpty())
            return;
        IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(face),
                face.getOpposite());
        if (target != null) {
            int moved = target.fill(tank.getFluid(), IFluidHandler.FluidAction.EXECUTE);
            if (moved > 0) {
                tank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
                setChanged();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Feed", feed.writeToNBT(registries, new CompoundTag()));
        tag.put("Anode", anode.writeToNBT(registries, new CompoundTag()));
        tag.put("Cathode", cathode.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        feed.readFromNBT(registries, tag.getCompound("Feed"));
        anode.readFromNBT(registries, tag.getCompound("Anode"));
        cathode.readFromNBT(registries, tag.getCompound("Cathode"));
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }
}
