// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import io.github.soundgoodizerfan.feedback.control.Analog;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.machine.bearing.Joint;
import io.github.soundgoodizerfan.feedback.machine.bearing.JointPart;
import io.github.soundgoodizerfan.feedback.machine.bearing.RotaryBearingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * One joint voxel of a micro cell as a part a controller card can set (vehicles v2 spec: the card
 * names a part by its cell and voxel). A piston takes a level for its travel; a hinge set to steer or
 * to a control surface takes a level for its angle; a driven hinge takes a brake level. Nothing links
 * to it; a card targets it directly, as every vehicle part.
 */
public final class VoxelNode implements DataNode, Analog, io.github.soundgoodizerfan.feedback.control.Switchable {

    private final MicroCellBlockEntity cell;
    private final int index;
    private final Set<DataNodeRef> links = new HashSet<>();

    VoxelNode(MicroCellBlockEntity cell, int index) {
        this.cell = cell;
        this.index = index;
    }

    /** True if the voxel is a part a card can set: a piston, or a Rotary Bearing (not a ring gear). */
    public static boolean settable(@Nullable BlockState state) {
        if (state != null && state.getBlock() instanceof io.github.soundgoodizerfan.feedback.machine.clutch.ClutchBlock)
            return true;
        if (state == null || !(state.getBlock() instanceof JointPart part))
            return false;
        return part.kind() == Joint.Kind.PISTON || part.kind() == Joint.Kind.HINGE;
    }

    private boolean clutch() {
        return cell.get(index) != null && cell.get(index).getBlock() instanceof io.github.soundgoodizerfan.feedback.machine.clutch.ClutchBlock;
    }

    /** A clutch voxel is engaged or let go; any other settable voxel is full or off (a piston at one end, a brake on or off). */
    @Override
    public void setEngaged(boolean engaged) {
        BlockState state = cell.get(index);
        if (state == null)
            return;
        if (clutch()) {
            if (state.getValue(io.github.soundgoodizerfan.feedback.machine.clutch.ClutchBlock.ENGAGED) == engaged)
                return;
            // The voxel takes the other state; the cell makes its rotor anew and the runs on both sides are rebuilt.
            cell.set(index, state.setValue(io.github.soundgoodizerfan.feedback.machine.clutch.ClutchBlock.ENGAGED, engaged));
            cell.commit();
        } else
            setLevel(0, engaged ? 1 : 0);
    }

    @Override
    public boolean isEngaged() {
        BlockState state = cell.get(index);
        if (state == null)
            return false;
        return clutch() ? state.getValue(io.github.soundgoodizerfan.feedback.machine.clutch.ClutchBlock.ENGAGED) : cell.level(index) > 0;
    }

    @Override
    public void setLevel(int channel, float level) {
        BlockState state = cell.get(index);
        if (!settable(state) || channel != 0)
            return;
        if (clutch()) {
            setEngaged(level > 0.5f);
            return;
        }
        if (state.getBlock() instanceof JointPart part && part.kind() == Joint.Kind.HINGE && role(state) == 0)
            cell.setBrake(index, level);
        else
            cell.setLevel(index, level);
    }

    static int role(BlockState state) {
        return state.hasProperty(RotaryBearingBlock.ROLE) ? state.getValue(RotaryBearingBlock.ROLE) : 0;
    }

    @Override
    public String[] channelKeys() {
        BlockState state = cell.get(index);
        if (state == null)
            return new String[0];
        if (clutch())
            return new String[]{"feedback.channel.clutch"};
        if (state.getBlock() instanceof JointPart part && part.kind() == Joint.Kind.PISTON)
            return new String[]{"feedback.channel.piston_level"};
        return new String[]{role(state) == 0 ? "feedback.channel.brake" : "feedback.channel.joint_angle"};
    }

    @Override
    public BlockPos getNodePos() {
        return cell.getBlockPos();
    }

    @Nullable
    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public DataNodeRef getNodeRef() {
        return new DataNodeRef(cell.getBlockPos(), null, index);
    }

    @Override
    public Level getNodeLevel() {
        return cell.getLevel();
    }

    @Override
    public BlockEntity getNodeOwner() {
        return cell;
    }

    @Override
    public Set<DataNodeRef> getNodeLinks() {
        return links;
    }

    @Override
    public boolean acceptsLinks() {
        return false;
    }

    @Override
    public boolean isNodeValid() {
        return !cell.isRemoved() && settable(cell.get(index));
    }
}
