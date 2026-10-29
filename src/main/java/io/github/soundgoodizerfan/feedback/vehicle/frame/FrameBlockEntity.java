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

import io.github.soundgoodizerfan.feedback.control.Analog;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Units;
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
 * ({@code docs/feedback_vehicles_spec.md} section 4, <em>cells, again</em>).
 * <p>
 * The frame is also an {@link Analog} part a controller sets (spec section 5, <em>Controls</em>):
 * channel 0 is the steering level, -1 to 1, which turns every steered hinge to that share of
 * {@link FTuning#STEER_MAX_ANGLE}; channel 1 the brake, 0 to 1, read by the {@link Drivetrain}
 * at every hub with a brake.
 */
public class FrameBlockEntity extends BlockEntity implements BlockEntitySubLevelActor, DataNode, Analog, io.github.soundgoodizerfan.feedback.vehicle.VehiclePart {

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

    /** Steering level, -1 to 1, and brake, 0 to 1: the controller's (channels 0 and 1). */
    private float steer, brake;
    /** Required by {@link DataNode}; a card targets a frame, nothing links to one. Not saved. */
    private final Set<DataNodeRef> links = new HashSet<>();

    /**
     * A steered hinge's joint and the knuckle's inertia about it, for the servo's gains. The
     * joint's first body is the chassis side, so one steering level turns every kingpin the same
     * way whatever order the frame was cut in.
     */
    private record Steered(PhysicsConstraintHandle handle, double inertia) {
    }

    private final List<Steered> steered = new ArrayList<>();

    /** A slider's joint, with the bodies it joins and the band its spring was set to. */
    private record Sprung(dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle handle, Mount a, Mount b, int band,
                          int vertex, Vector3d va, Vector3d vb, Vector3d axis, double travel, int[] strain) {
    }

    private final List<Sprung> sprung = new ArrayList<>();

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

    /** How many cells the last cut or recut left out because a block stood there. Not saved. */
    private int blockedCells;

    void setBlockedCells(int n) {
        blockedCells = n;
    }

    public int blockedCells() {
        return blockedCells;
    }

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
        new java.util.TreeMap<>(frame.pivots()).forEach((v, p) -> sig.append(v).append(':').append(p.kind()).append(p.drives()).append(p.steers()).append(p.differential()).append(p.band()).append(p.axis()).append(';'));
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
                else if (m.body() != first.body()) {
                    // The chassis side first, so a steering angle means the same at every kingpin.
                    boolean swap = m.body() == own;
                    pivotHandles.add(join(pipeline, swap ? m : first, swap ? first : m, e.getKey(), pivot));
                }
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
            // One linear axis free, along the pivot's: the frame's x is turned onto it, everything else held.
            case SLIDER -> pipeline.addConstraint(a.body(), b.body(), new GenericConstraintConfiguration(va, vb,
                    twist, new Quaterniond(twist), java.util.EnumSet.of(
                    dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis.LINEAR_Y,
                    dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis.LINEAR_Z,
                    dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis.ANGULAR_X,
                    dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis.ANGULAR_Y,
                    dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis.ANGULAR_Z)));
        };
        // The two sides collide: their cells are cleared round the bearing (FrameAssembly), so an
        // arm swung into the chassis stops rather than passing through (playtest 2026-10-16).
        h.setContactsEnabled(true);
        if (pivot.kind() == FrameJoint.Kind.SLIDER && h instanceof dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle g) {
            double travel = springTravel(vertex);
            g.setLimit(dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis.LINEAR_X, -travel, travel);
            sprung.add(new Sprung(g, a, b, pivot.band(), vertex, va, vb, n, travel, new int[1]));
        }
        if (pivot.steers() && pivot.kind() == FrameJoint.Kind.HINGE)
            steered.add(new Steered(h, inertiaAbout(b, vb, n)));
        return h;
    }

    /**
     * {@code mount}'s body's moment of inertia about the line through {@code at} along {@code n},
     * both in its plot: about its centre of mass, plus mass times the distance squared.
     */
    private static double inertiaAbout(Mount mount, Vector3d at, Vector3d n) {
        var mass = mount.body().getMassTracker();
        if (mass.getCenterOfMass() == null)
            return 0;
        Vector3d r = new Vector3d(mass.getCenterOfMass()).sub(at);
        double d2 = r.lengthSquared() - r.dot(n) * r.dot(n);
        return n.dot(mass.getInertiaTensor().transform(new Vector3d(n))) + mass.getMass() * d2;
    }

    /**
     * Turns every kingpin to the steering level, through a servo whose force limit is the
     * driver's arm. The gains are per unit of the knuckle's inertia, so the arm is the only limit
     * that shows (Create Simulated's swivel bearing, MIT: gains scaled by the turning body's
     * inertia; its config-set stiffness was not taken -- ours is the arm). Power steering adds the
     * assist the drive lends ({@link Drivetrain#assistSu}) to the arm's force limit.
     */
    private void steer() {
        double rate = FTuning.STEER_SERVO_RATE,
                arm = (FTuning.STEER_ARM_SU.value() + drivetrain.assistSu()) / Units.SU_PER_KPG_TORQUE;
        for (Steered s : steered)
            if (s.handle().isValid())
                s.handle().setMotor(dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintHandle.DEFAULT_AXIS,
                        steer * FTuning.STEER_MAX_ANGLE, rate * rate * s.inertia(),
                        2 * FTuning.STEER_SERVO_DAMPING_RATIO * rate * s.inertia(), true, arm);
    }

    /**
     * How far a slider at {@code vertex} runs either side of rest: the spring's range is set by its
     * material (spec section 5), the yield strength of the strongest tube meeting the point, as a share of
     * {@link FTuning#SPRING_REFERENCE_YIELD} on {@link FTuning#SPRING_TRAVEL}. No tube, or a material with no
     * yield figure, gets the stock travel.
     */
    private double springTravel(int vertex) {
        float yield = 0;
        for (Map.Entry<Long, Frame.Tube> e : frame.tubes().entrySet())
            if (Frame.edgeA(e.getKey()) == vertex || Frame.edgeB(e.getKey()) == vertex)
                yield = Math.max(yield, io.github.soundgoodizerfan.feedback.process.Material
                        .find(io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials(),
                                e.getValue().materialState().getBlock().asItem())
                        .flatMap(m -> m.material().mechanical().yieldStrength()).map(st -> st.value()).orElse(0f));
        if (yield <= 0)
            return FTuning.SPRING_TRAVEL;
        return FTuning.SPRING_TRAVEL * Math.clamp(yield / FTuning.SPRING_REFERENCE_YIELD, 0.5, 2);
    }

    /**
     * Holds every slider at rest with a spring and a shock. The spring carries the two bodies' reduced
     * mass at the band's frequency, so a heavier car sits no lower on it (a frequency, not a stiffness,
     * as the tire's is); the shock is a fixed share of critical. Refreshed every tick because the
     * masses settle after the joint is made. Sable's own PD motor, not a hand-summed force: the force
     * is not ours to read, which the spec's *Springs push by our own hand* wanted, and that waits until
     * the motor has been played and found wanting.
     */
    private void spring() {
        for (Sprung s : sprung) {
            if (!s.handle().isValid())
                continue;
            double ma = s.a().body().getMassTracker().getMass(), mb = s.b().body().getMassTracker().getMass();
            if (ma <= 0 || mb <= 0)
                continue;
            double reduced = ma * mb / (ma + mb), omega = 2 * Math.PI * FTuning.SUSPENSION_HZ[s.band()];
            s.handle().setMotor(dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis.LINEAR_X, 0,
                    reduced * omega * omega, 2 * FTuning.TIRE_DAMPING_RATIO * omega * reduced, false, 0);
            // Held against a stop it is overextended; long enough and it snaps, and the point is a
            // loose ball until the player sets it right again.
            Vector3d pa = s.a().body().logicalPose().transformPosition(new Vector3d(s.va()));
            Vector3d pb = s.b().body().logicalPose().transformPosition(new Vector3d(s.vb()));
            double extension = pb.sub(pa).dot(s.a().body().logicalPose().transformNormal(new Vector3d(s.axis())));
            s.strain()[0] = Math.abs(extension) >= s.travel() * 0.97 ? s.strain()[0] + 1 : Math.max(0, s.strain()[0] - 2);
            if (s.strain()[0] >= FTuning.SPRING_SNAP_TICKS) {
                Frame.Pivot was = frame.pivots().get(s.vertex());
                if (was != null)
                    frame.setPivot(s.vertex(), new Frame.Pivot(FrameJoint.Kind.BALL, was.axis(), false, false, Frame.Pivot.NO_DIFFERENTIAL, was.band()));
                level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 0.6f);
                changed();
                return;
            }
        }
    }

    /** The brake level, 0 to 1, for the drivetrain. */
    public float brake() {
        return brake;
    }

    /** The steering level, -1 to 1. */
    public float steering() {
        return steer;
    }

    // --- Analog -------------------------------------------------------------------------------

    @Override
    public void setLevel(int channel, float level) {
        switch (channel) {
            case 0 -> steer = Math.clamp(level, -1, 1);
            case 1 -> brake = Math.clamp(level, 0, 1);
            default -> {
                return;
            }
        }
        setChanged();
    }

    @Override
    public String[] channelKeys() {
        return new String[]{"feedback.analog.steer", "feedback.analog.brake"};
    }

    // --- DataNode -----------------------------------------------------------------------------

    @Override
    public boolean acceptsLinks() {
        return false;
    }

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
    public net.minecraft.world.level.Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }

    @Override
    public Set<DataNodeRef> getNodeLinks() {
        return links;
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
        steered.clear();
        sprung.clear();
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

    /**
     * Every loaded frame joined to this one by a frame-to-frame joint, whichever of the two stores
     * it. Server thread only.
     */
    public List<FrameBlockEntity> partners() {
        List<FrameBlockEntity> out = new ArrayList<>();
        for (FrameJoint j : joints) {
            FrameBlockEntity far = LOADED.get(j.other());
            if (far != null && far.level == level && !out.contains(far))
                out.add(far);
        }
        for (FrameBlockEntity far : LOADED.values())
            if (far != this && far.level == level && !out.contains(far)
                    && far.joints.stream().anyMatch(j -> j.other().equals(id)))
                out.add(far);
        return out;
    }

    /** The loaded frame on {@code level} nearest {@code pos}: a vehicle's own, wherever it stands. Server thread only. */
    @Nullable
    public static FrameBlockEntity nearest(net.minecraft.world.level.Level level, BlockPos pos) {
        FrameBlockEntity best = null;
        for (FrameBlockEntity f : loadedOn(level))
            if (best == null || f.worldPosition.distSqr(pos) < best.worldPosition.distSqr(pos))
                best = f;
        return best;
    }

    /**
     * {@code ref} as an offset from the frame of the vehicle it sits in, if it is a vehicle part
     * with a frame within {@link FTuning#CARD_FRAME_REACH} blocks; else unchanged.
     */
    public static DataNodeRef framed(net.minecraft.world.level.Level level, DataNodeRef ref) {
        if (ref.framed() || !(level.getBlockEntity(ref.pos()) instanceof io.github.soundgoodizerfan.feedback.vehicle.VehiclePart))
            return ref;
        FrameBlockEntity frame = nearest(level, ref.pos());
        if (frame == null || frame.worldPosition.distSqr(ref.pos()) > (double) FTuning.CARD_FRAME_REACH * FTuning.CARD_FRAME_REACH)
            return ref;
        return new DataNodeRef(ref.pos().subtract(frame.worldPosition), ref.side(), frame.worldPosition);
    }

    /**
     * A card's target as it stands now: a framed one is found from the frame nearest {@code from},
     * the controller running the card, which rides in the same vehicle. Null if there is no frame.
     */
    @Nullable
    public static DataNodeRef located(net.minecraft.world.level.Level level, DataNodeRef ref, BlockPos from) {
        if (!ref.framed())
            return ref;
        FrameBlockEntity frame = nearest(level, from);
        return frame == null ? null : new DataNodeRef(frame.worldPosition.offset(ref.pos()), ref.side());
    }

    /** Every loaded frame on {@code level}. Server thread only. */
    public static List<FrameBlockEntity> loadedOn(net.minecraft.world.level.Level level) {
        return LOADED.values().stream().filter(f -> f.level == level && !f.isRemoved()).toList();
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
        if (level != null)
            drivetrain.release(level, worldPosition);
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
    private Map<Integer, Float> sentHubs = Map.of();
    private Map<Long, Float> sentTubes = Map.of();
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
        Map<Long, Float> tubes = drivetrain.tubeSpin();
        Map<Integer, Float> hubs = drivetrain.hubSpin();
        boolean still = tubes.values().stream().allMatch(w -> Math.abs(w) < 1e-3) && hubs.values().stream().allMatch(w -> Math.abs(w) < 1e-3);
        boolean wasStill = sentHubs.values().stream().allMatch(w -> Math.abs(w) < 1e-3) && sentTubes.values().stream().allMatch(w -> Math.abs(w) < 1e-3);
        // Still and unchanged sends nothing; coupling or uncoupling is news even standing still.
        if (still && wasStill && tubes.keySet().equals(sentTubes.keySet()))
            return;
        sentHubs = hubs;
        sentTubes = tubes;
        Vec3 at = Vec3.atCenterOf(worldPosition);
        var body = SableCompanion.INSTANCE.getContaining(this);
        if (body != null)
            at = body.logicalPose().transformPosition(at);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(server, null, at.x, at.y, at.z, 96,
                new io.github.soundgoodizerfan.feedback.net.FrameSpinPayload(worldPosition, tubes, drivetrain.tubeSu(), hubs));
    }

    /**
     * Takes every cell of this frame out of the world, and every swinging part's body with it: a
     * broken frame leaves nothing behind (author, 2026-10-16). Only a base cell that names this
     * frame is cleared, so a cell of another frame standing in the way is safe.
     */
    public void breakCells(ServerLevel level) {
        for (BlockPos rel : baseCells) {
            BlockPos at = worldPosition.offset(rel);
            if (level.getBlockEntity(at) instanceof ChassisCellBlockEntity cell && (cell.frame() == null || cell.frame().equals(id)))
                level.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        baseCells.clear();
        for (GroupBody g : groupBodies.values())
            FrameAssembly.destroy(level, g);
        groupBodies.clear();
        dropPivots();
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
            be.steer();
            be.spring();
        }
        // Unlifted too: a drive line on the bench couples, turns and reads like one on the road --
        // only the tires need a body under them (physicsTick).
        if (!level.isClientSide) {
            if (be.drivetrainStale)
                be.drivetrain.rebuild(be.frame);
            be.drivetrainStale = false;
            be.drivetrain.tick(level, pos, be.brake, be.steer);
            if (level.getGameTime() % 20 == 0 && be.drivetrain.wearPads(be.frame))
                be.setChanged();
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
            case WELD, LOCKED, SLIDER -> pipeline.addConstraint(a, b, new FixedConstraintConfiguration(pa, pb,
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
        tag.putFloat("Steer", steer);
        tag.putFloat("Brake", brake);
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
        steer = tag.getFloat("Steer");
        brake = tag.getFloat("Brake");
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
