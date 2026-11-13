// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.detector;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.phys.Vec3;

/**
 * A wire that hears sparks. It sits in a run as a wire and joins its neighbours only while
 * {@link #conducting()}; a spark within earshot ({@link #hear}) is what changes that. The two
 * detectors differ only in what a spark does to the conductor and what it takes to undo it.
 * Nothing here senses current: a detector is the sense, the run beyond it is the signal.
 */
public abstract class SparkDetectorBlockEntity extends ElectricNode
        implements GameEventListener.Provider<SparkDetectorBlockEntity.Ear> {

    private final Ear ear = new Ear();

    protected SparkDetectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Role role() {
        return Role.WIRE;
    }

    @Override
    public Ear getListener() {
        return ear;
    }

    /** A spark went off {@code distance} blocks away, inside the event's own radius. */
    protected abstract void hear(double distance);

    public class Ear implements GameEventListener {
        private final BlockPositionSource source = new BlockPositionSource(getBlockPos());

        @Override
        public net.minecraft.world.level.gameevent.PositionSource getListenerSource() {
            return source;
        }

        @Override
        public int getListenerRadius() {
            return ElectricTuning.SPARK_RANGE_BLOCKS;
        }

        @Override
        public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 origin) {
            if (!event.is(FWeapons.SPARK.getId()) || isRemoved())
                return false;
            hear(Math.sqrt(origin.distanceToSqr(Vec3.atCenterOf(getBlockPos()))));
            return true;
        }
    }
}
