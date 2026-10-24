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
import io.github.soundgoodizerfan.feedback.process.PhaseChangeTable;

/**
 * Fire in, Steam out, and nothing else -- see {@code FTuning}'s {@code --- the boiler ---}
 * section for why this deliberately does not know rotation exists.
 * <p>
 * A {@link ThermalBody} heated from below exactly like a crucible -- see
 * {@link HeatSource#below} -- with a water tank and a steam tank. Above
 * the boiling point in the phase-change table (default {@link FTuning#BOILER_WORKING_TU}) it turns water into Steam, stamped at the boiler's own temperature, at a flat rate; below it, nothing
 * happens beyond ordinary heat loss. What burns under it is this block's problem alone; what
 * eventually spends the Steam is somebody else's.
 * <p>
 * A {@link Fittable} for Upgrades only (no face to mount a sensor on yet): the one it honours is
 * a {@link ReliefValveFitting}, at most one. Fitted, the boiler vents steam over
 * {@link FTuning#BOILER_RELIEF_PU}, below the seep line, so it never reaches the danger line; the
 * price is the steam and a scald. Unfitted, the seams are the only relief.
 */
public class BoilerBlockEntity extends BlockEntity implements io.github.soundgoodizerfan.feedback.core.thermal.HeatedTanks, ThermalBody, Fittable, io.github.soundgoodizerfan.feedback.core.hazard.PressureHolder {

    /** At most one {@link ReliefValveFitting}; stateless, so a flag is its whole persistence. */
    private final List<UpgradeFitting> upgrades = new ArrayList<>();

    private float temperature = FTuning.AMBIENT_TU.value();
    /** Away-time bookkeeping: see {@link Catchup}. */
    private final Catchup offline = new Catchup();
    private float lastDelta;
    /** For {@link #measureDelta}; NaN until the first tick after a load. */
    private float temperatureLastTick = Float.NaN;

    private final HeatedTank waterTank = new HeatedTank(FTuning.BOILER_WATER_TANK_MB,
            stack -> stack.is(FluidTags.WATER), this::setChanged);
    private final HeatedTank steamTank = new HeatedTank(FTuning.BOILER_STEAM_TANK_MB,
            stack -> stack.is(FTags.STEAM), this::setChanged);

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.BOILER.get(), pos, state);
    }

    @Override
    public java.util.List<HeatedTank> heatedTanks() {
        return java.util.List.of(waterTank, steamTank);
    }

    public HeatedTank getWaterTank() {
        return waterTank;
    }

    public HeatedTank getSteamTank() {
        return steamTank;
    }

    // --- thermal ----------------------------------------------------------------------------

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
        return FTuning.BOILER_MASS;
    }

    @Override
    public Conductance getLeak() {
        return FTuning.BOILER_LEAK;
    }

    public TuRate getHeatingRate() {
        return new TuRate(lastDelta);
    }

    // --- the tick ---------------------------------------------------------------------------

    public void tickServer() {
        if (level == null)
            return;

        Tu fireTu = HeatSource.below(level, worldPosition);
        Tu ambient = AmbientTable.at(level);
        long away = offline.take(level);
        if (away > 0L)
            Catchup.settle(this, HeatSource.spanBelow(level, worldPosition, away), ambient, away);
        Heat.tick(this, fireTu, ambient, fireTu.value() > ambient.value());
        measureDelta();

        setChanged();
        // Contents settle toward the body they sit in.
        steamTank.relax(getTemperature());
        waterTank.relax(getTemperature());
        makeSteam();
        holdPressure();
    }

    /** The water's boiling point, from the phase-change table; the working temperature when none applies. */
    private float workingTu() {
        return PhaseChangeTable.get().boiling(waterTank.getFluid()).map(c -> c.boil().value())
                .orElse(FTuning.BOILER_WORKING_TU.value());
    }

    private void makeSteam() {
        var change = PhaseChangeTable.get().boiling(waterTank.getFluid());
        if (change.isEmpty())
            return;
        PhaseChange phase = change.get();
        if (temperature < phase.boil().value())
            return;

        // Only the heat above the boiling point can boil anything, and boiling spends it.
        int affordable = (int) ((temperature - phase.boil().value())
                * FTuning.BOILER_MASS.workPerTu() / phase.latent());
        int amount = Math.min(Math.min(FTuning.BOILER_STEAM_PER_TICK_MB, affordable),
                Math.min(waterTank.getFluidAmount(), steamTank.getSpace()));
        if (amount <= 0)
            return;

        float at = temperature;
        temperature -= amount * phase.latent() / FTuning.BOILER_MASS.workPerTu();
        waterTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        var gas = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(phase.gasFluid());
        steamTank.fill(FluidHeat.stamp(new FluidStack(gas, amount), at), IFluidHandler.FluidAction.EXECUTE);
        // Boiling: the brewing stand's bubbling, now and then, only while water is actually turning to steam.
        if (level.random.nextInt(30) == 0)
            level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.1f + level.random.nextFloat() * 0.3f);
    }

    /**
     * The steam held, over a fixed volume, plus what a shut-in boiler's heat adds to it.
     *
     * <h3>Pressure is not temperature, and it is not only steam either</h3>
     * A boiler making steam into an open line spends its heat on the work of boiling (see
     * {@link #makeSteam}), so its temperature is held near working however big the fire.
     * Steam with nowhere to go removes that outlet: the tank is full, nothing is boiled, and the
     * fire carries on pushing the temperature up. That surplus is paid as pressure, linearly in
     * how far over working temperature the boiler sits. A full tank on its own is
     * {@code 4 Pu}, under the danger line; it is the heat on top that crosses it, which is why
     * the only mistake that kills a boiler is a restricted line under a real fire.
     */
    @Override
    public Pu getPressure() {
        float held = steamTank.getFluidAmount() / FTuning.BOILER_STEAM_MB_PER_PU;
        boolean trapped = steamTank.getSpace() <= FTuning.BOILER_STEAM_PER_TICK_MB;
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
            waterTank.setFluid(FluidStack.EMPTY);
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

    @Override
    public List<UpgradeFitting> getUpgrades() {
        return upgrades;
    }

    @Override
    public boolean addUpgrade(UpgradeFitting upgrade) {
        boolean added = upgrades.add(upgrade);
        if (added)
            sync();
        return added;
    }

    @Override
    public void removeUpgrade(UpgradeFitting upgrade) {
        if (upgrades.remove(upgrade))
            sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // --- persistence ------------------------------------------------------------------------

    /**
     * The rate is the temperature's actual change since last tick, not Heat.tick's own figure --
     * a heat exchanger moves heat in its own tick, and that cooling has to count toward the rate
     * limit and toward a cooling-only process like any other.
     */
    private void measureDelta() {
        lastDelta = Float.isNaN(temperatureLastTick) ? 0f : temperature - temperatureLastTick;
        temperatureLastTick = temperature;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.put("WaterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("SteamTank", steamTank.writeToNBT(registries, new CompoundTag()));
        tag.putBoolean("ReliefValve", hasReliefValve());
        offline.save(tag, level);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        if (tag.contains("WaterTank"))
            waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        if (tag.contains("SteamTank"))
            steamTank.readFromNBT(registries, tag.getCompound("SteamTank"));
        upgrades.clear();
        if (tag.getBoolean("ReliefValve"))
            upgrades.add(new ReliefValveFitting());
        offline.load(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
