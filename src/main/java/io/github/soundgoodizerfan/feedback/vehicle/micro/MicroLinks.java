// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import io.github.soundgoodizerfan.feedback.machine.bearing.Joint;
import io.github.soundgoodizerfan.feedback.machine.bearing.JointBlock;
import io.github.soundgoodizerfan.feedback.machine.bearing.JointPart;
import io.github.soundgoodizerfan.feedback.machine.bearing.RotaryBearingBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A linkage: a joint voxel that ties two bodies that already exist, instead of making a new one from what is in front of
 * it. Used with the Joint Link item: click a joint voxel, then a voxel of the other body beside it. The same
 * {@link Joint} then holds the other body to the joint voxel's own, flagged as linked, so putting it back only lets it
 * go. A chain closed this way is a loop, which Sable solves as it can; the Lift reports loops rather than refusing them.
 */
public final class MicroLinks {

    /** How far, in blocks, the other body's voxel may be from the joint's face: a linkage ties what is beside it. */
    private static final double REACH = 1.5;

    private record Pending(GlobalPos cell, int index) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private MicroLinks() {
    }

    /** The Joint Link item used on voxel {@code index} of {@code cell}: the first click selects a joint voxel, the second names the body it ties. */
    public static void use(Player player, MicroCellBlockEntity cell, int index) {
        ServerLevel level = (ServerLevel) cell.getLevel();
        GlobalPos here = GlobalPos.of(level.dimension(), cell.getBlockPos());
        Pending first = PENDING.remove(player.getUUID());
        BlockState state = cell.get(index);
        if (first == null) {
            if (state == null || !(state.getBlock() instanceof JointPart) || cell.bearing(index) != null) {
                player.displayClientMessage(Component.translatable("message.feedback.link.pick_joint"), true);
                return;
            }
            PENDING.put(player.getUUID(), new Pending(here, index));
            io.github.soundgoodizerfan.feedback.net.AimPayloads.linkFrom(player, null, state.getBlock().getName());
            return;
        }
        String result = first.cell().dimension() == level.dimension() && level.isLoaded(first.cell().pos())
                && level.getBlockEntity(first.cell().pos()) instanceof MicroCellBlockEntity joint
                ? link(level, joint, first.index(), cell, index) : "failed";
        io.github.soundgoodizerfan.feedback.net.AimPayloads.linkDone(player);
        player.displayClientMessage(Component.translatable("message.feedback.link." + result), true);
    }

    private static String link(ServerLevel level, MicroCellBlockEntity jointCell, int jointIndex, MicroCellBlockEntity otherCell, int otherIndex) {
        BlockState state = jointCell.get(jointIndex);
        if (state == null || !(state.getBlock() instanceof JointPart part) || jointCell.bearing(jointIndex) != null)
            return "failed";
        ServerSubLevel parent = Joint.parentOf(level, jointCell.getBlockPos());
        ServerSubLevel other = Joint.parentOf(level, otherCell.getBlockPos());
        if (other == null)
            return "other_world"; // the world is not a body to hold
        if (other == parent)
            return "same_body";
        Direction f = state.getValue(RotaryBearingBlock.FACING);
        Vector3d face = faceCentre(jointCell.getBlockPos(), jointIndex, f);
        // The joint's face and the other body's voxel, both in the world, must be close.
        Vec3 faceWorld = parent == null ? new Vec3(face.x, face.y, face.z) : parent.logicalPose().transformPosition(new Vec3(face.x, face.y, face.z));
        Vec3 voxel = other.logicalPose().transformPosition(new Vec3(otherCell.getBlockPos().getX() + ((otherIndex & 3) + 0.5) / 4,
                otherCell.getBlockPos().getY() + ((otherIndex >> 2 & 3) + 0.5) / 4, otherCell.getBlockPos().getZ() + ((otherIndex >> 4) + 0.5) / 4));
        if (faceWorld.distanceTo(voxel) > REACH)
            return "too_far";
        int variant = part.kind() == Joint.Kind.HINGE ? VoxelNode.role(state)
                : state.hasProperty(JointBlock.VARIANT) ? state.getValue(JointBlock.VARIANT) : 1;
        Joint joint = Joint.of(part.kind(), variant, other, face, f, jointCell.getBlockPos(), parent);
        joint.markLinked();
        jointCell.setBearing(jointIndex, joint);
        return "linked";
    }

    private static Vector3d faceCentre(BlockPos cell, int index, Direction f) {
        return new Vector3d(cell.getX() + ((index & 3) + 0.5 + 0.5 * f.getStepX()) / 4, cell.getY() + ((index >> 2 & 3) + 0.5 + 0.5 * f.getStepY()) / 4,
                cell.getZ() + ((index >> 4) + 0.5 + 0.5 * f.getStepZ()) / 4);
    }
}
