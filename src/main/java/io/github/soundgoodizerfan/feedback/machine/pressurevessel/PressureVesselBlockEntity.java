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
package io.github.soundgoodizerfan.feedback.machine.pressurevessel;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.machine.bellows.Blown;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A sealed tank of compressed air, fed by a Bellows exactly the way a Firebox or a self-fired
 * vessel already is -- see {@link Blown}. Air here does not multiply a flame; it accumulates, and
 * enough of it explodes.
 *
 * <h2>Amount and volume, not a stored Pu</h2>
 * {@code amount} is an {@code int} in the same units {@link Blown#addAir} already deals in, and
 * {@link FTuning#PRESSURE_VESSEL_VOLUME} is fixed per block. {@link #getPressure()} divides them
 * on read, never stored -- the same discipline {@code ItemHeat} and {@code Heat.equilibrium}
 * already use for a derived figure, and directly {@code PressureTier}/{@code IAirHandler}'s own
 * shape (PneumaticCraft: Repressurized, GPL-3.0 -- see {@code THIRD-PARTY-LICENSES.md}).
 *
 * <h2>No identity check on the compressor either</h2>
 * A Bellows aimed at this finds it the same way it finds a Firebox or a self-fired vessel: by
 * scanning its six neighbours for anything that implements {@link Blown} and handing over air.
 * Nothing here knows it is being fed by a Bellows specifically, and nothing about the Bellows knows
 * it is feeding a Pressure Vessel rather than a fire -- philosophy 11 applied to an actuator
 * instead of a sorter.
 */
public class PressureVesselBlockEntity extends BlockEntity implements Blown {

    private static final String AMOUNT = "Amount";

    private int amount;

    public PressureVesselBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.PRESSURE_VESSEL.get(), pos, state);
    }

    @Override
    public void addAir(float air) {
        amount = Math.max(0, amount + Math.round(air));
        setChanged();
    }

    public Pu getPressure() {
        return new Pu((float) amount / FTuning.PRESSURE_VESSEL_VOLUME);
    }

    public void tickServer() {
        if (level == null || level.isClientSide)
            return;

        if (amount > 0) {
            amount = Math.max(0, amount - FTuning.PRESSURE_VESSEL_LEAK_PER_TICK);
            setChanged();
        }

        float pressure = getPressure().value();
        if (pressure <= FTuning.PRESSURE_VESSEL_DANGER_PU)
            return;

        // Read from PneumaticCraft: Repressurized's MachineAirHandler#addAir (GPL-3.0) -- risk
        // climbs linearly across the danger-to-critical band, and critical itself is certain
        // rather than merely likely. This is philosophy 6's Hazard tier, and philosophy 14's named
        // GregTech-boiler precedent, both landing on Feedback's own equipment rather than staying
        // an open item -- see FTuning#PRESSURE_VESSEL_DANGER_PU.
        boolean critical = pressure >= FTuning.PRESSURE_VESSEL_CRITICAL_PU;
        float range = FTuning.PRESSURE_VESSEL_CRITICAL_PU - FTuning.PRESSURE_VESSEL_DANGER_PU;
        float chance = range <= 0 ? 1f : (pressure - FTuning.PRESSURE_VESSEL_DANGER_PU) / range;
        if (critical || level.random.nextFloat() < chance)
            explode();
    }

    private void explode() {
        Level level = this.level;
        BlockPos pos = worldPosition;
        amount = 0;
        setChanged();
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.0f,
                Level.ExplosionInteraction.BLOCK);
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(AMOUNT, amount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        amount = tag.getInt(AMOUNT);
    }
}
