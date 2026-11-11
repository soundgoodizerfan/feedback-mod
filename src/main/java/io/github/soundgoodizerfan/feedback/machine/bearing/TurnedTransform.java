// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.bearing;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;

import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroBearings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Sable's block move, turned: a body's blocks go from its plot back to the world as {@code k} quarter turns
 * right-handed about the axis through the bearing block {@code pivot}, which keeps full-size blocks on the grid.
 * Sable's own transform turns only about Y; this one turns about any axis and the states' direction and axis
 * properties with it.
 */
public class TurnedTransform extends SubLevelAssemblyHelper.AssemblyTransform {

    private final BlockPos pivot;
    private final Direction.Axis axis;
    private final int turns;

    public TurnedTransform(BlockPos homeFrom, BlockPos homeTo, BlockPos pivot, Direction.Axis axis, int turns, ServerLevel level) {
        super(homeFrom, homeTo, 0, Rotation.NONE, level);
        this.pivot = pivot;
        this.axis = axis;
        this.turns = turns;
    }

    @Override
    public BlockPos apply(BlockPos p) {
        return MicroBearings.turn(super.apply(p), pivot, axis, turns);
    }

    @Override
    public BlockState apply(BlockState state) {
        return MicroBearings.turn(super.apply(state), axis, turns);
    }

    @Override
    public Vec3 apply(Vec3 v) {
        Vec3 moved = super.apply(v);
        double x = moved.x - (pivot.getX() + 0.5), y = moved.y - (pivot.getY() + 0.5), z = moved.z - (pivot.getZ() + 0.5);
        for (int i = 0; i < turns; i++) {
            double t;
            switch (axis) {
                case X -> { t = y; y = -z; z = t; }
                case Y -> { t = z; z = -x; x = t; }
                case Z -> { t = x; x = -y; y = t; }
            }
        }
        return new Vec3(pivot.getX() + 0.5 + x, pivot.getY() + 0.5 + y, pivot.getZ() + 0.5 + z);
    }
}
