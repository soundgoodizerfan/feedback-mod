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
package io.github.soundgoodizerfan.feedback.machine.dynamo;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * The dynamo's winding figure run backwards (mechanics 6.8): the voltage it sees sets the speed it
 * drives toward, and the current it is delivered sets the torque, by the same {@code voltsPerRpm}.
 * A dynamo feeding a motor on one winding, or a motor turning a dynamo on the other, therefore
 * passes power through unchanged apart from the shaft's own friction -- which is what makes the
 * pair a voltage converter (the motor-generator set) without a constant anywhere.
 *
 * <h2>The torque-speed line</h2>
 * A real DC motor's back-EMF rises with speed until it cancels the supply, so the current it
 * draws falls linearly from stall to nothing at its no-load speed, {@code volts / voltsPerRpm}.
 * Resistance is not modelled (philosophy 17), so the winding's rated amps stand in for the stall
 * current. Consequences nobody wrote: an idle motor draws next to nothing, a loaded one slows
 * until the current it pulls carries the load, an under-voltage motor runs slow rather than not
 * at all (6.5), and an over-voltage one runs fast -- the overspeed trip already answers that.
 *
 * <h2>How it reaches the shaft</h2>
 * Not a {@link RotationNode} for the dynamo's reason. It drives the run on its output face
 * through {@code RotationNetwork.driveByBelt}, every tick, exactly as a belt drives a run with no
 * source of its own; on a run that has one, its torque adds and the source keeps the speed.
 */
public class MotorBlockEntity extends ElectricNode {

    /** The output shaft's speed last tick. Not persisted, as the dynamo's. */
    private float shaftRpm;

    public MotorBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.MOTOR.get(), pos, state);
    }

    private DynamoBlock.Winding winding() {
        return getBlockState().getValue(DynamoBlock.WINDING);
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    /** The speed the voltage it sees would carry it to with no load at all. */
    public float noLoadRpm() {
        return getVolts() / winding().voltsPerRpm;
    }

    @Override
    public float wantedAmps() {
        float free = noLoadRpm();
        // Seeing no voltage yet reads as stalled, so it asks for the stall current and is told.
        float back = free > 0 ? Math.min(1f, shaftRpm / free) : 0f;
        return winding().ratedAmps * (1f - back);
    }

    /** Coarse is built for a cell's band, fine for the next -- the dynamo's own anchors. */
    @Override
    public VoltageBand ratedBand() {
        return winding() == DynamoBlock.Winding.COARSE ? VoltageBand.LV : VoltageBand.MV;
    }

    /** The torque the current delivered last tick puts on the shaft, in Su. */
    public Su getShaftTorque() {
        return new Su(getAmps() * winding().voltsPerRpm);
    }

    public float getShaftRpm() {
        return shaftRpm;
    }

    public void tickServer() {
        RotationNode shaft = outputShaft();
        shaftRpm = shaft == null ? 0 : Math.abs(shaft.getRpm().value());
        if (shaft == null || shaft.getNetwork() == null || shaft.getRatio() == 0 || getAmps() <= 0)
            return;
        // Into the run's own frame through the coupling node's ratio, as a pulley does.
        shaft.getNetwork().driveByBelt(new Rpm(noLoadRpm() / shaft.getRatio()), getShaftTorque());
    }

    /** The rotation node on the output face whose own axle points at this block, or null. */
    @Nullable
    private RotationNode outputShaft() {
        if (level == null)
            return null;
        Direction facing = getBlockState().getValue(DynamoBlock.FACING);
        BlockPos at = worldPosition.relative(facing);
        if (!level.isLoaded(at))
            return null;
        BlockState state = level.getBlockState(at);
        return state.getBlock() instanceof Rotatable rotatable
                && rotatable.hasShaftTowards(level, at, state, facing.getOpposite())
                && level.getBlockEntity(at) instanceof RotationNode node ? node : null;
    }
}
