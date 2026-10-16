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
import io.github.soundgoodizerfan.feedback.vehicle.drive.Drivetrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
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
 * Holds one welded {@link Frame}, measured from this block, and -- once assembled -- what became of
 * it: the {@link ChassisCellBlock cells} in this block's body, the groups lifted into bodies of
 * their own ({@link GroupBody}), and the Sable joints that hang those groups at their bearings
 * ({@code feedback_vehicles_spec.md} section 4, <em>cells, again</em>).
 */
public class FrameBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {

    private final Frame frame = new Frame();
    /** True once the frame has been cut into cells and lifted; see {@link FrameAssembly#cut}. */
    private boolean assembled;
    /** This frame's cells in this block's own body, by offset from this block. */
    private final Set<BlockPos> baseCells = new HashSet<>();

    /**
     * A group of the frame lifted into a body of its own: the body, where this block's position
     * falls in that body's plot (so frame-local point {@code p} is at {@code origin + p} there),
     * and its cells by offset from {@code origin}.
     */
    public record GroupBody(UUID body, BlockPos origin, Set<BlockPos> cells) {
    }

    /** By {@link FrameAssembly.Part#id}. A group not named here is rigid with this block's body. */
    private final Map<Integer, GroupBody> groupBodies = new HashMap<>();
    /** The frame changed since its cells were last made to match it. */
    private boolean cellsStale;
    /** The bearings, as live Sable constraints between bodies; rebuilt when they change, never saved. */
    private final List<PhysicsConstraintHandle> pivotHandles = new ArrayList<>();
    /** Welds on bearings that hold a group of its own to this block's body; likewise. */
    private final List<PhysicsConstraintHandle> anchorHandles = new ArrayList<>();
    private String pivotSignature = "";
    /** The frame cut at its bearings, as of the last edit; see {@link #groupOf}. */
    @Nullable
    private List<FrameAssembly.Part> cut;

    /** What the frame turns, if it is welded to a shaft. Derived from {@link #frame}, never saved. */
    private final Drivetrain drivetrain = new Drivetrain();
    /** The frame changed since {@link #drivetrain} was last rebuilt from it. */
    private boolean drivetrainStale = true;
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

    public boolean assembled() {
        return assembled;
    }

    public void setAssembled(boolean assembled) {
        this.assembled = assembled;
        setChanged();
    }

    // --- cells and group bodies, written by FrameAssembly ------------------------------------

    void clearCells() {
        baseCells.clear();
        groupBodies.clear();
        dropPivots();
    }

    void setBaseCells(Set<BlockPos> cells) {
        baseCells.clear();
        baseCells.addAll(cells);
        setChanged();
    }

    Set<BlockPos> baseCells() {
        return baseCells;
    }

    void addGroupBody(int group, UUID body, BlockPos origin, Set<BlockPos> cells) {
        groupBodies.put(group, new GroupBody(body, origin, new HashSet<>(cells)));
        setChanged();
    }

    void removeGroupBody(int group) {
        groupBodies.remove(group);
        setChanged();
    }

    public Map<Integer, GroupBody> groupBodies() {
        return groupBodies;
    }

    /** How many cells the frame has in the world right now, every body counted, for tests. */
    public int cellCount() {
        return baseCells.size() + groupBodies.values().stream().mapToInt(g -> g.cells().size()).sum();
    }

    /**
     * Turns an assembled frame back into a frame: takes every group body out of the world and
     * forgets the cells. The cells in this block's own body are the caller's to drop -- the Lift
     * lowering a vehicle moves the base's other blocks out and destroys the rest.
     */
    public void disassemble(ServerLevel level) {
        groupBodies.values().forEach(g -> FrameAssembly.destroy(level, g));
        clearCells();
        setAssembled(false);
        changed();
    }

    /** The group holding {@code vertex} -- the first, for a bearing on several; null if none does. */
    @Nullable
    public Integer groupOf(int vertex) {
        for (FrameAssembly.Part p : cut())
            if (p.vertices().contains(vertex))
                return p.id();
        return null;
    }

    /** The groups as of the last edit; also what the renderer draws each group by. */
    public List<FrameAssembly.Part> cut() {
        if (cut == null)
            cut = FrameAssembly.groups(frame);
        return cut;
    }

    /**
     * A body and where this frame's origin is in its plot: frame-local point {@code p} is at
     * {@code origin + p} in {@code body}'s plot.
     */
    public record Mount(ServerSubLevel body, BlockPos origin) {
    }

    /**
     * The body that carries {@code vertex}: its group's own body if it has one, else the one this
     * block is in. Null for a frame not in a body.
     */
    @Nullable
    public Mount mount(ServerLevel level, int vertex) {
        Integer group = groupOf(vertex);
        return group == null ? own() : mountOfGroup(level, group);
    }

    @Nullable
    private Mount own() {
        return SableCompanion.INSTANCE.getContaining(this) instanceof ServerSubLevel s ? new Mount(s, worldPosition) : null;
    }

    @Nullable
    private Mount mountOfGroup(ServerLevel level, int group) {
        GroupBody g = groupBodies.get(group);
        if (g != null && ServerSubLevelContainer.getContainer(level).getSubLevel(g.body()) instanceof ServerSubLevel s && !s.isRemoved())
            return new Mount(s, g.origin());
        return own();
    }

    /** Where frame point {@code vertex} is in the world, through the body carrying it; for tests. */
    public Vec3 worldPos(ServerLevel level, int vertex) {
        Mount m = mount(level, vertex);
        Vec3 local = frame.pos(vertex);
        if (m == null)
            return local.add(Vec3.atLowerCornerOf(worldPosition));
        return m.body().logicalPose().transformPosition(local.add(Vec3.atLowerCornerOf(m.origin())));
    }

    /**
     * Makes the frame's bearings Sable joints between the bodies of the groups they separate, and
     * each weld on a bearing a joint between this block's body and its group's: the pivot's kind
     * at the pivot's point, the point in each body's own plot. Joints are made at rest -- each
     * plot is the frame as built, so a pivot is at the same frame point in both and a hinge's axis
     * the same plot axis -- and remade only when the bearings or bodies change, or Sable drops one.
     * Two groups in one body (both welded) need nothing.
     */
    private void pivots(ServerLevel level, ServerSubLevel own) {
        StringBuilder sig = new StringBuilder();
        new java.util.TreeMap<>(frame.pivots()).forEach((v, p) -> sig.append(v).append(':').append(p.kind()).append(p.drives()).append(p.axis()).append(';'));
        new java.util.TreeMap<>(frame.anchors()).forEach((v, off) -> sig.append(v).append('@').append(off.toShortString())
                .append(level.getBlockState(worldPosition.offset(off)).isAir() ? '-' : '+').append(';'));
        new java.util.TreeMap<>(groupBodies).forEach((g, b) -> sig.append('g').append(g).append(b.body()).append(';'));
        boolean live = pivotHandles.stream().allMatch(PhysicsConstraintHandle::isValid)
                && anchorHandles.stream().allMatch(PhysicsConstraintHandle::isValid);
        if (live && sig.toString().equals(pivotSignature))
            return;
        dropPivots();
        var pipeline = dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem.get(level).getPipeline();
        Mount base = new Mount(own, worldPosition);
        for (Map.Entry<Integer, Frame.Pivot> e : frame.pivots().entrySet()) {
            Frame.Pivot pivot = e.getValue();
            Mount first = null;
            for (FrameAssembly.Part part : cut()) {
                if (!part.vertices().contains(e.getKey()))
                    continue;
                Mount m = mountOfGroup(level, part.id());
                if (m == null)
                    return;
                if (first == null)
                    first = m;
                else if (m.body() != first.body())
                    pivotHandles.add(join(pipeline, first, m, e.getKey(), pivot));
            }
            BlockPos off = frame.anchors().get(e.getKey());
            if (off == null || level.getBlockState(worldPosition.offset(off)).isAir())
                continue;
            for (FrameAssembly.Part part : cut())
                if (part.vertices().contains(e.getKey())) {
                    Mount m = mountOfGroup(level, part.id());
                    if (m != null && m.body() != own)
                        anchorHandles.add(join(pipeline, base, m, e.getKey(), pivot));
                }
        }
        pivotSignature = sig.toString();
    }

    private PhysicsConstraintHandle join(dev.ryanhcode.sable.api.physics.PhysicsPipeline pipeline, Mount a, Mount b,
                                         int vertex, Frame.Pivot pivot) {
        Vec3 axis = pivot.axis();
        Vec3 local = frame.pos(vertex);
        Vec3 pa = local.add(Vec3.atLowerCornerOf(a.origin())), pb = local.add(Vec3.atLowerCornerOf(b.origin()));
        Vector3d va = new Vector3d(pa.x, pa.y, pa.z), vb = new Vector3d(pb.x, pb.y, pb.z);
        Vector3d n = new Vector3d(axis.x, axis.y, axis.z).normalize();
        // A ball that carries rotation is a universal joint: it swings, but does not twist about
        // the shaft -- the spin is the drive's, computed, so the bodies must not take it up.
        Quaterniond twist = new Quaterniond().rotationTo(1, 0, 0, n.x, n.y, n.z);
        var locked = new java.util.HashSet<>(java.util.Set.of(dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis.LINEAR));
        if (pivot.drives())
            locked.add(dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis.ANGULAR_X);
        PhysicsConstraintHandle h = switch (pivot.kind()) {
            case WELD, LOCKED -> pipeline.addConstraint(a.body(), b.body(), new FixedConstraintConfiguration(va, vb, new Quaterniond()));
            case BALL -> pipeline.addConstraint(a.body(), b.body(), new GenericConstraintConfiguration(va, vb,
                    twist, new Quaterniond(twist), locked));
            case HINGE -> pipeline.addConstraint(a.body(), b.body(), new RotaryConstraintConfiguration(va, vb, n, new Vector3d(n)));
        };
        h.setContactsEnabled(false); // the two sides touch at the bearing by design
        return h;
    }

    private void dropPivots() {
        pivotHandles.forEach(h -> {
            if (h.isValid())
                h.remove();
        });
        anchorHandles.forEach(h -> {
            if (h.isValid())
                h.remove();
        });
        pivotHandles.clear();
        anchorHandles.clear();
        pivotSignature = "";
    }

    public UUID id() {
        return id;
    }

    public List<FrameJoint> joints() {
        return joints;
    }

    /**
     * How many joints are live Sable constraints right now: those to other frames, and the
     * bearings inside this one ({@link Frame#pivots()}).
     */
    public int liveJoints() {
        return (int) (handles.values().stream().filter(PhysicsConstraintHandle::isValid).count()
                + pivotHandles.stream().filter(PhysicsConstraintHandle::isValid).count());
    }

    /** How many bearing-welds hold a group of its own to this block's body right now, for tests. */
    public int liveAnchors() {
        return (int) anchorHandles.stream().filter(PhysicsConstraintHandle::isValid).count();
    }

    /**
     * The frame a chassis cell at {@code pos} belongs to, if one is loaded on this level: cells are
     * hidden blocks that know nothing, so the frame is asked. For the debug helmet's card.
     */
    @Nullable
    public static FrameBlockEntity owning(net.minecraft.world.level.Level level, BlockPos pos) {
        for (FrameBlockEntity be : LOADED.values()) {
            if (be.level != level || be.isRemoved())
                continue;
            if (be.baseCells.contains(pos.subtract(be.worldPosition)))
                return be;
            for (GroupBody g : be.groupBodies.values())
                if (g.cells().contains(pos.subtract(g.origin())))
                    return be;
        }
        return null;
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

    /** A joint seen from one end: the frame at the far end, and the joint as stored. */
    public record Held(FrameBlockEntity other, FrameJoint joint) {
        /** The far end's vertex. */
        public int theirs(FrameBlockEntity from) {
            return joint.other().equals(from.id) ? joint.mine() : joint.theirs();
        }
    }

    /** Every joint on vertex {@code vertex} of this frame, whichever frame stores it. Server thread only. */
    public List<Held> jointsAt(int vertex) {
        List<Held> out = new ArrayList<>();
        for (FrameJoint j : joints) {
            FrameBlockEntity far = LOADED.get(j.other());
            if (j.mine() == vertex && far != null)
                out.add(new Held(far, j));
        }
        for (FrameBlockEntity far : LOADED.values())
            if (far != this)
                for (FrameJoint j : far.joints)
                    if (j.other().equals(id) && j.theirs() == vertex)
                        out.add(new Held(far, j));
        return out;
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
        dropPivots();
        handles.values().forEach(h -> {
            if (h.isValid())
                h.remove();
        });
        handles.clear();
    }

    /** How often a frame's spin goes to the clients near it, in ticks. */
    private static final int SPIN_SYNC_TICKS = 5;
    /** The spin last sent, so a frame standing still sends nothing. */
    private float sentChain;
    private Map<Integer, Float> sentHubs = Map.of();
    private final FrameSpin spin = new FrameSpin();

    /** The client's picture of how fast this frame's drive and tires turn. */
    public FrameSpin spin() {
        return spin;
    }

    /**
     * Sends the drive chain's and tires' spin to players near the vehicle -- found by the body's
     * world position, since a frame inside a body sits in a plot millions of blocks out.
     */
    private void sendSpin(ServerLevel server) {
        float chain = drivetrain.chainSpin();
        Map<Integer, Float> hubs = drivetrain.hubSpin();
        boolean still = Math.abs(chain) < 1e-3 && hubs.values().stream().allMatch(w -> Math.abs(w) < 1e-3);
        boolean wasStill = Math.abs(sentChain) < 1e-3 && sentHubs.values().stream().allMatch(w -> Math.abs(w) < 1e-3);
        if (still && wasStill)
            return;
        sentChain = chain;
        sentHubs = hubs;
        Vec3 at = Vec3.atCenterOf(worldPosition);
        var body = SableCompanion.INSTANCE.getContaining(this);
        if (body != null)
            at = body.logicalPose().transformPosition(at);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(server, null, at.x, at.y, at.z, 96,
                new io.github.soundgoodizerfan.feedback.net.FrameSpinPayload(worldPosition, chain, drivetrain.chainTubes(), hubs));
    }

    /** The drivetrain, for tests and the debug helmet. */
    public Drivetrain drivetrain() {
        return drivetrain;
    }

    /** Every physics step, on the server: the tires and the air ({@link Drivetrain#physicsTick}). */
    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (assembled && level instanceof ServerLevel server)
            drivetrain.physicsTick(server, this, subLevel, handle, timeStep);
    }

    /**
     * Makes each joint a Sable constraint once both its frames are bodies, and drops joints whose
     * vertex was cut away. Runs every tick because a body can come and go (assembly, unloading)
     * without telling this frame; an existing live handle costs one check.
     */
    static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, FrameBlockEntity be) {
        LOADED.putIfAbsent(be.id, be); // a frame moved into a body's plot is not always given onLoad
        if (be.assembled && level instanceof ServerLevel server
                && SableCompanion.INSTANCE.getContaining(be) instanceof ServerSubLevel body) {
            if (be.cellsStale)
                FrameAssembly.recut(server, be);
            be.cellsStale = false;
            be.pivots(server, body);
        }
        if (be.assembled) {
            if (be.drivetrainStale)
                be.drivetrain.rebuild(be.frame);
            be.drivetrainStale = false;
            be.drivetrain.tick(level, pos);
            if (level.getGameTime() % SPIN_SYNC_TICKS == 0 && level instanceof ServerLevel server)
                be.sendSpin(server);
        }
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
        return constrain(dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem.get(fa.level).getPipeline(),
                a, plot(fa, va), new Quaterniond(a.logicalPose().orientation()),
                b, plot(fb, vb), new Quaterniond(b.logicalPose().orientation()),
                new Quaterniond(a.logicalPose().orientation()), j.kind(), j.axis());
    }

    /**
     * The Sable constraint for one joint between two frames' bodies, anchors in each body's plot.
     * {@code pa}/{@code pb} are the pivot in each
     * one's own space and {@code qa}/{@code qb} how each is turned now; {@code frame} is how the
     * plot is turned in the world, to carry a hinge's axis -- given in plot space -- into each
     * body's own.
     */
    static PhysicsConstraintHandle constrain(dev.ryanhcode.sable.api.physics.PhysicsPipeline pipeline,
                                             dev.ryanhcode.sable.api.physics.PhysicsPipelineBody a, Vector3d pa, Quaterniond qa,
                                             dev.ryanhcode.sable.api.physics.PhysicsPipelineBody b, Vector3d pb, Quaterniond qb,
                                             Quaterniond frame, FrameJoint.Kind kind, Vec3 axis) {
        return switch (kind) {
            case WELD, LOCKED -> pipeline.addConstraint(a, b, new FixedConstraintConfiguration(pa, pb,
                    new Quaterniond(qa).conjugate().mul(qb)));
            case BALL -> pipeline.addConstraint(a, b, new GenericConstraintConfiguration(pa, pb,
                    new Quaterniond(qa).conjugate(), new Quaterniond(qb).conjugate(),
                    java.util.Set.of(dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis.LINEAR)));
            case HINGE -> {
                Vector3d world = frame.transform(new Vector3d(axis.x, axis.y, axis.z));
                Vector3d na = new Quaterniond(qa).conjugate().transform(new Vector3d(world));
                Vector3d nb = new Quaterniond(qb).conjugate().transform(new Vector3d(world));
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
     * already a vehicle, has its cells made to match on the next tick.
     */
    public void changed() {
        shape = null;
        cut = null;
        cellsStale = assembled;
        drivetrainStale = true;
        setChanged();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Frame", frame.save());
        tag.putBoolean("Assembled", assembled);
        tag.putLongArray("BaseCells", baseCells.stream().mapToLong(BlockPos::asLong).toArray());
        ListTag groups = new ListTag();
        groupBodies.forEach((g, b) -> {
            CompoundTag t = new CompoundTag();
            t.putInt("Group", g);
            t.putUUID("Body", b.body());
            t.putLong("Origin", b.origin().asLong());
            t.putLongArray("Cells", b.cells().stream().mapToLong(BlockPos::asLong).toArray());
            groups.add(t);
        });
        tag.put("Groups", groups);
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
        // A save from before boxes had "Cells" and no flag; a frame that had cells was assembled.
        assembled = tag.getBoolean("Assembled") || tag.getLongArray("Cells").length > 0;
        baseCells.clear();
        for (long l : tag.getLongArray("BaseCells"))
            baseCells.add(BlockPos.of(l));
        groupBodies.clear();
        for (Tag t : tag.getList("Groups", Tag.TAG_COMPOUND)) {
            CompoundTag g = (CompoundTag) t;
            Set<BlockPos> cells = new HashSet<>();
            for (long l : g.getLongArray("Cells"))
                cells.add(BlockPos.of(l));
            groupBodies.put(g.getInt("Group"), new GroupBody(g.getUUID("Body"), BlockPos.of(g.getLong("Origin")), cells));
        }
        // An assembled frame saved before cells (its plates were boxes) gets its cells, all rigid
        // with the body it is in, on its first tick.
        cellsStale = assembled && !tag.contains("BaseCells");
        cut = null;
        drivetrainStale = true;
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
