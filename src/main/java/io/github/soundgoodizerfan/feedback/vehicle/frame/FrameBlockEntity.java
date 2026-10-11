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
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Holds one welded {@link Frame}, anchored at this block, and -- once assembled -- the chassis
 * cells cut from it, so an edit can take the old cells away before cutting new ones.
 */
public class FrameBlockEntity extends BlockEntity {

    private final Frame frame = new Frame();
    /** Cells placed for this frame, relative to it. Empty until assembled. */
    private final Set<BlockPos> cells = new HashSet<>();
    /** Survives assembly, which moves this block entity into a body's plot and changes its pos. */
    private UUID id = UUID.randomUUID();
    private final List<FrameJoint> joints = new ArrayList<>();
    /** Live Sable constraints for {@link #joints}; rebuilt after every load, so never saved. */
    private final Map<FrameJoint, PhysicsConstraintHandle> handles = new HashMap<>();

    /** Frames loaded on the server, by id, so a joint can find its other end. Server thread only. */
    private static final Map<UUID, FrameBlockEntity> LOADED = new HashMap<>();

    public FrameBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.FRAME_ENTITY.get(), pos, state);
    }

    public Frame frame() {
        return frame;
    }

    /** Outline and collision: the frame's in-block voxels. Rebuilt lazily after any change. */
    @Nullable
    private net.minecraft.world.phys.shapes.VoxelShape shape;

    public net.minecraft.world.phys.shapes.VoxelShape shape() {
        if (shape == null)
            shape = FrameShape.of(frame);
        return shape;
    }

    public Set<BlockPos> cells() {
        return cells;
    }

    public UUID id() {
        return id;
    }

    public List<FrameJoint> joints() {
        return joints;
    }

    /** How many of {@link #joints()} are live Sable constraints right now. */
    public int liveJoints() {
        return (int) handles.values().stream().filter(PhysicsConstraintHandle::isValid).count();
    }

    @Nullable
    public static FrameBlockEntity loaded(UUID id) {
        return LOADED.get(id);
    }

    /** Joins vertex {@code mine} here to vertex {@code theirs} on {@code other}, replacing any joint between them. */
    public void join(int mine, FrameBlockEntity other, int theirs, FrameJoint.Kind kind, Vec3 axis) {
        unjoin(mine, other, theirs);
        joints.add(new FrameJoint(other.id, mine, theirs, kind, axis, false));
        changed();
    }

    /** The joint between these vertices, stored on either frame; null if none. */
    @Nullable
    public FrameJoint joint(int mine, FrameBlockEntity other, int theirs) {
        for (FrameJoint j : joints)
            if (j.other().equals(other.id) && j.mine() == mine && j.theirs() == theirs)
                return j;
        for (FrameJoint j : other.joints)
            if (j.other().equals(id) && j.mine() == theirs && j.theirs() == mine)
                return j;
        return null;
    }

    /**
     * Swaps joint {@code was} for {@code now} on whichever frame holds it, dropping its live
     * constraint so the next tick makes the new one.
     */
    public void replace(FrameBlockEntity other, FrameJoint was, FrameJoint now) {
        FrameBlockEntity holder = joints.contains(was) ? this : other;
        holder.drop(j -> j.equals(was));
        holder.joints.add(now);
        holder.changed();
    }

    /** Removes the joint between these vertices, stored on either frame. True if there was one. */
    public boolean unjoin(int mine, FrameBlockEntity other, int theirs) {
        boolean here = drop(j -> j.other().equals(other.id) && j.mine() == mine && j.theirs() == theirs);
        boolean there = other.drop(j -> j.other().equals(id) && j.mine() == theirs && j.theirs() == mine);
        if (there)
            other.changed();
        return here || there;
    }

    private boolean drop(java.util.function.Predicate<FrameJoint> which) {
        boolean any = false;
        for (var it = joints.iterator(); it.hasNext(); ) {
            FrameJoint j = it.next();
            if (which.test(j)) {
                it.remove();
                PhysicsConstraintHandle h = handles.remove(j);
                if (h != null && h.isValid())
                    h.remove();
                any = true;
            }
        }
        return any;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel)
            LOADED.put(id, this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LOADED.remove(id, this);
        handles.values().forEach(h -> {
            if (h.isValid())
                h.remove();
        });
        handles.clear();
    }

    /**
     * Makes each joint a Sable constraint once both its frames are bodies, and drops joints whose
     * vertex was cut away. Runs every tick because a body can come and go (assembly, unloading)
     * without telling this frame; an existing live handle costs one check.
     */
    static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, FrameBlockEntity be) {
        LOADED.putIfAbsent(be.id, be); // a frame moved into a body's plot is not always given onLoad
        if (be.joints.isEmpty())
            return;
        if (be.drop(j -> !be.frame.vertices().containsKey(j.mine()))) {
            be.changed();
            return;
        }
        for (FrameJoint j : be.joints) {
            PhysicsConstraintHandle h = be.handles.get(j);
            if (h != null && h.isValid())
                continue;
            FrameBlockEntity other = LOADED.get(j.other());
            if (other == null || other.level != level || !other.frame.vertices().containsKey(j.theirs()))
                continue;
            if (!(SableCompanion.INSTANCE.getContaining(be) instanceof ServerSubLevel a)
                    || !(SableCompanion.INSTANCE.getContaining(other) instanceof ServerSubLevel b) || a == b)
                continue;
            PhysicsConstraintHandle made = constrain(a, be, j.mine(), b, other, j.theirs(), j);
            made.setContactsEnabled(false); // joined parts touch at the joint by design
            be.handles.put(j, made);
        }
    }

    /**
     * The Sable constraint for {@code j}. Positions are in each body's plot, which is where these
     * block entities live once assembled. A weld pulls the two vertices together and keeps the
     * bodies turned as they stand now. It is Sable's {@code Fixed} joint, not a {@code Generic} one
     * with every axis locked: the Generic one, tried first, held the bodies at whatever offset
     * they started with and never closed the gap. A ball joint is the Generic one all the same,
     * linear axes only -- Sable's {@code Free} joint, the obvious name for it, ignores its anchor
     * and holds nothing -- so a ball joint holds its vertices where they are when it is made
     * rather than pulling them together.
     */
    private static PhysicsConstraintHandle constrain(ServerSubLevel a, FrameBlockEntity fa, int va,
                                                     ServerSubLevel b, FrameBlockEntity fb, int vb, FrameJoint j) {
        Vector3d pa = plot(fa, va), pb = plot(fb, vb);
        Quaterniond qa = new Quaterniond(a.logicalPose().orientation()), qb = new Quaterniond(b.logicalPose().orientation());
        var pipeline = dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem.get(fa.level).getPipeline();
        return switch (j.kind()) {
            case WELD, LOCKED -> pipeline.addConstraint(a, b, new FixedConstraintConfiguration(pa, pb,
                    new Quaterniond(qa).conjugate().mul(qb)));
            case BALL -> pipeline.addConstraint(a, b, new GenericConstraintConfiguration(pa, pb,
                    new Quaterniond(qa).conjugate(), new Quaterniond(qb).conjugate(),
                    java.util.Set.of(dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis.LINEAR)));
            case HINGE -> {
                Vector3d na = new Vector3d(j.axis().x, j.axis().y, j.axis().z);
                Vector3d world = qa.transform(new Vector3d(na));
                Vector3d nb = new Quaterniond(qb).conjugate().transform(world);
                yield pipeline.addConstraint(a, b, new RotaryConstraintConfiguration(pa, pb, na, nb));
            }
        };
    }

    /** How the body holding {@code be} is turned; none for a frame still standing in the world. */
    static Quaterniond rotation(FrameBlockEntity be) {
        var body = SableCompanion.INSTANCE.getContaining(be);
        return body == null ? new Quaterniond() : new Quaterniond(body.logicalPose().orientation());
    }

    private static Vector3d plot(FrameBlockEntity be, int vertex) {
        Vec3 p = be.frame.pos(vertex).add(Vec3.atLowerCornerOf(be.worldPosition));
        return new Vector3d(p.x, p.y, p.z);
    }

    /**
     * Call after editing {@link #frame()}: saves and pushes it to clients, and if the frame is
     * already a vehicle, re-cuts its collision cells so the body matches at once.
     */
    public void changed() {
        shape = null;
        if (level instanceof ServerLevel server && !cells.isEmpty())
            FrameAssembly.recut(server, worldPosition, this);
        setChanged();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Frame", frame.save());
        tag.putLongArray("Cells", cells.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putUUID("Id", id);
        ListTag list = new ListTag();
        joints.forEach(j -> list.add(j.save()));
        tag.put("Joints", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        frame.load(tag.getCompound("Frame"));
        shape = null;
        cells.clear();
        for (long c : tag.getLongArray("Cells"))
            cells.add(BlockPos.of(c));
        if (tag.hasUUID("Id"))
            id = tag.getUUID("Id");
        joints.clear();
        for (Tag t : tag.getList("Joints", Tag.TAG_COMPOUND))
            joints.add(FrameJoint.load((net.minecraft.nbt.CompoundTag) t));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
