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
package io.github.soundgoodizerfan.feedback.machine.reactor;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureBurst;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureHolder;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTanks;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.geology.CrudeGrade;
import io.github.soundgoodizerfan.feedback.process.ThermalProcess;
import io.github.soundgoodizerfan.feedback.process.ThermalProcessTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * One physical operation (spec 7.4): a pressure vessel with a thermal body and a catalyst bed that
 * applies whichever {@code thermal_process} row carrying a {@code reaction} fits what is inside. It
 * names no reaction, product or catalyst; rows are data and a catalyst is an item tag.
 * <p>
 * Progress is TPu-style: it climbs by the row's triangular suitability on Tu, times a pressure
 * factor and the bed's activity, and decays when conditions slip rather than resetting. Over the
 * row's max Tu a row with an {@code over_tu} product makes that instead, so off-window is a worse
 * product and never a lock; inputs are consumed either way. A bed loses activity per mB of bad feed
 * (a crude's sulfur, an over-temperature batch's coke), not per tick.
 * The bed's material (a {@code catalyst} block) sets its activity, sulfur tolerance, regeneration and sinter
 * temperatures; a bed whose item names none uses the {@code FTuning} figures.
 * shortcut: no concentration window beyond "enough of every input"; pressure is the gas held over {@link FTuning#REACTOR_GAS_MB_PER_PU}, there is no compressor yet.
 */
public class ReactorBlockEntity extends BlockEntity implements ThermalBody, PressureHolder, HeatedTanks {

    private static final int INPUT_TANKS = 3;

    private final HeatedTank[] inputs = new HeatedTank[INPUT_TANKS];
    private final HeatedTank output = new HeatedTank(FTuning.REACTOR_TANK_MB, this::setChanged);
    private ItemStack bed = ItemStack.EMPTY;
    /** The hopper: what a row's first item input is taken from, one per batch. */
    private ItemStack hopper = ItemStack.EMPTY;
    private float temperature = FTuning.AMBIENT_TU.value();
    private float lastDelta, temperatureLastTick = Float.NaN;
    private float progress;
    @Nullable
    private ThermalProcess running;

    private final IFluidHandler handler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return INPUT_TANKS + 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank < INPUT_TANKS ? inputs[tank].getFluid() : output.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return FTuning.REACTOR_TANK_MB;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank < INPUT_TANKS;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            for (HeatedTank tank : inputs)
                if (!tank.isEmpty() && tank.getFluid().getFluid() == resource.getFluid())
                    return tank.fill(resource, action);
            for (HeatedTank tank : inputs)
                if (tank.isEmpty())
                    return tank.fill(resource, action);
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return output.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return output.drain(maxDrain, action);
        }
    };

    public ReactorBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.REACTOR.get(), pos, state);
        for (int i = 0; i < INPUT_TANKS; i++)
            inputs[i] = new HeatedTank(FTuning.REACTOR_TANK_MB, this::setChanged);
    }

    public IFluidHandler getHandler() {
        return handler;
    }

    @Override
    public List<HeatedTank> heatedTanks() {
        return List.of(inputs[0], inputs[1], inputs[2], output);
    }

    // --- the bed ---------------------------------------------------------------------------

    public boolean hasBed() {
        return !bed.isEmpty();
    }

    public boolean insertBed(ItemStack stack) {
        if (hasBed() || stack.isEmpty())
            return false;
        bed = stack.split(1);
        setChanged();
        return true;
    }

    /** Takes as much of {@code stack} as the hopper holds beside what is there; true if any went in. */
    public boolean insertHopper(ItemStack stack) {
        if (stack.isEmpty() || (!hopper.isEmpty() && !ItemStack.isSameItemSameComponents(hopper, stack)))
            return false;
        if (hopper.isEmpty()) {
            hopper = stack.split(stack.getMaxStackSize());
        } else {
            int moved = Math.min(hopper.getMaxStackSize() - hopper.getCount(), stack.getCount());
            if (moved <= 0)
                return false;
            hopper.grow(moved);
            stack.shrink(moved);
        }
        setChanged();
        return true;
    }

    public ItemStack takeHopper() {
        ItemStack taken = hopper;
        hopper = ItemStack.EMPTY;
        setChanged();
        return taken;
    }

    /** Whether a row wants this item as a bed (names a catalyst tag it is in), as opposed to feed. */
    public static boolean isBedFor(ItemStack stack) {
        for (ThermalProcess p : ThermalProcessTable.get().reactions())
            if (p.reaction().get().catalyst().isPresent() && stack.is(p.reaction().get().catalyst().get()))
                return true;
        return false;
    }

    public ItemStack takeBed() {
        ItemStack taken = bed;
        bed = ItemStack.EMPTY;
        setChanged();
        return taken;
    }

    /** The bed's activity, 0 to 1 (shown as a percentage, like machine condition). */
    public float activity() {
        return Math.max(0f, 1f - loss(FDataComponents.BED_COKE.get()) - loss(FDataComponents.BED_POISON.get())
                - loss(FDataComponents.BED_SINTER.get()));
    }

    /** The bed's material block, if its item has one; the figures below fall back to {@code FTuning} for a bed that names none. */
    @Nullable
    private io.github.soundgoodizerfan.feedback.process.Material.Catalyst catalyst() {
        return hasBed() ? io.github.soundgoodizerfan.feedback.process.Material.find(
                io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials(), bed.getItem())
                .flatMap(l -> l.material().catalyst()).orElse(null) : null;
    }

    private float regenTu() {
        var c = catalyst();
        return c == null ? FTuning.REACTOR_REGEN_TU : c.regenerationTemperature().value();
    }

    private float sinterTu() {
        var c = catalyst();
        return c == null ? FTuning.REACTOR_SINTER_TU : c.sinterTemperature().value();
    }

    private float loss(net.minecraft.core.component.DataComponentType<Float> type) {
        return bed.getOrDefault(type, 0f);
    }

    private void addLoss(net.minecraft.core.component.DataComponentType<Float> type, float amount) {
        if (hasBed() && amount != 0f) {
            bed.set(type, Math.max(0f, loss(type) + amount));
            setChanged();
        }
    }

    /**
     * With no row running (the feed stopped) a bed regenerates (spec 7.5): hot enough and an oxidiser
     * flowing burns coke off, a reducing gas flowing sweeps poison off, each by its own tank and its own
     * pace. Both are read off the fluid's {@code oxygen_balance} and phase, never its name. A bed held
     * over the sinter line loses activity for good, whether it is working or regenerating.
     */
    private void regenerate() {
        if (!hasBed())
            return;
        if (temperature > sinterTu())
            addLoss(FDataComponents.BED_SINTER.get(), FTuning.REACTOR_SINTER_PER_TICK
                    * (temperature - sinterTu()) / sinterTu());
        if (running != null || temperature < regenTu())
            return;
        for (HeatedTank tank : inputs) {
            if (tank.isEmpty())
                continue;
            var m = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials()
                    .get(BuiltInRegistries.FLUID.getKey(tank.getFluid().getFluid()));
            if (m == null || m.combustion().isEmpty())
                continue;
            float balance = m.combustion().get().oxygenBalance();
            int used = tank.drain((int) FTuning.REACTOR_REGEN_MB_PER_TICK, IFluidHandler.FluidAction.SIMULATE).getAmount();
            if (balance > 0f && loss(FDataComponents.BED_COKE.get()) > 0f) {
                tank.drain(used, IFluidHandler.FluidAction.EXECUTE);
                addLoss(FDataComponents.BED_COKE.get(), -used * FTuning.REACTOR_COKE_CLEAR_PER_MB);
                Heat.warm(this, used * FTuning.REACTOR_REGEN_HEAT_PER_MB);
            } else if (balance < 0f && tank.getFluid().getFluidType().getDensity() < 0
                    && loss(FDataComponents.BED_POISON.get()) > 0f) {
                tank.drain(used, IFluidHandler.FluidAction.EXECUTE);
                addLoss(FDataComponents.BED_POISON.get(), -used * FTuning.REACTOR_POISON_CLEAR_PER_MB);
            }
        }
    }

    // --- thermal and pressure --------------------------------------------------------------

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
        return FTuning.REACTOR_MASS;
    }

    @Override
    public Conductance getLeak() {
        return FTuning.BOILER_LEAK;
    }

    /** Gas held over a fixed volume: a fluid whose type is lighter than air. */
    @Override
    public Pu getPressure() {
        float gas = 0f;
        for (HeatedTank tank : inputs)
            if (!tank.isEmpty() && tank.getFluid().getFluidType().getDensity() < 0)
                gas += tank.getFluidAmount();
        return new Pu(gas / FTuning.REACTOR_GAS_MB_PER_PU);
    }

    // --- the tick --------------------------------------------------------------------------

    public void tickServer() {
        if (level == null)
            return;
        Tu fireTu = HeatSource.below(level, worldPosition);
        Tu ambient = AmbientTable.at(level);
        Heat.tick(this, fireTu, ambient, fireTu.value() > ambient.value());
        lastDelta = Float.isNaN(temperatureLastTick) ? 0f : temperature - temperatureLastTick;
        temperatureLastTick = temperature;
        pull();
        for (HeatedTank tank : heatedTanks())
            tank.relax(getTemperature());
        react();
        regenerate();
        if (PressureBurst.fails(getPressure(), FTuning.PRESSURE_VESSEL_DANGER_PU, FTuning.PRESSURE_VESSEL_CRITICAL_PU,
                level.random)) {
            for (HeatedTank tank : heatedTanks())
                tank.setFluid(FluidStack.EMPTY);
            PressureBurst.burst(level, worldPosition, FTuning.BOILER_BURST_POWER);
            return;
        }
        pushOutput();
        setChanged();
    }

    /** Reaches for any fluid some reaction row lists as an input, from any side, when an input tank has room. The reactor names nothing: the rows do. */
    private void pull() {
        for (Direction face : Direction.values()) {
            IFluidHandler neighbour = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(face), face.getOpposite());
            if (neighbour == null)
                continue;
            FluidStack offered = neighbour.drain(FTuning.REACTOR_PUSH_MB, IFluidHandler.FluidAction.SIMULATE);
            if (offered.isEmpty() || !isInput(offered))
                continue;
            int fits = handler.fill(offered, IFluidHandler.FluidAction.SIMULATE);
            if (fits > 0)
                handler.fill(neighbour.drain(offered.copyWithAmount(fits), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private static boolean isInput(FluidStack stack) {
        var id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        for (ThermalProcess p : ThermalProcessTable.get().reactions())
            for (ThermalProcess.Amount a : p.reaction().get().fluidInputs())
                if (a.fluid().equals(id))
                    return true;
        return false;
    }

    private int held(ThermalProcess.Amount a) {
        int total = 0;
        for (HeatedTank tank : inputs)
            if (!tank.isEmpty() && BuiltInRegistries.FLUID.getKey(tank.getFluid().getFluid()).equals(a.fluid()))
                total += tank.getFluidAmount();
        return total;
    }

    private boolean fits(ThermalProcess p) {
        ThermalProcess.Reaction r = p.reaction().get();
        if (!p.inputs().isEmpty() && (hopper.isEmpty() || !p.inputs().get(0).test(hopper)))
            return false;
        for (ThermalProcess.Amount a : r.fluidInputs())
            if (held(a) < a.mb())
                return false;
        return r.catalyst().isEmpty() || (hasBed() && bed.is(r.catalyst().get()));
    }

    private float pressureFactor(ThermalProcess.Reaction r) {
        float pu = getPressure().value();
        if (r.minPu().isPresent() && pu < r.minPu().get().value())
            return 0f;
        if (r.optimalPu().isPresent() && r.minPu().isPresent() && pu < r.optimalPu().get().value()
                && r.optimalPu().get().value() > r.minPu().get().value())
            return (pu - r.minPu().get().value()) / (r.optimalPu().get().value() - r.minPu().get().value());
        return 1f;
    }

    private void react() {
        ThermalProcess row = null;
        for (ThermalProcess p : ThermalProcessTable.get().reactions())
            if (fits(p)) {
                row = p;
                break;
            }
        if (row != running) {
            running = row;
            progress = 0f;
        }
        if (row == null)
            return;
        ThermalProcess.Reaction r = row.reaction().get();
        boolean over = !r.overTu().isEmpty() && temperature >= row.maxTemperature().value();
        float rate = over ? 1f : row.suitability(getTemperature()) * pressureFactor(r);
        if (r.catalyst().isPresent())
            rate *= (activity() <= r.activityFloor() ? 0f : (activity() - r.activityFloor()) / (1f - r.activityFloor()))
                    * (catalyst() == null ? 1f : catalyst().activity());
        if (Math.abs(lastDelta) > row.maxRateTuPerTick().tuPerTick())
            rate = 0f;
        progress = rate > 0f ? progress + rate : Math.max(0f, progress - FTuning.TPU_DECAY_PER_TICK);
        if (progress < row.requiredTpu())
            return;

        // The batch happens, in window or out: inputs are consumed either way.
        float sulfur = 0f;
        for (ThermalProcess.Amount a : r.fluidInputs()) {
            int left = a.mb();
            for (HeatedTank tank : inputs) {
                if (left <= 0 || tank.isEmpty() || !BuiltInRegistries.FLUID.getKey(tank.getFluid().getFluid()).equals(a.fluid()))
                    continue;
                if (tank.getFluid().has(FDataComponents.CRUDE_GRADE.get()))
                    sulfur += CrudeGrade.of(tank.getFluid()).sulfur() * Math.min(left, tank.getFluidAmount());
                left -= tank.drain(left, IFluidHandler.FluidAction.EXECUTE).getAmount();
            }
        }
        if (!row.inputs().isEmpty())
            hopper.shrink(1);
        if (!over && !row.result().isEmpty())
            net.minecraft.world.level.block.Block.popResource(level, worldPosition.above(), row.result().copy());
        for (ThermalProcess.Amount a : over ? r.overTu() : r.fluidOutputs())
            output.fill(FluidHeat.stamp(new FluidStack(BuiltInRegistries.FLUID.get(a.fluid()), a.mb()), temperature),
                    IFluidHandler.FluidAction.EXECUTE);
        if (r.catalyst().isPresent()) {
            addLoss(FDataComponents.BED_POISON.get(), sulfur * FTuning.REACTOR_SULFUR_FOUL_PER_MB
                    / (catalyst() == null ? 1f : Math.max(0.01f, catalyst().sulfurTolerance())));
            addLoss(FDataComponents.BED_COKE.get(), over ? FTuning.REACTOR_COKE_FOUL_PER_BATCH : 0f);
        }
        r.heat().ifPresent(w -> Heat.warm(this, w));
        progress = 0f;
    }

    private void pushOutput() {
        if (output.isEmpty())
            return;
        for (Direction d : Direction.values()) {
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(d),
                    d.getOpposite());
            if (target == null)
                continue;
            int fits = target.fill(output.drain(FTuning.REACTOR_PUSH_MB, IFluidHandler.FluidAction.SIMULATE),
                    IFluidHandler.FluidAction.SIMULATE);
            if (fits > 0)
                target.fill(output.drain(fits, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    // --- persistence -----------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.putFloat("Progress", progress);
        for (int i = 0; i < INPUT_TANKS; i++)
            tag.put("In" + i, inputs[i].writeToNBT(registries, new CompoundTag()));
        tag.put("Out", output.writeToNBT(registries, new CompoundTag()));
        if (hasBed())
            tag.put("Bed", bed.save(registries));
        if (!hopper.isEmpty())
            tag.put("Hopper", hopper.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        progress = tag.getFloat("Progress");
        for (int i = 0; i < INPUT_TANKS; i++)
            if (tag.contains("In" + i))
                inputs[i].readFromNBT(registries, tag.getCompound("In" + i));
        if (tag.contains("Out"))
            output.readFromNBT(registries, tag.getCompound("Out"));
        hopper = tag.contains("Hopper") ? ItemStack.parseOptional(registries, tag.getCompound("Hopper")) : ItemStack.EMPTY;
        bed = tag.contains("Bed") ? ItemStack.parseOptional(registries, tag.getCompound("Bed")) : ItemStack.EMPTY;
    }
}
