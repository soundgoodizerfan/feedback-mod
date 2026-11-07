// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.sculk;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.core.xp.XpNetworks;
import io.github.soundgoodizerfan.feedback.core.xp.XpNode;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The warden's relay organ grown into a node: every relay within range of another joins its XP run, no wire between.
 * The partners are found on demand when a run is rebuilt, so a relay ticks nothing.
 */
public class SculkRelayBlockEntity extends SculkNodeBlockEntity {

    public SculkRelayBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.SCULK_RELAY.get(), pos, state);
    }

    @Override
    public List<XpNode> bridges() {
        List<XpNode> out = new ArrayList<>(2);
        out.addAll(SculkSites.relaysNear(level, worldPosition));
        return out;
    }

    @Override
    protected void joined() {
        SculkSites.addRelay(level, worldPosition);
        rebuildPartners();
    }

    @Override
    protected void left() {
        rebuildPartners();
    }

    /** A relay appearing or going changes who is in range, so each relay in range has its run rebuilt. */
    private void rebuildPartners() {
        for (SculkRelayBlockEntity other : SculkSites.relaysNear(level, worldPosition))
            XpNetworks.of(level).queue(other.getBlockPos());
    }
}
