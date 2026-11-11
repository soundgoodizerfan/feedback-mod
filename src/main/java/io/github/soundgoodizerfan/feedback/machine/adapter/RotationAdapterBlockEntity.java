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
package io.github.soundgoodizerfan.feedback.machine.adapter;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** A Rotation Adapter on the network: a relay, a gearbox's drag and mass. The coupling is the block's and the propagator's. */
public class RotationAdapterBlockEntity extends RotationNode {

    public RotationAdapterBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ROTATION_ADAPTER.get(), pos, state);
    }

    @Override
    public Drag getDragSuPerRpm() {
        return FTuning.ROTATION_ADAPTER_DRAG_SU_PER_RPM;
    }

    @Override
    public Inertia getInertia() {
        return FTuning.ROTATION_ADAPTER_INERTIA;
    }

    /**
     * Once a tick: feeds each run the four voxels at the middle of the cell in front belong to, at this adapter's own
     * speed (each voxel's sign), so a wheel's run can stall, slip or overstress without taking the others' with it.
     */
    public void tickServer() {
        if (level == null || level.isClientSide || getNetwork() == null)
            return;
        net.minecraft.core.Direction face = RotationAdapterBlock.microFace(getBlockState());
        if (face == null || !level.isLoaded(worldPosition.relative(face))
                || !(level.getBlockEntity(worldPosition.relative(face)) instanceof io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity cell))
            return;
        java.util.List<io.github.soundgoodizerfan.feedback.core.rotation.RotationMember> outs = new java.util.ArrayList<>(4);
        float[] wants = new float[4];
        float rpm = getRpm().value();
        for (int i : io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator.centralFace(face.getOpposite())) {
            var rotor = cell.rotor(i);
            if (rotor == null)
                continue;
            float sign = io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator.microLink(level, cell.getBlockPos(), rotor.state(), face.getOpposite());
            if (sign == 0)
                continue;
            wants[outs.size()] = rpm * sign;
            outs.add(rotor);
        }
        io.github.soundgoodizerfan.feedback.core.rotation.RotationSplit.drive(getNetwork(), outs, wants);
    }
}
