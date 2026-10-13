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
package io.github.soundgoodizerfan.feedback.machine.cable;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.SidedFitting;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.fitting.sensor.CurrentSensorFitting;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * A wire's physics: a {@link ThermalBody} that warms only when it carries more than it is rated
 * for, and fails in three readable stages (mechanics 6.4).
 *
 * <h2>Inside its rating, nothing happens</h2>
 * A wire carrying at or below its rating is given no heat at all, so it sits at ambient forever.
 * That is the wear rule applied to cable: a correctly built line never accrues any, and a hot
 * wire is evidence of one specific mistake.
 *
 * <h2>Over it, the existing thermal model is the whole mechanism</h2>
 * Work per tick goes in for every amp over the rating ({@link Heat#warm}), and the wire leaks to
 * the room exactly as a vessel does ({@link Heat#tick} with nothing firing it). It therefore
 * settles where input equals leak and can never run away -- the same difference-driven shape that
 * stopped insulation being a trap in the crucible. No simulator was added.
 *
 * <h2>Three stages, never a surprise</h2>
 * <ol>
 *   <li>The insulation layer burns off above {@link FTuning#WIRE_INSULATION_LIMIT_TU}. The
 *       conductor is untouched and still carries; it now shocks (see {@code CableBlock}).</li>
 *   <li>Above the material's service temperature the conductor degrades: condition falls,
 *       its rating shrinks and its drop grows.</li>
 *   <li>Condition reaching zero destroys the wire. This is the single place destruction exists
 *       (mechanics 6.4): it is the end of a path the player was visibly walking, reached only by
 *       leaving degradation unfixed. A wire is replaced; <b>[OPEN]</b> whether it can be mended.</li>
 * </ol>
 */
public class CableBlockEntity extends ElectricNode implements ThermalBody, Fittable {

    private final SidedFitting[] fittings = new SidedFitting[Direction.values().length];

    private float temperature = FTuning.AMBIENT_TU.value();
    private float condition = 1f;
    private boolean insulated = true;

    // Figures read from the material file, refreshed when the table reloads.
    private int figuresGeneration = -1;
    private float baseRating;
    private float baseDrop;
    private float serviceTu = Float.MAX_VALUE;

    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CABLE.get(), pos, state);
    }

    // --- role and figures ---------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.WIRE;
    }

    private void figures() {
        if (figuresGeneration == MaterialTable.generation())
            return;
        figuresGeneration = MaterialTable.generation();
        baseRating = 0;
        baseDrop = 0;
        serviceTu = Float.MAX_VALUE;
        if (!(getBlockState().getBlock() instanceof CableBlock block))
            return;
        Material material = MaterialTable.get().materials().get(block.material());
        if (material == null)
            return;
        // Absent means unmeasured, never zero (mechanics 5.6): an unmeasured material is a wire
        // that carries nothing, which is a pack error that shows on the first current.
        baseRating = material.ampacity().orElse(0f) * FTuning.WIRE_AMPS_PER_AMPACITY;
        baseDrop = material.electrical().drop().orElse(0f);
        material.thermal().serviceTemperature().ifPresent(tu -> serviceTu = tu.value());
    }

    /** Amps it carries before it heats, falling with the conductor's condition. */
    public float getRatingAmps() {
        figures();
        return baseRating * condition;
    }

    @Override
    public float dropPerAmp() {
        figures();
        // A degraded conductor loses more per amp. Bounded so a nearly-gone one is bad, not infinite.
        return baseDrop / Math.max(0.1f, condition);
    }

    public float getCondition() {
        return condition;
    }

    public boolean isInsulated() {
        return insulated;
    }

    /** Bare and carrying: the state in which touching it hurts. */
    public boolean isShocking() {
        return !insulated && getAmps() > 0f;
    }

    // --- the resolver's answer ----------------------------------------------------------------

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        step(amps);
    }

    /**
     * One tick of this wire's own life, given the current through it. Public so a test can run it
     * without a source.
     */
    public void step(float amps) {
        Tu ambient = AmbientTable.at(level);
        float over = amps - getRatingAmps();
        if (over > 0f)
            Heat.warm(this, FTuning.WIRE_OVERLOAD_WORK_PER_AMP * over);
        Heat.tick(this, ambient, ambient, false);

        if (insulated && temperature > FTuning.WIRE_INSULATION_LIMIT_TU.value()) {
            insulated = false;
            setChanged();
        }

        figures();
        if (temperature > serviceTu) {
            condition -= FTuning.WIRE_DEGRADE_PER_TICK * (temperature - serviceTu) / serviceTu;
            setChanged();
            if (condition <= 0f)
                burnOut();
        }
    }

    private void burnOut() {
        condition = 0f;
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 8, 0.15, 0.15, 0.15, 0.01);
            server.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.4f);
            server.destroyBlock(worldPosition, false);
        }
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
        return FTuning.WIRE_MASS;
    }

    @Override
    public Conductance getLeak() {
        return FTuning.WIRE_LEAK;
    }

    // --- fitting ------------------------------------------------------------------------------

    @Nullable
    @Override
    public SidedFitting getSidedFitting(Direction side) {
        return fittings[side.get3DDataValue()];
    }

    @Override
    public void setSidedFitting(Direction side, @Nullable SidedFitting fitting) {
        fittings[side.get3DDataValue()] = fitting;
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // A wire takes sensors and nothing else: it has no numbers of its own that an upgrade could
    // fold into, so canAdd stays false and these are never reached.
    @Override
    public List<UpgradeFitting> getUpgrades() {
        return List.of();
    }

    @Override
    public boolean addUpgrade(UpgradeFitting upgrade) {
        return false;
    }

    @Override
    public void removeUpgrade(UpgradeFitting upgrade) {
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.putFloat("Condition", condition);
        tag.putBoolean("Insulated", insulated);
        // One concrete SidedFitting fits a wire, so this stays a direct check, as the crucible's does.
        for (Direction side : Direction.values())
            if (fittings[side.get3DDataValue()] instanceof CurrentSensorFitting sensor) {
                CompoundTag sensorTag = new CompoundTag();
                sensor.writeNbt(sensorTag);
                tag.put("Sensor" + side.get3DDataValue(), sensorTag);
            }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Temperature"))
            temperature = tag.getFloat("Temperature");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
        insulated = !tag.contains("Insulated") || tag.getBoolean("Insulated");
        for (Direction side : Direction.values()) {
            String key = "Sensor" + side.get3DDataValue();
            int i = side.get3DDataValue();
            if (!tag.contains(key)) {
                fittings[i] = null;   // removed since the last sync
                continue;
            }
            CurrentSensorFitting sensor = fittings[i] instanceof CurrentSensorFitting existing
                    ? existing : new CurrentSensorFitting(this, side);
            sensor.readNbt(tag.getCompound(key));
            fittings[i] = sensor;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
