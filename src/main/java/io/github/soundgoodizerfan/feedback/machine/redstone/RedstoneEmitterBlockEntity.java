// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.redstone;

import java.util.Collections;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The data-to-redstone converter's state: engaged is the block's {@code POWERED}, so nothing needs saving or syncing by hand. */
public class RedstoneEmitterBlockEntity extends BlockEntity implements Switchable, DataNode {

    public RedstoneEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.REDSTONE_EMITTER.get(), pos, state);
    }

    @Override
    public void setEngaged(boolean engaged) {
        if (level == null || level.isClientSide || engaged == isEngaged())
            return;
        level.setBlock(worldPosition, getBlockState().setValue(RedstoneEmitterBlock.POWERED, engaged), net.minecraft.world.level.block.Block.UPDATE_ALL);
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    @Override
    public boolean isEngaged() {
        return getBlockState().getValue(RedstoneEmitterBlock.POWERED);
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
        return Collections.emptySet();
    }
}
