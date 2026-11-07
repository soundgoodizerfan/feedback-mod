// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.waterwheel;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Slice 1: 256 Su, continuous, needs flowing water.
 * <p>
 * Speed depends on how many sides are being driven, which makes siting the wheel a real
 * decision rather than a placement. Placeholder RPM -- the slice fixes Su, never speed.
 */
public class WaterWheelBlockEntity extends RotationNode {

    private int flowingSides;
    private int ticksUntilFlowCheck;

    public WaterWheelBlockEntity(BlockPos pos, BlockState state) {
        this(FBlockEntities.WATER_WHEEL.get(), pos, state);
    }

    /** For a wheel variant (the fish wheel) that is the same wheel under another block entity type. */
    protected WaterWheelBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** How many blade faces moving water presses on, as last counted. */
    public int flowing() {
        return flowingSides;
    }

    public void tickServer() {
        if (level == null)
            return;
        if (--ticksUntilFlowCheck > 0)
            return;
        ticksUntilFlowCheck = FTuning.WATER_WHEEL_FLOW_CHECK_INTERVAL;

        int previous = flowingSides;
        flowingSides = countFlowingSides();
        if (previous != flowingSides)
            RotationPropagator.rebuildFrom(level, worldPosition);
    }

    /**
     * Count faces with moving water against them, ignoring the two the axle passes through --
     * water there would be hitting the hub rather than a blade.
     */
    private int countFlowingSides() {
        Direction.Axis axis = getBlockState().getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS);
        int count = 0;
        for (Direction face : Direction.values()) {
            if (face.getAxis() == axis)
                continue;
            FluidState fluid = level.getFluidState(worldPosition.relative(face));
            if (!fluid.isEmpty() && !fluid.isSource())
                count++;
        }
        return count;
    }

    @Override
    public Inertia getInertia() {
        return FTuning.WATER_WHEEL_INERTIA;
    }

    @Override
    public Rpm getGeneratedRpm() {
        return new Rpm(flowingSides * FTuning.WATER_WHEEL_RPM_PER_FLOW.value());
    }

    @Override
    public Su getCapacitySu() {
        return FTuning.WATER_WHEEL_CAPACITY_SU;
    }
}
