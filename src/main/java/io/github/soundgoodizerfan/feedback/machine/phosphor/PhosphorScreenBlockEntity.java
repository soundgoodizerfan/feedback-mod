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
package io.github.soundgoodizerfan.feedback.machine.phosphor;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureHolder;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Catchup;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureBurst;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A vacuum-glass envelope coated inside with a luminous phosphor, which glows where a signal lands
 * (mechanics 6.20; compendium 3.1). The coating is glowstone dust, which the material system
 * already calls {@code luminous}; the glass is the {@code glass} material, whose service
 * temperature is the envelope's.
 *
 * <h2>The smallest vacuum: hot-seal</h2>
 * No pump exists and none is built. The envelope is a {@link ThermalBody} that sits on a fire.
 * Pinching it off while hot seals the gas in; as it cools the sealed gas contracts, so its gauge
 * pressure is {@code (T + 273) / (T_seal + 273) - 1} times a scale, negative once cooler than the
 * seal. A hotter seal is a better vacuum, and the player picks the moment. It is a
 * {@link PressureHolder}, so the pressure gauge reads it (negative is vacuum). It is a partial
 * vacuum (about 0.6 of an atmosphere at best), which is enough to glow and not a hard vacuum.
 *
 * <h2>Overrun</h2>
 * Reheating a sealed envelope past its seal temperature turns the vacuum into pressure; past
 * {@link FTuning#ENVELOPE_DANGER_PU} the chance of letting go climbs each tick ({@link
 * PressureBurst}, shared with the vessel and boiler) and at critical it is certain. Heat past the
 * glass's service temperature cracks it outright. Either shatters it: nothing is returned. The
 * second misuse is burn-in: a screen held at full glow accumulates burn that permanently lowers
 * the brightest level it can reach, down to dead. Sealing, cooling and a signal that moves cost
 * nothing.
 *
 * <h2>Sense and stop</h2>
 * Sense: the pressure gauge (vacuum), a thermometer (heat). Stop: cool it, and take the signal off
 * a screen that is burning.
 */
public class PhosphorScreenBlockEntity extends BlockEntity implements ThermalBody, PressureHolder {

    private float temperature = FTuning.AMBIENT_TU.value();
    private final Catchup offline = new Catchup();
    private boolean sealed;
    private float sealTu;
    private float burn;
    private int drive;
    private int hold;

    public PhosphorScreenBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.PHOSPHOR_SCREEN.get(), pos, state);
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
        return FTuning.ENVELOPE_MASS;
    }

    @Override
    public Conductance getLeak() {
        return FTuning.ENVELOPE_LEAK;
    }

    public boolean isSealed() {
        return sealed;
    }

    /** Pinch the envelope off at its present heat. */
    public void seal() {
        if (sealed)
            return;
        sealed = true;
        sealTu = temperature;
        setChanged();
    }

    /** Gauge Pu: zero while open to the air, the sealed gas's contraction or expansion after. */
    @Override
    public Pu getPressure() {
        if (!sealed)
            return new Pu(0f);
        float off = FTuning.ENVELOPE_ABSOLUTE_OFFSET;
        return new Pu(((temperature + off) / (sealTu + off) - 1f) * FTuning.ENVELOPE_PU_PER_ATMOSPHERE);
    }

    /** An oscilloscope drives the screen at this level (0 to 3) for the next few ticks. */
    public void land(int level) {
        drive = Math.max(0, Math.min(3, level));
        hold = FTuning.SCOPE_SAMPLE_TICKS + 2;
    }

    /** The brightest level burn-in still allows: 3 new, down to 0 dead. */
    public int ceiling() {
        return Math.max(0, 3 - (int) Math.floor(burn * 3f));
    }

    public float burn() {
        return burn;
    }

    private float serviceTu() {
        Material glass = MaterialTable.get().materials().get(Feedback.id("glass"));
        return glass == null ? 500f : glass.thermal().serviceTemperature().map(Tu::value).orElse(500f);
    }

    private void shatter() {
        Level level = this.level;
        level.playSound(null, worldPosition, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1f, 0.8f);
        level.removeBlock(worldPosition, false);
    }

    public void tickServer() {
        if (level == null)
            return;
        Tu fireTu = HeatSource.below(level, worldPosition);
        Tu ambient = AmbientTable.at(level);
        long away = offline.take(level);
        if (away > 0L)
            Catchup.settle(this, HeatSource.spanBelow(level, worldPosition, away), ambient, away);
        Heat.tick(this, fireTu, ambient, fireTu.value() > ambient.value());
        setChanged();

        if (temperature > serviceTu() && level.random.nextFloat() < 0.02f
                || sealed && PressureBurst.fails(getPressure(), FTuning.ENVELOPE_DANGER_PU, FTuning.ENVELOPE_CRITICAL_PU, level.random)) {
            shatter();
            return;
        }

        if (hold > 0)
            hold--;
        else
            drive = 0;
        boolean vacuum = sealed && getPressure().value() <= FTuning.SCREEN_MIN_VACUUM_PU;
        int glow = vacuum ? Math.min(drive, ceiling()) : 0;
        if (glow == 3)
            burn = Math.min(1f, burn + FTuning.SCREEN_BURN_PER_TICK);
        if (glow != getBlockState().getValue(PhosphorScreenBlock.GLOW))
            level.setBlock(worldPosition, getBlockState().setValue(PhosphorScreenBlock.GLOW, glow), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.putBoolean("Sealed", sealed);
        tag.putFloat("SealTu", sealTu);
        tag.putFloat("Burn", burn);
        offline.save(tag, level);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        sealed = tag.getBoolean("Sealed");
        sealTu = tag.getFloat("SealTu");
        burn = tag.getFloat("Burn");
        offline.load(tag);
    }
}
