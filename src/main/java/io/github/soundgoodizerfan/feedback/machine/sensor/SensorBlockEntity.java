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
