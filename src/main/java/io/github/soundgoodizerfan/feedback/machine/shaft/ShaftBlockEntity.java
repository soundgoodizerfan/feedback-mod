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
package io.github.soundgoodizerfan.feedback.machine.shaft;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A relay. Generates nothing and demands nothing while stopped, but drags once it turns: every
 * shaft charges bearing friction to the network in proportion to speed.
 * <p>
 * So a sprawling layout costs Su that a compact one does not, and a long run cannot be driven as
 * fast as a short one -- not because anything forbids it, but because friction runs out of
 * torque first.
 */
public class ShaftBlockEntity extends RotationNode {

    public ShaftBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.SHAFT.get(), pos, state);
    }

    /** One block entity type serves both the Shaft and the Bearing; the figures live on the block,
     * the same split {@code CogBlockEntity} already uses for its two sizes. */
    @Override
    public Drag getDragSuPerRpm() {
        return getBlockState().getBlock() instanceof ShaftBlock shaft
                ? shaft.getDragSuPerRpm()
                : FTuning.SHAFT_DRAG_SU_PER_RPM;
    }

    @Override
    public Inertia getInertia() {
        return getBlockState().getBlock() instanceof ShaftBlock shaft
                ? shaft.getInertia()
                : FTuning.SHAFT_INERTIA;
    }
}
