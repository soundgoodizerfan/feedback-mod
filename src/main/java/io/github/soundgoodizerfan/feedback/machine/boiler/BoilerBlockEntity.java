// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.boiler;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.tags.FluidTags;
import io.github.soundgoodizerfan.feedback.registry.FTags;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureBurst;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Catchup;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.SidedFitting;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.fitting.upgrade.ReliefValveFitting;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

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
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank;
import io.github.soundgoodizerfan.feedback.process.PhaseChange;
import io.github.soundgoodizerfan.feedback.process.ThermalProcessTable;

/**
 * Fire in, Steam out, and nothing else -- see {@code FTuning}'s {@code --- the boiler ---}
 * section for why this deliberately does not know rotation exists.
 * <p>
 * A {@link ThermalBody} heated from below exactly like a crucible -- see
 * {@link HeatSource#at} -- with a water tank and a steam tank. Above
 * the boiling point in the phase-change table (default {@link FTuning#BOILER_WORKING_TU}) it turns water into Steam, stamped at the boiler's own temperature, at a flat rate; below it, nothing
 * happens beyond ordinary heat loss. What burns under it is this block's problem alone; what
 * eventually spends the Steam is somebody else's.
 * <p>
 * A {@link Fittable} for Upgrades only (no face to mount a sensor on yet): the one it honours is
 * a {@link ReliefValveFitting}, at most one. Fitted, the boiler vents steam over
 * {@link FTuning#BOILER_RELIEF_PU}, below the seep line, so it never reaches the danger line; the
 * price is the steam and a scald. Unfitted, the seams are the only relief.
 */
public class BoilerBlockEntity extends io.github.soundgoodizerfan.feedback.machine.vessel.ThermalVesselCore implements io.github.soundgoodizerfan.feedback.core.thermal.HeatedTanks, io.github.soundgoodizerfan.feedback.core.hazard.PressureHolder {

    /** Away-time bookkeeping: see {@link Catchup}. */
    private final Catchup offline = new Catchup();

    private final HeatedTank steamTank = new HeatedTank(FTuning.BOILER_STEAM_TANK_MB * scaleOf(getBlockState()),
            stack -> stack.is(FTags.STEAM), this::setChanged);

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.BOILER.get(), pos, state);
    }

    private static int scaleOf(BlockState state) {
        return state.getBlock() instanceof BoilerBlock boiler ? boiler.scale() : 1;
    }

    private int scale() {
        return scaleOf(getBlockState());
    }

    /** A boiler holds no workpieces: it is a closed vessel whose contents are the water and the steam. */
    @Override
    protected List<net.minecraft.world.item.ItemStack> processSlots() {
        return List.of();
    }

    @Override
    public java.util.List<HeatedTank> heatedTanks() {
        return java.util.List.of(tank, steamTank);
    }

    public HeatedTank getWaterTank() {
        return tank;
    }

    public HeatedTank getSteamTank() {
        return steamTank;
    }

    // --- thermal ----------------------------------------------------------------------------

    @Override
    public ThermalMass getThermalMass() {
        return new ThermalMass(FTuning.BOILER_MASS.workPerTu() * scale());
    }

    @Override
    public Conductance getLeak() {
        return new Conductance(FTuning.BOILER_LEAK.workPerTickPerTu() * (float) Math.cbrt((double) scale() * scale()));
    }

    public TuRate getHeatingRate() {
        return new TuRate(lastDelta);
    }

    // --- the tick ---------------------------------------------------------------------------

    public void tickServer() {
        if (level == null)
            return;

        Tu fireTu = HeatSource.at(level, worldPosition);
        Tu ambient = AmbientTable.at(level);
        long away = offline.take(level);
        if (away > 0L)
            Catchup.settle(this, HeatSource.spanAt(level, worldPosition, away), ambient, away);
        Heat.tick(this, fireTu, ambient, fireTu.value() > ambient.value(), scale());
        measureDelta();

        setChanged();
        // Contents settle toward the body they sit in.
        steamTank.relax(getTemperature());
        tank.relax(getTemperature());
        boilOff();
        holdPressure();
    }

    /** The water's boiling point, from the phase-change table; the working temperature when none applies. */
    private float workingTu() {
        return ThermalProcessTable.get().boiling(tank.getFluid()).map(c -> c.boil().value())
                .orElse(FTuning.BOILER_WORKING_TU.value());
    }

    // --- what a boiler is, beside the vessel core's boil-off -----------------------------------

    @Override
    protected int tankCapacity() {
        return FTuning.BOILER_WATER_TANK_MB * scaleOf(getBlockState());
    }

    @Override
    protected boolean tankTakes(FluidStack stack) {
        return stack.is(FluidTags.WATER);
    }

    /** Only the heat above the boiling point can boil anything, and a boiler turns a fixed quantity a tick, not a share of its tank. */
    @Override
    protected int boilAmount(PhaseChange cut, float wanted) {
        int affordable = (int) ((temperature - cut.boil().value()) * FTuning.BOILER_MASS.workPerTu() * scale() / cut.latent());
        return Math.min(FTuning.BOILER_STEAM_PER_TICK_MB * scale(), affordable);
    }

    @Override
    protected float boilCost(PhaseChange cut, int mB) {
        return mB * cut.latent() / (FTuning.BOILER_MASS.workPerTu() * scale());
    }

    @Override
    protected int vapourRoom() {
        return steamTank.getSpace();
    }

    /** The steam goes into the steam tank at the heat it boiled at; every so often the stand bubbles. */
    @Override
    protected void vent(PhaseChange change, int mB, float at) {
        var gas = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(change.gasFluid());
        steamTank.fill(FluidHeat.stamp(new FluidStack(gas, mB), at), IFluidHandler.FluidAction.EXECUTE);
        if (level.random.nextInt(30) == 0)
            level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.1f + level.random.nextFloat() * 0.3f);
    }

    /**
     * The steam held, over a fixed volume, plus what a shut-in boiler's heat adds to it.
     *
     * <h3>Pressure is not temperature, and it is not only steam either</h3>
     * A boiler making steam into an open line spends its heat on the work of boiling (see
     * {@link #boilAmount}), so its temperature is held near working however big the fire.
     * Steam with nowhere to go removes that outlet: the tank is full, nothing is boiled, and the
     * fire carries on pushing the temperature up. That surplus is paid as pressure, linearly in
     * how far over working temperature the boiler sits. A full tank on its own is
     * {@code 4 Pu}, under the danger line; it is the heat on top that crosses it, which is why
     * the only mistake that kills a boiler is a restricted line under a real fire.
     */
    @Override
    public Pu getPressure() {
        float held = steamTank.getFluidAmount() / (FTuning.BOILER_STEAM_MB_PER_PU * scale());
        boolean trapped = steamTank.getSpace() <= FTuning.BOILER_STEAM_PER_TICK_MB * scale();
        float over = trapped ? Math.max(0f, temperature - workingTu()) : 0f;
        return new Pu(held + over * FTuning.BOILER_PU_PER_TU_OVER);
    }

    /**
     * The ways a pressurised boiler tells the player about it, in the order they happen: a fitted
     * relief valve lifts at {@link FTuning#BOILER_RELIEF_PU} (steam lost, a scald, and never
     * lower than that line); its seams weep (steam lost, whoever stands beside it scalded -- loud, local, and survivable),
     * and then, if nothing was done, it lets go. The weep is also a partial relief, so a boiler a
     * little over the line loses steam and may settle; one under a real fire does not.
     */
    private void holdPressure() {
        Pu pressure = getPressure();
        float relief = pressure.value() - FTuning.BOILER_RELIEF_PU.value();
        if (relief > 0f && hasReliefValve()) {
            // Venting un-traps a full tank, which drops the over-temperature term with it -- so
            // pressure is re-read: the seams only weep if the valve could not keep up.
            int vented = Math.min(steamTank.getFluidAmount(),
                    Math.max(1, Math.round(relief * FTuning.BOILER_RELIEF_MB_PER_PU)));
            steamTank.drain(vented, IFluidHandler.FluidAction.EXECUTE);
            if (level.getGameTime() % FTuning.CONTACT_INTERVAL_TICKS == 0) {
                PressureBurst.hiss(level, worldPosition);
                PressureBurst.scald(level, worldPosition, FTuning.STEAM_SCALD_REACH, FTuning.STEAM_SCALD_DAMAGE);
            }
            pressure = getPressure();
        }
        float over = pressure.value() - FTuning.BOILER_SEEP_PU.value();
        if (over > 0f) {
            int lost = Math.min(steamTank.getFluidAmount(),
                    Math.max(1, Math.round(over * FTuning.BOILER_SEEP_MB_PER_PU)));
            steamTank.drain(lost, IFluidHandler.FluidAction.EXECUTE);
            if (level.getGameTime() % FTuning.CONTACT_INTERVAL_TICKS == 0) {
                PressureBurst.hiss(level, worldPosition);
                PressureBurst.scald(level, worldPosition, FTuning.STEAM_SCALD_REACH, FTuning.STEAM_SCALD_DAMAGE);
            }
        }
        if (PressureBurst.fails(pressure, FTuning.PRESSURE_VESSEL_DANGER_PU,
                FTuning.PRESSURE_VESSEL_CRITICAL_PU, level.random)) {
            steamTank.setFluid(FluidStack.EMPTY);
            tank.setFluid(FluidStack.EMPTY);
            PressureBurst.burst(level, worldPosition, FTuning.BOILER_BURST_POWER);
        }
    }

    // --- fitting ----------------------------------------------------------------------------

    private boolean hasReliefValve() {
        return upgrades.stream().anyMatch(ReliefValveFitting.class::isInstance);
    }

    /** No faces yet: a boiler takes Upgrades only. */
    @Override
    public SidedFitting getSidedFitting(Direction side) {
        return null;
    }

    @Override
    public void setSidedFitting(Direction side, SidedFitting fitting) {
    }

    @Override
    public boolean canMount(SidedFitting fitting, Direction side) {
        return false;
    }

    /** The only upgrade this boiler reads is a relief valve, one of it. */
    @Override
    public boolean canAdd(UpgradeFitting upgrade) {
        return upgrade instanceof ReliefValveFitting && !hasReliefValve();
    }

    // --- persistence ------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.put("WaterTank", tank.writeToNBT(registries, new CompoundTag()));
        tag.put("SteamTank", steamTank.writeToNBT(registries, new CompoundTag()));
        tag.putBoolean("ReliefValve", hasReliefValve());
        offline.save(tag, level);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        if (tag.contains("WaterTank"))
            tank.readFromNBT(registries, tag.getCompound("WaterTank"));
        if (tag.contains("SteamTank"))
            steamTank.readFromNBT(registries, tag.getCompound("SteamTank"));
        upgrades.clear();
        if (tag.getBoolean("ReliefValve"))
            upgrades.add(new ReliefValveFitting());
        offline.load(tag);
    }
}
