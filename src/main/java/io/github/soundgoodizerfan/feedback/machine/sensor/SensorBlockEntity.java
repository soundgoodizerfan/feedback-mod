// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.sensor;

import java.util.HashSet;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.registry.FAutomation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A linkable sensor. It holds no state of its own except a tell-tale's last seen block state and the ticks left on its pulse; every other
 * reading is taken from the world when a controller asks, so a sensor costs nothing while nobody reads it. Failure characters: a tell-tale
 * lags the change by a tick, a photocell reads its own lamp.
 */
public class SensorBlockEntity extends BlockEntity implements DataNode, ChannelSource {

    /** Ticks a tell-tale stays high after the state beside it changes. */
    public static final int PULSE_TICKS = 10;

    private final Set<DataNodeRef> links = new HashSet<>();
    private BlockState seen;
    private int pulse;

    public SensorBlockEntity(BlockPos pos, BlockState state) {
        super(FAutomation.SENSOR_ENTITY.get(), pos, state);
    }

    private SensorKind kind() {
        return ((SensorBlock) getBlockState().getBlock()).kind();
    }

    private Direction face() {
        return getBlockState().getValue(SensorBlock.FACING);
    }

    int pulse() {
        return pulse;
    }

    /** One tick of a tell-tale: notice the block beside it has changed state, and count the pulse down. */
    public void tickServer() {
        if (level == null)
            return;
        BlockState now = level.getBlockState(worldPosition.relative(face()));
        if (seen != null && !seen.equals(now))
            pulse = PULSE_TICKS;
        else if (pulse > 0)
            pulse--;
        seen = now;
    }

    @Override
    public float read(int channel) {
        return level == null ? 0f : kind().read(level, worldPosition, face(), this);
    }

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }

    /** Targeted by a card, never linked. */
    @Override
    public boolean acceptsLinks() {
        return false;
    }

    @Override
    public Set<DataNodeRef> getNodeLinks() {
        return links;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Pulse", pulse);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pulse = tag.getInt("Pulse");
    }
}
