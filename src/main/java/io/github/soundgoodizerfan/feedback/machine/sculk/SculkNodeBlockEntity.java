// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.sculk;

import io.github.soundgoodizerfan.feedback.core.xp.XpNetwork;
import io.github.soundgoodizerfan.feedback.core.xp.XpNetworks;
import io.github.soundgoodizerfan.feedback.core.xp.XpNode;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The shared half of the sculk blocks that sit on the XP network: joining a run, leaving it, and the load and unload rules. */
public abstract class SculkNodeBlockEntity extends BlockEntity implements XpNode {

    @Nullable
    private XpNetwork network;
    private boolean unloaded;

    protected SculkNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    @Override
    public XpNetwork network() {
        return network;
    }

    @Override
    public void network(@Nullable XpNetwork network) {
        this.network = network;
    }

    /** Called on the server after the node has joined the world. */
    protected void joined() {
    }

    /** Called on the server as the node leaves the world (removed, not just unloaded). */
    protected void left() {
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            XpNetworks.of(level).queue(worldPosition);
            joined();
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        unloaded = true;
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            if (unloaded)
                XpNetworks.of(level).unloaded(this);
            else {
                XpNetworks.of(level).removed(this);
                left();
            }
            SculkSites.forget(level, this);
        }
        super.setRemoved();
    }
}
