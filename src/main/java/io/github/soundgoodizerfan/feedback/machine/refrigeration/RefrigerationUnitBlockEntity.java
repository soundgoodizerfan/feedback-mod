// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.refrigeration;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.Refrigerable;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.condenser.CondenserBlockEntity;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A heat pump (food spec 8, Refrigeration; roadmap SV4). A sink: while it is fed it holds every
 * {@link Refrigerable} touching it cold with no ice, and the room it opens onto ({@link ColdRoom}):
 * the room is one temperature that drifts toward what the running units can hold against the walls'
 * leak, and every Ice Box and loose item in it settles there. A Condenser placed against the unit is
 * a compressor stage that lets it reach the frozen set point. It has no thermostat and no switch; the
 * player's breaker is the controller. The room is recomputed on a poll and never stored; units that
 * share a room each compute the same target, so there is nothing to share.
 *
 * <p>A pump moves heat, it does not delete it: running drives the unit's own temperature up toward
 * a ceiling and off lets it fall, so its radiator burns a bare hand ({@code HotContact} reads any
 * {@link ThermalBody}). That hot spot and the Eu drawn are the whole cost.
 */
public class RefrigerationUnitBlockEntity extends ElectricNode implements ThermalBody {

    private float temperature = FTuning.AMBIENT_TU.value();
    private boolean running;
    private float roomTu = FTuning.AMBIENT_TU.value();
    private float needAmps = FTuning.REFRIGERATION_AMPS;
    private ColdRoom.Scan scan;
    private long nextFill;

    public RefrigerationUnitBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.REFRIGERATION_UNIT.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return needAmps;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.LV;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        if (level == null)
            return;
        running = ElectricRules.rate(volts, amps, wanted, ratedBand()) > 0f;
        Tu ambient = AmbientTable.at(level);
        Heat.tick(this, running ? FTuning.REFRIGERATION_HOT_TU : ambient, ambient, running);
        setChanged();
    }

    /** A Condenser against it is a compressor stage: the unit can reach the frozen set point. */
    public boolean staged() {
        if (level == null)
            return false;
        for (Direction side : Direction.values())
            if (level.getBlockEntity(worldPosition.relative(side)) instanceof CondenserBlockEntity)
                return true;
        return false;
    }

    /** Once a second: hold boxes beside it and in the room, drift the room, settle what is in it. */
    public void tickServer() {
        if (level == null || level.getGameTime() % 20 != 0)
            return;
        room();
        // Last, so a box against the unit holds the pump's own set point, not the room's slower drift.
        if (running) {
            Tu own = staged() ? FTuning.REFRIGERATION_FROZEN_TU : FTuning.ICEBOX_COLD_TU;
            for (Direction side : Direction.values())
                if (level.getBlockEntity(worldPosition.relative(side)) instanceof Refrigerable box)
                    box.holdCold(own);
        }
    }

    private void room() {
        long now = level.getGameTime();
        float outside = AmbientTable.at(level).value();
        if (scan == null || now >= nextFill) {
            scan = ColdRoom.scan(level, worldPosition);
            nextFill = now + FTuning.ROOM_FILL_TICKS + Math.floorMod(worldPosition.hashCode(), 20);
        }
        if (scan.volume() == 0) {
            roomTu = outside;
            needAmps = FTuning.REFRIGERATION_AMPS;
            return;
        }
        int pumping = running ? 1 : 0;
        boolean frozen = staged();
        for (RefrigerationUnitBlockEntity u : scan.units())
            if (u != this) {
                pumping += u.running ? 1 : 0;
                frozen |= u.staged();
            }
        float set = (frozen ? FTuning.REFRIGERATION_FROZEN_TU : FTuning.ICEBOX_COLD_TU).value();
        float leak = Math.max(1e-6f, scan.leak());
        // A pump moves heat out, so it never warms a room that is already colder than its set point.
        float target = pumping > 0 ? Math.min(outside, Math.max(set, outside - pumping * FTuning.ROOM_PULL / leak)) : outside;
        roomTu += (target - roomTu) * FTuning.ROOM_DRIFT_PER_SECOND / (1f + scan.volume() / FTuning.ROOM_VOLUME_REF);
        // The draw follows the leak: what holding the room at its set point asks, shared among the units on it.
        float share = leak * Math.max(0f, outside - set) / ((1 + scan.units().size()) * FTuning.ROOM_PULL);
        needAmps = Math.min(FTuning.REFRIGERATION_AMPS * 4f, Math.max(FTuning.REFRIGERATION_AMPS * 0.25f, FTuning.REFRIGERATION_AMPS * share));
        if (Math.abs(roomTu - outside) < 0.5f)
            return;
        if (pumping > 0)
            for (Refrigerable box : scan.boxes())
                box.holdCold(new Tu(roomTu));
        AABB bounds = AABB.encapsulatingFullBlocks(scan.min(), scan.max());
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, bounds))
            if (scan.interior().contains(item.blockPosition().asLong()))
                Cooking.settle(item.getItem(), level, roomTu);
    }

    /** Blocks of air in the room it last found (0 when it faces none, or before its first poll). */
    public int roomVolume() {
        return scan == null ? 0 : scan.volume();
    }

    public int roomOpenings() {
        return scan == null ? 0 : scan.openings();
    }

    /** What the room's walls and openings leak, Work per tick per Tu. */
    public float roomLeak() {
        return scan == null ? 0f : scan.leak();
    }

    /** Where the room leaks, up to 64 positions. */
    public java.util.List<BlockPos> roomOpeningAt() {
        return scan == null ? java.util.List.of() : scan.openingAt();
    }

    public float roomTemperature() {
        return roomTu;
    }

    /** Whether the last resolve left it fed enough to pump. */
    public boolean running() {
        return running;
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
        return FTuning.REFRIGERATION_MASS;
    }

    @Override
    public Conductance getLeak() {
        return FTuning.REFRIGERATION_LEAK;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.putFloat("RoomTu", roomTu);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        roomTu = tag.contains("RoomTu") ? tag.getFloat("RoomTu") : FTuning.AMBIENT_TU.value();
    }
}
