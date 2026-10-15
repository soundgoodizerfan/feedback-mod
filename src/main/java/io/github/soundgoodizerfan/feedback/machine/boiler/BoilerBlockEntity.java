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
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Fire in, Steam out, and nothing else -- see {@code FTuning}'s {@code --- the boiler ---}
 * section for why this deliberately does not know rotation exists.
 * <p>
 * A {@link ThermalBody} heated from below exactly like a crucible -- see
 * {@link HeatSource#below} -- with a water tank and a steam tank. Above
 * {@link FTuning#BOILER_WORKING_TU} it turns water into Steam at a flat rate; below it, nothing
 * happens beyond ordinary heat loss. What burns under it is this block's problem alone; what
 * eventually spends the Steam is somebody else's.
 */
public class BoilerBlockEntity extends BlockEntity implements ThermalBody {

    private float temperature = FTuning.AMBIENT_TU.value();
    /** Away-time bookkeeping: see {@link Catchup}. */
    private final Catchup offline = new Catchup();
    private float lastDelta;
    /** For {@link #measureDelta}; NaN until the first tick after a load. */
    private float temperatureLastTick = Float.NaN;

    private final FluidTank waterTank = new FluidTank(FTuning.BOILER_WATER_TANK_MB,
            stack -> stack.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank steamTank = new FluidTank(FTuning.BOILER_STEAM_TANK_MB,
            stack -> stack.is(FTags.STEAM)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.BOILER.get(), pos, state);
    }

    public FluidTank getWaterTank() {
        return waterTank;
    }

    public FluidTank getSteamTank() {
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
        makeSteam();
        holdPressure();
    }

    private void makeSteam() {
        if (temperature < FTuning.BOILER_WORKING_TU.value())
            return;

        // Only the heat above working temperature can boil anything, and boiling spends it.
        int affordable = (int) ((temperature - FTuning.BOILER_WORKING_TU.value())
                * FTuning.BOILER_MASS.workPerTu() / FTuning.BOILER_WORK_PER_MB_STEAM);
        int amount = Math.min(Math.min(FTuning.BOILER_STEAM_PER_TICK_MB, affordable),
                Math.min(waterTank.getFluidAmount(), steamTank.getSpace()));
        if (amount <= 0)
            return;

        temperature -= amount * FTuning.BOILER_WORK_PER_MB_STEAM / FTuning.BOILER_MASS.workPerTu();
        waterTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        steamTank.fill(new FluidStack(FFluids.STEAM.get(), amount), IFluidHandler.FluidAction.EXECUTE);
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
    public Pu getPressure() {
        float held = steamTank.getFluidAmount() / FTuning.BOILER_STEAM_MB_PER_PU;
        boolean trapped = steamTank.getSpace() <= FTuning.BOILER_STEAM_PER_TICK_MB;
        float over = trapped ? Math.max(0f, temperature - FTuning.BOILER_WORKING_TU.value()) : 0f;
        return new Pu(held + over * FTuning.BOILER_PU_PER_TU_OVER);
    }

    /**
     * The two ways a pressurised boiler tells the player about it, in the order they happen:
     * its seams weep (steam lost, whoever stands beside it scalded -- loud, local, and survivable),
     * and then, if nothing was done, it lets go. The weep is also a partial relief, so a boiler a
     * little over the line loses steam and may settle; one under a real fire does not.
     */
    private void holdPressure() {
        Pu pressure = getPressure();
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
