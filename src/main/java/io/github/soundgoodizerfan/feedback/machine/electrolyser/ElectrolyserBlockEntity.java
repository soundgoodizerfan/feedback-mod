// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.electrolyser;

import io.github.soundgoodizerfan.feedback.core.hazard.HazardTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.hazard.Corrosion;
import io.github.soundgoodizerfan.feedback.core.hazard.Fumes;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.process.Electrolysis;
import io.github.soundgoodizerfan.feedback.process.ProcessRun;
import io.github.soundgoodizerfan.feedback.process.ThermalProcessTable;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Runs current through a liquid and collects what comes out at each electrode. It knows no liquid by name: any
 * fluid with a row in the {@code electrolysis} table can be fed in, the row gives the voltage it comes apart at and
 * what leaves at the anode (the front face) and the cathode (the back face), and every other face takes the feed and
 * gives back the residue the cell leaves behind (lye, behind a splitting brine). Current is the rate and the row's
 * voltage the gate; with the feed out or an electrode's tank full it asks for no current. Voltage over the rating
 * costs condition, never uptime.
 * <p>
 * A row with an {@code item} splits that item dissolved in the liquid, which is then only a bath. A row with a
 * {@code min_temperature} runs only once the cell is that hot; the cell is a {@link ThermalBody} that the current
 * itself heats, so a cold cell given current warms until the bath melts and holds its heat while it works. A bath
 * row with no solute left is an anode effect: the bath breaks down, fumes escape and the cell loses condition.
 * <p>
 * shortcut: one fluid per electrode tank, so a row with two products at one electrode makes only the first that
 * fits; upgrade when a row needs it.
 */
public class ElectrolyserBlockEntity extends ElectricNode implements ThermalBody {

    private final FluidTank feed = new FluidTank(ElectricMachineTuning.ELECTROLYSER_FEED_MB,
            f -> ThermalProcessTable.get().electrolysisOf(f).isPresent());
    private final FluidTank liquor = new FluidTank(ElectricMachineTuning.ELECTROLYSER_FEED_MB);
    private final FluidTank anode = new FluidTank(ElectricMachineTuning.ELECTROLYSER_GAS_MB);
    private final FluidTank cathode = new FluidTank(ElectricMachineTuning.ELECTROLYSER_GAS_MB);
    private final ItemStackHandler solute = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return ThermalProcessTable.get().electrolyses().stream().anyMatch(r -> r.takes(stack));
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** What plates out on the cathode (a won metal), taken off by hand or by a hopper at the back face. */
    private final ItemStackHandler plate = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** Units of work plated toward the next deposit item. */
    private int plated;
    private float condition = 1f;
    private float owed;
    /** The fraction of a residue millibucket not yet made, for a row that leaves less than one per unit. */
    private float residueOwed;
    private float unitsLeft;
    private float bathLoss;
    private float temperature = ThermalTuning.AMBIENT_TU.value();

    public ElectrolyserBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ELECTROLYSER.get(), pos, state);
    }

    public float condition() {
        return condition;
    }

    /** The slot a solute goes into (a hopper or a hand). */
    public ItemStackHandler solute() {
        return solute;
    }

    /** The cathode plate: what a won metal grows into. */
    public ItemStackHandler plate() {
        return plate;
    }

    /** The item handler a neighbour on {@code side} sees: the back face (the cathode) gives the plate, the rest take solute. */
    public ItemStackHandler itemPort(@javax.annotation.Nullable Direction side) {
        return side != null && side == front().getOpposite() ? plate : solute;
    }

    /** Items the plate can still grow by, as units of work. */
    private int plateRoom(Electrolysis.Deposit d) {
        ItemStack held = plate.getStackInSlot(0);
        ItemStack next = d.stack();
        if (!held.isEmpty() && !ItemStack.isSameItemSameComponents(held, next))
            return 0;
        int items = next.getMaxStackSize() - held.getCount();
        return Math.max(0, items * d.units() - plated);
    }

    private FluidTank tank(Electrolysis.Electrode electrode) {
        return electrode == Electrolysis.Electrode.ANODE ? anode : cathode;
    }

    private static FluidStack stackOf(net.minecraft.resources.ResourceLocation id, int mb) {
        return new FluidStack(BuiltInRegistries.FLUID.get(id), mb);
    }

    /** Whole units of work the product and residue tanks have room for, given what {@code row} makes. */
    private int roomFor(Electrolysis row) {
        int room = Integer.MAX_VALUE;
        for (Electrolysis.Product p : row.products()) {
            room = Math.min(room, ProcessRun.room(tank(p.electrode()), stackOf(p.fluid(), 1)) / p.mb());
        }
        if (row.residue().isPresent()) {
            var r = row.residue().get();
            room = Math.min(room, (int) ((long) ProcessRun.room(liquor, stackOf(r.fluid(), 1)) * r.per() / r.mb()));
        }
        if (row.deposit().isPresent())
            room = Math.min(room, plateRoom(row.deposit().get()));
        return room;
    }

    /** Whether the cell holds enough feed (or bath) for {@code row} to run. */
    private boolean fed(Electrolysis row) {
        return feed.getFluidAmount() >= (row.solvent() ? ElectricMachineTuning.ELECTROLYSER_SOLVENT_MIN_MB : 1);
    }

    /** Whether a bath row still has solute, in the work already melted in or in the slot. */
    private boolean hasSolute(Electrolysis row) {
        return unitsLeft >= 1f || row.takes(solute.getStackInSlot(0));
    }

    // --- sink ---------------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.MV;
    }

    @Override
    public float wantedAmps() {
        var row = ThermalProcessTable.get().electrolysisOf(feed.getFluid());
        if (row.isEmpty() || !fed(row.get()))
            return 0f;
        boolean hot = temperature >= row.get().minTemperature();
        // A cold cell takes current to warm up; a hot one only while it can store what it makes.
        return !hot || roomFor(row.get()) > 0 ? ElectricMachineTuning.ELECTROLYSER_AMPS : 0f;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        var found = ThermalProcessTable.get().electrolysisOf(feed.getFluid());
        if (found.isPresent() && amps > 0f) {
            Electrolysis row = found.get();
            Heat.warm(this, volts * amps);
            if (volts >= row.volts() && temperature >= row.minTemperature())
                electrolyse(row, amps);
        }
        float excess = ElectricRules.overVoltage(volts, ratedBand());
        if (excess > 0f)
            wear(ElectricMachineTuning.ELECTROLYSER_WEAR_PER_EXCESS * excess);
    }

    private void wear(float amount) {
        condition = Math.max(ElectricMachineTuning.ELECTROLYSER_CONDITION_FLOOR, condition - amount);
        setChanged();
    }

    private void electrolyse(Electrolysis row, float amps) {
        owed = Math.min(4f, owed + amps * ElectricMachineTuning.ELECTROLYSER_MB_PER_AMP_TICK * condition);
        int units = Math.min((int) owed, roomFor(row));
        if (row.item().isPresent()) {
            while (unitsLeft < units && row.takes(solute.getStackInSlot(0))) {
                solute.extractItem(0, 1, false);
                unitsLeft += row.unitsPerItem();
            }
            if (!hasSolute(row)) {
                anodeEffect();
                return;
            }
            units = Math.min(units, (int) unitsLeft);
        }
        if (!row.solvent())
            units = Math.min(units, feed.getFluidAmount());
        if (units <= 0)
            return;
        owed -= units;
        if (row.item().isPresent())
            unitsLeft -= units;
        if (!row.solvent())
            feed.drain(units, IFluidHandler.FluidAction.EXECUTE);
        for (Electrolysis.Product p : row.products())
            ProcessRun.deposit(java.util.List.of(tank(p.electrode())), stackOf(p.fluid(), units * p.mb()));
        if (row.residue().isPresent()) {
            var r = row.residue().get();
            residueOwed += units * (float) r.mb() / r.per();
            int whole = (int) residueOwed;
            residueOwed -= whole;
            if (whole > 0)
                ProcessRun.deposit(java.util.List.of(liquor), stackOf(r.fluid(), whole));
        }
        if (row.deposit().isPresent()) {
            var d = row.deposit().get();
            plated += units;
            int grown = plated / d.units();
            if (grown > 0) {
                plated -= grown * d.units();
                ItemStack add = d.stack();
                add.setCount(grown);
                ItemStack held = plate.getStackInSlot(0);
                if (held.isEmpty())
                    plate.setStackInSlot(0, add);
                else
                    held.grow(grown);
            }
        }
        setChanged();
    }

    /** A bath with nothing dissolved in it: the bath itself breaks down, fumes escape, the cell wears. */
    private void anodeEffect() {
        wear(ElectricMachineTuning.ELECTROLYSER_ANODE_EFFECT_WEAR);
        bathLoss += ElectricMachineTuning.ELECTROLYSER_ANODE_EFFECT_BATH_MB;
        int lost = (int) bathLoss;
        if (lost > 0) {
            bathLoss -= lost;
            feed.drain(lost, IFluidHandler.FluidAction.EXECUTE);
        }
        if (level != null && level.getGameTime() % 20 == 0)
            Fumes.release(level, Vec3.atCenterOf(worldPosition), 3);
    }

    // --- fluid faces --------------------------------------------------------------------------

    private Direction front() {
        return getBlockState().getValue(ElectrolyserBlock.FACING);
    }

    /**
     * What a neighbour on {@code side} sees: the front gives the anode's product, the back the cathode's, any other
     * face fills the feed and drains the residue (then the feed).
     */
    public IFluidHandler port(Direction side) {
        if (side == front())
            return out(anode);
        if (side == front().getOpposite())
            return out(cathode);
        return sides;
    }

    private final IFluidHandler sides = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int t) {
            return t == 0 ? feed.getFluid() : liquor.getFluid();
        }

        @Override
        public int getTankCapacity(int t) {
            return t == 0 ? feed.getCapacity() : liquor.getCapacity();
        }

        @Override
        public boolean isFluidValid(int t, FluidStack stack) {
            return t == 0 && feed.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return feed.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack got = liquor.drain(resource, action);
            return got.isEmpty() ? feed.drain(resource, action) : got;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack got = liquor.drain(maxDrain, action);
            return got.isEmpty() ? feed.drain(maxDrain, action) : got;
        }
    };

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

    /**
     * Pushes each product into whatever sits on its face; a pipe or tank takes it, a dish pulls it itself. A toxic gas
     * with nowhere to go seeps out into the room once a second; a corrosive liquid bites the lining. A cell above room
     * temperature loses heat to it.
     */
    public void tickServer() {
        if (level == null)
            return;
        boolean movedAnode = push(front(), anode);
        boolean movedCathode = push(front().getOpposite(), cathode);
        Tu ambient = AmbientTable.at(level);
        if (temperature > ambient.value() + 0.5f)
            Heat.tick(this, ambient, ambient, false);
        if (level.getGameTime() % 20 == 0) {
            seep(anode, movedAnode);
            seep(cathode, movedCathode);
            if (Corrosion.corrosive(feed.getFluid()) || Corrosion.corrosive(liquor.getFluid())
                    || Corrosion.corrosive(anode.getFluid()) || Corrosion.corrosive(cathode.getFluid())) {
                float bite = Corrosion.wearPerSecond(HazardTuning.ELECTROLYSER_LINING);
                if (bite > 0f)
                    wear(bite);
            }
        }
    }

    private void seep(FluidTank tank, boolean moved) {
        if (moved || tank.getFluidAmount() < tank.getCapacity() / 2)
            return;
        Material m = MaterialTable.get().materials().get(BuiltInRegistries.FLUID.getKey(tank.getFluid().getFluid()));
        int toxic = m == null ? 0 : Fumes.level(m);
        if (toxic <= 0)
            return;
        tank.drain(ElectricMachineTuning.ELECTROLYSER_SEEP_MB, IFluidHandler.FluidAction.EXECUTE);
        Fumes.release(level, Vec3.atCenterOf(worldPosition), toxic);
        setChanged();
    }

    private boolean push(Direction face, FluidTank tank) {
        if (tank.isEmpty())
            return false;
        IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(face),
                face.getOpposite());
        if (target != null) {
            int moved = target.fill(tank.getFluid(), IFluidHandler.FluidAction.EXECUTE);
            if (moved > 0) {
                tank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
                setChanged();
                return true;
            }
        }
        return false;
    }

    // --- ThermalBody --------------------------------------------------------------------------

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
        return ElectricMachineTuning.ELECTROLYSER_MASS;
    }

    @Override
    public Conductance getLeak() {
        return ElectricMachineTuning.ELECTROLYSER_LEAK;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Feed", feed.writeToNBT(registries, new CompoundTag()));
        tag.put("Liquor", liquor.writeToNBT(registries, new CompoundTag()));
        tag.put("Anode", anode.writeToNBT(registries, new CompoundTag()));
        tag.put("Cathode", cathode.writeToNBT(registries, new CompoundTag()));
        tag.put("Solute", solute.serializeNBT(registries));
        tag.put("Plate", plate.serializeNBT(registries));
        tag.putInt("Plated", plated);
        tag.putFloat("Condition", condition);
        tag.putFloat("UnitsLeft", unitsLeft);
        tag.putFloat("ResidueOwed", residueOwed);
        tag.putFloat("Temperature", temperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        feed.readFromNBT(registries, tag.getCompound("Feed"));
        liquor.readFromNBT(registries, tag.getCompound("Liquor"));
        anode.readFromNBT(registries, tag.getCompound("Anode"));
        cathode.readFromNBT(registries, tag.getCompound("Cathode"));
        solute.deserializeNBT(registries, tag.getCompound("Solute"));
        if (tag.contains("Plate"))
            plate.deserializeNBT(registries, tag.getCompound("Plate"));
        plated = tag.getInt("Plated");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
        unitsLeft = tag.getFloat("UnitsLeft");
        residueOwed = tag.getFloat("ResidueOwed");
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : ThermalTuning.AMBIENT_TU.value();
    }
}
