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
package io.github.soundgoodizerfan.feedback.core.rotation;

import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The whitelist of rotation parts that may be a micro-block voxel ({@code docs/feedback_micro_spec.md},
 * <em>Micro rotation nodes</em>): pure transmission -- shafts, bearings, flywheels, cogs, gearboxes.
 * A machine, a source or anything with an inventory never implements this. Its full-size drag and
 * inertia are stated here because a voxel has no block entity to ask; the voxel scales them.
 */
public interface MicroRotation extends Rotatable {

    Drag getDragSuPerRpm();

    Inertia getInertia();

    /** The block's own placement; every {@code Block} already has it. */
    @Nullable
    BlockState getStateForPlacement(BlockPlaceContext context);

    /**
     * The state this part takes as a voxel placed against {@code against}, the voxel clicked (null if
     * none). A part whose full-size placement reads the block it is placed against (a cog takes a
     * shaft's axis) says so here, because for a voxel that block is the clicked voxel, not anything
     * the world holds behind the cell.
     */
    @Nullable
    default BlockState getStateForPlacement(BlockPlaceContext context, @Nullable BlockState against) {
        return getStateForPlacement(context);
    }
}
