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
package io.github.soundgoodizerfan.feedback.machine.sculk;

import java.util.HashSet;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;

/**
 * Listens for vibrations the way a vanilla sculk sensor does (same radius, same frequencies) and holds the last one
 * for a vanilla cooldown. A controller reads channel 0 as the frequency over 15, and channel 1 to 15 as whether that
 * exact frequency was just heard. Event-driven: it ticks nothing and costs one call per vibration in range.
 * <p>
 * shortcut: no wool dampening and no sneak filter, the listener hears every vibration game event in range; upgrade
 * if a sensor behind wool should be deaf.
 */
public class DataSensorBlockEntity extends BlockEntity
        implements DataNode, ChannelSource, GameEventListener.Provider<DataSensorBlockEntity.Ear> {

    private static final String LINKS = "Links";

    private final Set<DataNodeRef> links = new HashSet<>();
    private final Ear ear = new Ear();
    private int frequency;
    private long heardAt = Long.MIN_VALUE / 2;

    public DataSensorBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.DATA_SENSOR.get(), pos, state);
    }

    @Override
    public Ear getListener() {
        return ear;
    }

    /** The listening half, kept apart so the block entity itself is not a game event listener on every call path. */
    public final class Ear implements GameEventListener {
        private final PositionSource source = new BlockPositionSource(worldPosition);

        @Override
        public PositionSource getListenerSource() {
            return source;
        }

        @Override
        public int getListenerRadius() {
            return FTuning.DATA_SENSOR_RADIUS;
        }

        @Override
        public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 pos) {
            if (isRemoved() || !event.is(GameEventTags.VIBRATIONS))
                return false;
            frequency = VibrationSystem.getGameEventFrequency(event);
            heardAt = level.getGameTime();
            return true;
        }
    }

    private boolean fresh() {
        return level != null && level.getGameTime() - heardAt <= FTuning.DATA_SENSOR_HOLD_TICKS;
    }

    @Override
    public float read(int channel) {
        if (!fresh())
            return 0f;
        return channel == 0 ? frequency / 15f : (frequency == channel ? 1f : 0f);
    }

    // --- DataNode -----------------------------------------------------------------------------

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

    @Override
    public Set<DataNodeRef> getNodeLinks() {
        return links;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (DataNodeRef ref : links)
            list.add(ref.toTag());
        tag.put(LINKS, list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        links.clear();
        for (Tag entry : tag.getList(LINKS, Tag.TAG_COMPOUND))
            links.add(DataNodeRef.fromTag((CompoundTag) entry));
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide)
            unlinkAll();
        super.setRemoved();
    }
}
