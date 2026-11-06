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
