// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.membrane;

import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.core.hazard.FumeCloud;
import io.github.soundgoodizerfan.feedback.core.hazard.HazardTuning;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A phantom membrane's film in a block: it answers to the volatiles a tired creature gives off, the same ones that draw phantoms to a sleepless
 * player. Channel 0 reads the fatigue of the most tired player within {@link MachineTuning#MEMBRANE_REACH} blocks as a share of the sleeplessness
 * that calls phantoms (0 to 1, saturating), channel 1 reads whether anyone is in reach at all. A creature that does not sleep gives off nothing
 * the film answers to. The film answers to the volatiles of a gas as well: channel 2 reads the strength of the strongest loose gas cloud in reach
 * (a share of {@link HazardTuning#FUME_READING_FULL_LEVEL}), channel 3 its class: 0.5 a gas that hangs in the air, 1 a heavy gas that settled
 * to the floor, 0 none. Measurement only: it ticks nothing and controls nothing, and what a controller does with a reading is the controller's
 * business.
 */
public class MembraneSensorBlockEntity extends BlockEntity implements DataNode, ChannelSource {

    public MembraneSensorBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.MEMBRANE_SENSOR.get(), pos, state);
    }

    @Override
    public float read(int channel) {
        if (!(level instanceof ServerLevel server))
            return 0f;
        float reach = MachineTuning.MEMBRANE_REACH;
        if (channel >= 2) {
            var gas = FumeCloud.strongest(server, net.minecraft.world.phys.Vec3.atCenterOf(worldPosition), reach);
            if (gas == null)
                return 0f;
            return channel == 2 ? Math.min(1f, gas.level() / (float) HazardTuning.FUME_READING_FULL_LEVEL) : gas.heavy() ? 1f : 0.5f;
        }
        int tired = 0;
        boolean near = false;
        for (ServerPlayer p : server.players()) {
            if (p.isSpectator() || p.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) > reach * reach)
                continue;
            near = true;
            tired = Math.max(tired, p.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)));
        }
        return channel == 1 ? (near ? 1f : 0f) : Math.min(1f, tired / (float) MachineTuning.MEMBRANE_PHANTOM_TICKS);
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
}
