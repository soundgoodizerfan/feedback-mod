// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import dev.ryanhcode.sable.companion.SableCompanion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3d;

/** Where a belt's ends are, when one pulley may stand in the world and the other in a body: both through the world. */
final class BeltGeometry {

    private BeltGeometry() {
    }

    /** A point in the frame of the block at {@code inFrameOf} (a body's plot, or the world), as a world point. */
    static Vec3 toWorld(Level level, BlockPos inFrameOf, Vec3 point) {
        var sub = SableCompanion.INSTANCE.getContaining(level, inFrameOf);
        return sub == null ? point : sub.logicalPose().transformPosition(point);
    }

    /** The centre of voxel {@code index} of the micro cell at {@code cell}, as a world point. */
    static Vec3 voxelWorld(Level level, BlockPos cell, int index) {
        return toWorld(level, cell, new Vec3(cell.getX() + ((index & 3) + 0.5) / 4, cell.getY() + ((index >> 2 & 3) + 0.5) / 4,
                cell.getZ() + ((index >> 4) + 0.5) / 4));
    }

    /** A world direction as the frame of the block at {@code inFrameOf} sees it. */
    static Vec3 direction(Level level, BlockPos inFrameOf, Vec3 world) {
        var sub = SableCompanion.INSTANCE.getContaining(level, inFrameOf);
        if (sub == null)
            return world;
        Vector3d v = sub.logicalPose().transformNormalInverse(new Vector3d(world.x, world.y, world.z), new Vector3d());
        return new Vec3(v.x, v.y, v.z);
    }
}
