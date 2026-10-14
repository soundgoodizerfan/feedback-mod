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
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * The shaft-in boundary of mechanics 6.8, and its figures come from the block's own physics, not
 * from a table converting one currency into another.
 *
 * <h2>One winding figure, used twice</h2>
 * A generator's voltage rises with the speed it is turned at, and the torque it takes rises with
 * the current drawn from it, by the <em>same</em> constant -- the winding's (in a real machine
 * {@code k_e = k_t}). So {@code voltsPerRpm} sets the voltage from the shaft's live RPM, and the
 * same number times the current delivered is the Su it charges back to that shaft run. That is
 * conservation stated once, not an exchange rate: nothing anywhere says "N Su per Eu", and a
 * dynamo that is not drawn from costs the shaft nothing.
 * <p>
 * Consequences nobody wrote: a slow river makes a low voltage, and gearing a dynamo up is how you
 * raise it -- at the price gearing already charges in friction. Overdraw sags it like any source
 * ({@code ElectricRules.sagFactor}).
 *
 * <h2>Why it is not a {@link RotationNode}</h2>
 * It is an {@link ElectricNode}, and a block entity can extend only one of the two. It couples to
 * the shaft run on its input face the way a Pulley's belt couples to a far run: it reads the
 * neighbour's speed and adds its draw through {@code RotationNetwork.loadByBelt}, every tick, so
 * neither propagator learns that the other exists. The draw is flat, not referred through the
 * shaft's gear ratio, the same choice {@code RotationNetwork.recalculate} makes for every
 * machine's working draw. It does not spin on screen yet: placeholder art.
 */
public class DynamoBlockEntity extends ElectricNode {

    /** The input shaft's speed last tick. Not persisted: one tick after loading it is read again. */
    private float shaftRpm;

    public DynamoBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.DYNAMO.get(), pos, state);
    }

    private DynamoBlock.Winding winding() {
        return getBlockState().getValue(DynamoBlock.WINDING);
    }

    @Override
    public Role role() {
        return Role.SOURCE;
    }

    @Override
    public float sourceVolts() {
        return shaftRpm * winding().voltsPerRpm;
    }

    @Override
    public float sourceAmps() {
        return shaftRpm > 0 ? winding().ratedAmps : 0;
    }

    /** The speed the coil is being turned at, for display and tests. */
    public float getShaftRpm() {
        return shaftRpm;
    }

    /** The torque the current drawn last tick costs the shaft, in Su. */
    public Su getShaftLoad() {
        return new Su(getAmps() * winding().voltsPerRpm);
    }

    public void tickServer() {
        RotationNode shaft = inputShaft();
        shaftRpm = shaft == null ? 0 : Math.abs(shaft.getRpm().value());
        if (shaft != null && shaft.getNetwork() != null && getAmps() > 0)
            shaft.getNetwork().loadByBelt(getShaftLoad());
    }

    /** The rotation node on the input face whose own axle points at this block, or null. */
    @Nullable
    private RotationNode inputShaft() {
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
