// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.bearing;

import java.util.ArrayList;
import java.util.List;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;

import io.github.soundgoodizerfan.feedback.control.Analog;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A ball, locked, slider or piston joint in coarsespace: the same hold on a body ({@link Joint}) as the voxel kind in finespace
 * ({@code MicroBearings}), made the way a Rotary Bearing makes its body ({@link RotaryBearingBlockEntity}): everything connected to the
 * front face becomes a Sable body, joined here with the joint's own Sable joint. A piston's level is set by a controller card like a
 * voxel piston's ({@link Analog} channel 0, -1 to 1).
 */
public class JointBlockEntity extends BlockEntity implements Analog, DataNode {

    @Nullable
    private Joint joint;
    /** The piston's level; the other kinds ignore it. */
    private float pistonLevel;
    /** A locked joint's release was asked for by a card (set off the physics thread, acted on at the next server tick). */
    private boolean releaseAsked;
    /** The facing as last ticked: what a broken joint's body is put back about once the block's state is gone. */
    private Direction lastFacing;

    public JointBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.JOINT.get(), pos, state);
    }

    private JointBlock block() {
        return (JointBlock) getBlockState().getBlock();
    }

    private Direction facing() {
        return getBlockState().getValue(JointBlock.FACING);
    }

    /** The hold on its body, or null. */
    @Nullable
    public Joint joint() {
        return joint;
    }

    /** True while it holds a body. */
    public boolean holding() {
        return joint != null;
    }

    /** A Lift carried this joint's parent by {@code delta} blocks. */
    public void moved(BlockPos delta) {
        if (joint != null)
            joint.shift(delta);
    }

    /** Assembles, or puts back. Returns the message key (under {@code message.feedback.rotary_bearing.}). */
    public String toggle(ServerLevel level) {
        if (joint == null)
            return assemble(level);
        if (joint.docked()) {
            release(level);
            return "undocked";
        }
        return disassemble(level, facing());
    }

    private Vector3d anchor() {
        Direction f = facing();
        return new Vector3d(worldPosition.getX() + 0.5 + 0.5 * f.getStepX(), worldPosition.getY() + 0.5 + 0.5 * f.getStepY(),
                worldPosition.getZ() + 0.5 + 0.5 * f.getStepZ());
    }

    private String assemble(ServerLevel level) {
        Direction f = facing();
        BlockPos front = worldPosition.relative(f), host = worldPosition.relative(f.getOpposite());
        if (level.getBlockState(front).isAir())
            return dock(level, f);
        var gathered = SubLevelAssemblyHelper.gatherConnectedBlocks(front, level, FTuning.ROTARY_BEARING_MAX_BLOCKS,
                (a, aState, to, toState, direction) -> !to.equals(worldPosition) && !to.equals(host));
        if (gathered.assemblyState() == SubLevelAssemblyHelper.GatherResult.State.NO_BLOCKS)
            return "empty";
        if (gathered.assemblyState() != SubLevelAssemblyHelper.GatherResult.State.SUCCESS || gathered.blocks() == null)
            return "too_big";
        List<BlockPos> blocks = new ArrayList<>(gathered.blocks());
        // From a body too: Sable starts the new body at its parent's pose and speed, so nothing moves.
        ServerSubLevel parent = Joint.parentOf(level, worldPosition);
        BlockMoves.enter();
        ServerSubLevel made;
        try {
            made = SubLevelAssemblyHelper.assembleBlocks(level, front, blocks, BoundingBox3i.from(blocks));
        } finally {
            BlockMoves.leave();
        }
        joint = Joint.of(block().kind(), getBlockState().getValue(JointBlock.VARIANT), made, anchor(), f, front, parent);
        setChanged();
        return "assembled";
    }

    /**
     * Docking: a locked joint with no block in front of it, but another body touching the cell in front, joins that body as it stands
     * (a rigid hold at the pose it has now) and is let go again by the same use or the Release channel. Only the locked kind docks.
     */
    private String dock(ServerLevel level, Direction f) {
        if (block().kind() != Joint.Kind.LOCKED)
            return "empty";
        ServerSubLevel parent = Joint.parentOf(level, worldPosition);
        Vector3d probe = new Vector3d(worldPosition.getX() + 0.5 + f.getStepX(), worldPosition.getY() + 0.5 + f.getStepY(), worldPosition.getZ() + 0.5 + f.getStepZ());
        if (parent != null)
            parent.logicalPose().transformPosition(probe);
        for (ServerSubLevel other : ServerSubLevelContainer.getContainer(level).getAllSubLevels()) {
            if (other == parent || other.isRemoved())
                continue;
            Vector3d local = other.logicalPose().transformPositionInverse(new Vector3d(probe), new Vector3d());
            BlockPos at = BlockPos.containing(local.x, local.y, local.z);
            if (!level.getBlockState(at).isAir() && other.getPlot().getBoundingBox().contains(at.getX(), at.getY(), at.getZ())) {
                joint = Joint.of(block().kind(), getBlockState().getValue(JointBlock.VARIANT), other, anchor(), f, worldPosition.relative(f), parent);
                joint.setDocked(true);
                setChanged();
                return "docked";
            }
        }
        return "empty";
    }

    /** Puts the body back at the nearest quarter turn about {@code f}'s axis; its blocks and their states turn with it. */
    private String disassemble(ServerLevel level, Direction f) {
        ServerSubLevel s = joint.held(level);
        if (s == null)
            return "not_loaded";
        if (Joint.carriesChild(level, s))
            return "has_child";
        var box = s.getPlot().getBoundingBox();
        List<BlockPos> blocks = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()))
            if (!level.getBlockState(p).isAir())
                blocks.add(p.immutable());
        float sign = f.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
        int k = Math.floorMod(Math.round(sign * joint.turned(s, Joint.parentOf(level, worldPosition), f) / 90f), 4);
        // A finespace cell's voxels are not turned by a block move, only its position: a body with cells goes back unturned.
        if (k != 0)
            for (BlockPos p : blocks)
                if (level.getBlockState(p).getBlock() instanceof io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlock)
                    return "misaligned";
        var transform = new TurnedTransform(joint.homeFrom(), joint.homeTo(), worldPosition, f.getAxis(), k, level);
        for (BlockPos p : blocks)
            if (!level.getBlockState(transform.apply(p)).canBeReplaced())
                return "blocked";
        joint.letGo();
        BlockMoves.enter();
        try {
            if (!blocks.isEmpty())
                SubLevelAssemblyHelper.moveBlocks(level, transform, blocks);
            ServerSubLevelContainer.getContainer(level).removeSubLevel(s, SubLevelRemovalReason.REMOVED);
        } finally {
            BlockMoves.leave();
        }
        joint = null;
        setChanged();
        return "disassembled";
    }

    void tickServer(ServerLevel level) {
        if (joint == null)
            return;
        if (joint.gone(level)) {
            joint.letGo();
            joint = null;
            setChanged();
            return;
        }
        lastFacing = facing();
        if (releaseAsked) {
            releaseAsked = false;
            release(level);
            return;
        }
        joint.tick(level, worldPosition, anchor(), lastFacing, pistonLevel);
    }

    /**
     * Staging: a locked joint lets go on a controller's word. A spring in the joint parts the two bodies, an equal and opposite push along the
     * joint's facing, a speed change of {@link MachineTuning#STAGE_KICK_SPEED} on the held body, then the constraint is removed and the joint
     * is empty again: the held body is a free body of its own, a stage that has gone.
     */
    private void release(ServerLevel level) {
        ServerSubLevel held = joint.held(level);
        ServerSubLevel parent = Joint.parentOf(level, worldPosition);
        if (held != null) {
            Direction face = facing();
            Vector3d dir = new Vector3d(face.getStepX(), face.getStepY(), face.getStepZ());
            if (parent != null)
                parent.logicalPose().transformNormal(dir);
            Vector3d impulse = dir.mul(held.getMassTracker().getMass() * MachineTuning.STAGE_KICK_SPEED);
            io.github.soundgoodizerfan.feedback.vehicle.BodyPush.impulseAt(held, new Vector3d(held.logicalPose().position()), impulse);
            if (parent != null)
                io.github.soundgoodizerfan.feedback.vehicle.BodyPush.impulseAt(parent, new Vector3d(parent.logicalPose().position()), new Vector3d(impulse).negate());
        }
        joint.letGo();
        joint = null;
        setChanged();
    }

    /** Broken or unloaded: the joint goes (a body broken off goes back into the world, or falls free when it cannot). */
    @Override
    public void setRemoved() {
        if (joint != null) {
            boolean broken = !BlockMoves.active() && level instanceof ServerLevel && lastFacing != null && level.isLoaded(worldPosition)
                    && !(level.getBlockState(worldPosition).getBlock() instanceof JointBlock);
            if (joint.docked() || !broken || !"disassembled".equals(disassemble((ServerLevel) level, lastFacing)))
                joint.letGo();
        }
        super.setRemoved();
    }

    // --- Analog: a piston's level -----------------------------------------------------------

    @Override
    public void setLevel(int channel, float level) {
        if (channel == 0 && block().kind() == Joint.Kind.PISTON)
            pistonLevel = Math.clamp(level, -1f, 1f);
        else if (channel == 0 && block().kind() == Joint.Kind.LOCKED && level >= MachineTuning.STAGE_RELEASE_LEVEL)
            releaseAsked = true;
    }

    @Override
    public String[] channelKeys() {
        return switch (block().kind()) {
            case PISTON -> new String[]{"feedback.channel.piston_level"};
            case LOCKED -> new String[]{"feedback.channel.release"};
            default -> new String[0];
        };
    }

    // --- DataNode ---------------------------------------------------------------------------

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

    @Nullable
    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return getLevel();
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }

    @Override
    public boolean isNodeValid() {
        return !isRemoved() && (block().kind() == Joint.Kind.PISTON || block().kind() == Joint.Kind.LOCKED);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (joint != null)
            tag.put("Joint", joint.save());
        tag.putFloat("Level", pistonLevel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        joint = tag.contains("Joint") ? Joint.load(tag.getCompound("Joint")) : null;
        pistonLevel = tag.getFloat("Level");
    }
}
