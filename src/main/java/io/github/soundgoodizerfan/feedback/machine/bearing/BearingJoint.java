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
package io.github.soundgoodizerfan.feedback.machine.bearing;

import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * One bearing's hold on the body it turns ({@code docs/feedback_micro_spec.md}, <em>Bearings and
 * ring gears</em>), shared by the full-size {@link RotaryBearingBlockEntity} and a micro bearing
 * voxel (held by its cell). The joint is a Sable rotary constraint between the bearing's parent --
 * the body its block is in, or the world -- and the body it turns, at the centre of the bearing's
 * front face, about its axis, with no motor of its own. A bearing on a car, and a bearing on what
 * another bearing turns, are the same case: the parent is a body. Positions the bearing hands in
 * ({@code local}) are in its own level space, which is the parent's plot when it has one.
 *
 * <h2>Driven by speed, every physics step</h2>
 * Each physics step the child's spin about the axis, relative to the parent's, is pulled toward the
 * run's speed by an angular impulse of {@code I (w_target - w) (1 - e^(-k dt))}, and the parent gets
 * the equal and opposite one, so it turns back against the arm as a real bearing's housing does; with
 * {@code I} the two bodies' reduced inertia about the axis (the child's alone against the world) -- the exact solution of a first-order lag, so
 * it is stable at any rate and any step, never overshooting -- capped at
 * {@link FTuning#ROTARY_BEARING_MAX_TORQUE} so a jammed body cannot be wound up. The target is the
 * run's speed straight from the network, nothing integrated.
 * <p>
 * The first version was a position servo chasing an angle the speed advanced each game tick
 * (Create Simulated's swivel bearing does that, at its own capped speeds). At 1024 RPM that angle
 * moves 307 degrees a tick: the servo saw a target leaping round the circle, the error flipped sign
 * at the wrap, and the body surged, slowed and reversed (playtest 2026-11-02, "wobbles"; the log's
 * target stepped -48 degrees per 40 ticks, which is 12288 mod 360). A speed has no wrap.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class BearingJoint {

    private static final Logger LOG = LoggerFactory.getLogger("feedback.joint");
    /** Ticks between diagnostic lines while a bearing holds a body (the frame bearings' cadence). */
    private static final int LOG_EVERY = 40;
    /** Joints with a live constraint, driven by the physics step. Server thread only. */
    private static final Set<BearingJoint> LIVE = Collections.newSetFromMap(new IdentityHashMap<>());

    /** World cells whose physics collider a micro bearing cleared (see MicroCellBlockEntity#clear). */
    public final List<BlockPos> cleared = new ArrayList<>();
    private int ticks;

    private final UUID body;
    /** The front face's centre in the body's plot, the axis there, and a direction across it there. */
    private final Vector3d plotAnchor = new Vector3d(), plotAxis = new Vector3d(), plotRef = new Vector3d();
    /** A block of the body in its plot, and where it stood in the world: the way back. */
    private final BlockPos homeFrom, homeTo;
    /** The body's moment of inertia about the axis, kpg x block^2; read when the joint is made. */
    private double bodyInertia;
    @Nullable
    private RotaryConstraintHandle joint;

    /** Set each game tick, read each physics step: where, which way, how fast (rad/s about the facing). */
    @Nullable
    private ServerLevel level;
    /** The axis in the parent's frame (the bearing's facing), and the parent the live joint was made to; null is the world. */
    private final Vector3d localAxis = new Vector3d();
    @Nullable
    private UUID parent;
    private double targetRadPerS;
    /** The reduced inertia the drive pulls, kpg x block^2: the child's, or child and parent's in series. */
    private double driveInertia;
    /** The parent's moment of inertia about the axis through its own centre of mass: what its reaction spins. */
    private double parentInertia;
    /** The last physics step's drive impulse about the axis, for the diagnostics. */
    private double lastDrive;

    private BearingJoint(UUID body, BlockPos homeFrom, BlockPos homeTo) {
        this.body = body;
        this.homeFrom = homeFrom;
        this.homeTo = homeTo;
    }

    /**
     * The hold on {@code made}, just assembled, with {@code home} one of its blocks' position before
     * (in the bearing's level space), {@code localAnchor} the hinge there, and {@code parent} the body
     * the bearing is in (null: the world).
     */
    public static BearingJoint of(ServerSubLevel made, Vector3d localAnchor, Direction facing, BlockPos home,
                                  @Nullable ServerSubLevel parent) {
        Vec3 homeWorld = parent == null ? Vec3.atCenterOf(home) : parent.logicalPose().transformPosition(Vec3.atCenterOf(home));
        BearingJoint j = new BearingJoint(made.getUniqueId(),
                BlockPos.containing(made.logicalPose().transformPositionInverse(homeWorld)), home);
        made.logicalPose().transformPositionInverse(toWorld(parent, localAnchor, true), j.plotAnchor);
        made.logicalPose().transformNormalInverse(toWorld(parent, normal(facing), false), j.plotAxis);
        made.logicalPose().transformNormalInverse(toWorld(parent, across(facing), false), j.plotRef);
        return j;
    }

    /** A point or direction from the parent's frame to the world's. */
    private static Vector3d toWorld(@Nullable ServerSubLevel parent, Vector3d v, boolean point) {
        if (parent == null)
            return new Vector3d(v);
        return point ? parent.logicalPose().transformPosition(new Vector3d(v)) : parent.logicalPose().transformNormal(new Vector3d(v));
    }

    /** The body the block at {@code pos} is in, or null for the world. */
    @Nullable
    public static ServerSubLevel parentOf(ServerLevel level, BlockPos pos) {
        return SableCompanion.INSTANCE.getContaining(level, pos) instanceof ServerSubLevel s ? s : null;
    }

    /**
     * True if {@code body} carries a bearing that holds a body of its own: put-back goes children
     * first, so the parent refuses until the child is put back.
     */
    public static boolean carriesChild(ServerLevel level, ServerSubLevel body) {
        var box = body.getPlot().getBoundingBox();
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            var be = level.getBlockEntity(p);
            if (be instanceof RotaryBearingBlockEntity b && b.holding()
                    || be instanceof io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity c && c.holdsBearing())
                return true;
        }
        return false;
    }

    public BlockPos homeFrom() {
        return homeFrom;
    }

    public BlockPos homeTo() {
        return homeTo;
    }

    /** The body's inertia counted on the run, in the network's unit. */
    public float runInertia() {
        return (float) bodyInertia * FTuning.ROTARY_BEARING_INERTIA_PER_KPG_BLOCK2;
    }

    @Nullable
    public ServerSubLevel held(ServerLevel level) {
        return ServerSubLevelContainer.getContainer(level).getSubLevel(body) instanceof ServerSubLevel s ? s : null;
    }

    /** True if the body is gone for good: the bearing should forget this hold. */
    public boolean gone(ServerLevel level) {
        ServerSubLevel s = held(level);
        return s != null && s.isRemoved();
    }

    /**
     * Makes the joint once the body is loaded and sets the speed the physics step drives it toward,
     * the bearing turning at {@code rpm} about its axis's positive direction. True when the body's
     * inertia moved and the run should be rebuilt to count it.
     */
    public boolean tick(ServerLevel level, BlockPos bearingPos, Vector3d localAnchor, Direction facing, float rpm) {
        ServerSubLevel s = held(level);
        if (s == null || s.isRemoved())
            return false; // not loaded yet: wait, as FrameBlockEntity.pair does
        ServerSubLevel p = parentOf(level, bearingPos);
        if (p != null && p.isRemoved())
            return false;
        UUID parentId = p == null ? null : p.getUniqueId();
        // The bearing moved to another body (a parent picked up and put down): make the joint anew.
        if (joint != null && joint.isValid() && !java.util.Objects.equals(parentId, parent))
            letGo();
        this.level = level;
        localAxis.set(normal(facing));
        // About the positive axis, so about the facing it is negated for a negative facing.
        float sign = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
        targetRadPerS = sign * rpm * 2 * Math.PI / 60;
        boolean rebuild = false;
        if (joint == null || !joint.isValid()) {
            var pipeline = SubLevelPhysicsSystem.get(level).getPipeline();
            joint = pipeline.addConstraint(p, s,
                    new RotaryConstraintConfiguration(localAnchor, new Vector3d(plotAnchor), normal(facing), new Vector3d(plotAxis)));
            parent = parentId;
            // The two sides meet at the bearing by design, as the frame bearings and Simulated's do.
            joint.setContactsEnabled(false);
            pipeline.wakeUp(s);
            if (p != null)
                pipeline.wakeUp(p);
            LIVE.add(this);
            double inertia = inertiaAbout(s, plotAnchor, plotAxis);
            if (Math.abs(inertia - bodyInertia) > 0.05 * bodyInertia) {
                bodyInertia = inertia;
                rebuild = true;
            }
        }
        // In series: the parent turns back as the child turns on; against the world, the child alone.
        // The body's own inertia when Sable has weighed it: a floor above it would overshoot a one-voxel arm a hundredfold.
        double child = bodyInertia > 0 ? bodyInertia : FTuning.ROTARY_BEARING_MIN_INERTIA;
        double housing = p == null ? 0 : inertiaAbout(p, localAnchor, localAxis);
        driveInertia = housing > 0 ? child * housing / (child + housing) : child;
        parentInertia = p == null ? 0 : spinInertia(p, localAxis);
        if (++ticks % LOG_EVERY == 0)
            diagnose(s, p, localAnchor, facing, rpm);
        return rebuild;
    }

    @SubscribeEvent
    public static void onPhysicsTick(ForgeSablePrePhysicsTickEvent event) {
        ServerLevel level = event.getPhysicsSystem().getLevel();
        double dt = event.getTimeStep();
        for (BearingJoint j : LIVE)
            if (j.level == level)
                j.drive(event.getPhysicsSystem(), dt);
    }

    /**
     * One physics step's pull of the child's spin about the axis, relative to the parent's, toward
     * the target speed; the parent takes the opposite impulse.
     */
    private void drive(SubLevelPhysicsSystem physics, double dt) {
        ServerSubLevel s = level == null ? null : held(level);
        if (s == null || s.isRemoved() || joint == null || !joint.isValid())
            return;
        ServerSubLevel p = parent == null ? null
                : ServerSubLevelContainer.getContainer(level).getSubLevel(parent) instanceof ServerSubLevel ps && !ps.isRemoved() ? ps : null;
        if (parent != null && p == null)
            return;
        RigidBodyHandle h = physics.getPhysicsHandle(s);
        RigidBodyHandle hp = p == null ? null : physics.getPhysicsHandle(p);
        if (h == null || (p != null && hp == null))
            return;
        Vector3d axis = toWorld(p, localAxis, false);
        double w = h.getAngularVelocity(new Vector3d()).dot(axis) - (hp == null ? 0 : hp.getAngularVelocity(new Vector3d()).dot(axis));
        // The step toward the target speed, ramped: no faster than the bearing's angular acceleration.
        double step = (targetRadPerS - w) * (1 - Math.exp(-FTuning.ROTARY_BEARING_DRIVE_RATE * dt));
        double ramp = FTuning.ROTARY_BEARING_MAX_ACCEL * dt;
        double impulse = driveInertia * Math.max(-ramp, Math.min(ramp, step));
        // The reaction may spin the parent no faster than its own limit, so a light car is not thrown
        // over by its arm starting: the ground's friction and the car's weight hold a gentle twist.
        if (p != null && parentInertia > 0) {
            double parentCap = parentInertia * FTuning.ROTARY_BEARING_MAX_PARENT_ACCEL * dt;
            impulse = Math.max(-parentCap, Math.min(parentCap, impulse));
        }
        double cap = FTuning.ROTARY_BEARING_MAX_TORQUE * dt;
        impulse = Math.max(-cap, Math.min(cap, impulse));
        lastDrive = impulse;
        if (impulse == 0)
            return;
        torque(h, s, new Vector3d(axis).mul(impulse));
        if (hp != null)
            torque(hp, p, new Vector3d(axis).mul(-impulse));
    }

    /**
     * A world angular impulse onto {@code body}, in its own frame as Sable takes it, and the body
     * woken: a ForceTotal wakes a body only for an impulse above about 3e-3, and a micro arm's drive
     * is below that, so an arm that had fallen asleep never started (playtest 2026-11-03).
     */
    private static void torque(RigidBodyHandle h, ServerSubLevel body, Vector3d worldImpulse) {
        h.applyLinearAndAngularImpulse(new Vector3d(), body.logicalPose().transformNormalInverse(worldImpulse, new Vector3d()), true);
    }

    /**
     * Diagnostic (2026-11-02, micro bearing playtests): the speed asked and the speed got about the
     * axis, the angle turned, what the joint and the drive push, and the body's figures.
     */
    private void diagnose(ServerSubLevel s, @Nullable ServerSubLevel p, Vector3d localAnchor, Direction facing, float rpm) {
        Vector3d axis = toWorld(p, normal(facing), false);
        // The axis as each body carries it: they should agree (dot 1) while the hinge holds.
        Vector3d childAxis = s.logicalPose().transformNormal(new Vector3d(plotAxis));
        Vector3d spin = RigidBodyHandle.of(s).getAngularVelocity(new Vector3d());
        Vector3d parentSpinVec = p == null ? new Vector3d() : RigidBodyHandle.of(p).getAngularVelocity(new Vector3d());
        double parentSpin = parentSpinVec.dot(axis);
        // Angular momentum about the axis, child (about the hinge) plus parent (about its centre): from
        // rest a correct reaction keeps it near zero; ground contact and gravity are the only other sources.
        double momentum = bodyInertia * spin.dot(axis) + parentInertia * parentSpin;
        LOG.info("[feedback] bearing axes: parentAxisWorld={} childAxisWorld={} agree={} childSpinAboutAxis={} parentSpinAboutAxis={} "
                        + "momentumAboutAxis={} parentInertiaCm={}",
                axis, childAxis, axis.dot(childAxis), spin.dot(axis), parentSpin, momentum, parentInertia);
        Vector3d lin = new Vector3d(), ang = new Vector3d();
        if (joint != null && joint.isValid())
            joint.getJointImpulses(lin, ang);
        LOG.info("[feedback] bearing at {} facing {} parent {}: rpm={} targetRadPerS={} measuredRelRadPerS={} parentRadPerS={} "
                        + "turnedDeg={} angVel={} driveImpulse={} impulseLin={} impulseAng={} mass={} inertiaAboutAxis={} "
                        + "driveInertia={} contactsOff=true clearedCells={}",
                localAnchor, facing, p == null ? "world" : p.getUniqueId(), rpm, targetRadPerS, spin.dot(axis) - parentSpin, parentSpin,
                turned(s, p, facing), spin.length(), lastDrive, lin.length(), ang.length(), s.getMassTracker().getMass(),
                bodyInertia, driveInertia, cleared.size());
    }

    /** How far, degrees about the facing, the body has turned against its parent since it was made: put-back and the log. */
    public float turned(ServerSubLevel s, @Nullable ServerSubLevel parent, Direction facing) {
        Vector3d n = toWorld(parent, normal(facing), false), u = toWorld(parent, across(facing), false),
                now = s.logicalPose().transformNormal(new Vector3d(plotRef));
        return (float) Math.toDegrees(Math.atan2(n.dot(new Vector3d(u).cross(now)), u.dot(now)));
    }

    private static Vector3d across(Direction f) {
        return f.getAxis() == Direction.Axis.Y ? new Vector3d(1, 0, 0) : new Vector3d(0, 1, 0);
    }

    public static Vector3d normal(Direction f) {
        return new Vector3d(f.getStepX(), f.getStepY(), f.getStepZ());
    }

    /** A body's moment of inertia about the line through its own centre of mass along {@code axis} (its plot's frame). */
    private static double spinInertia(ServerSubLevel s, Vector3d axis) {
        var mass = s.getMassTracker();
        Vector3d n = new Vector3d(axis).normalize();
        return n.dot(mass.getInertiaTensor().transform(new Vector3d(n)));
    }

    /** A body's moment of inertia about the line through {@code at} along {@code axis}, both in its plot: about its centre of mass, plus m d^2. */
    private static double inertiaAbout(ServerSubLevel s, Vector3d at, Vector3d axis) {
        var mass = s.getMassTracker();
        if (mass.getCenterOfMass() == null)
            return 0;
        Vector3d n = new Vector3d(axis).normalize();
        Vector3d r = new Vector3d(mass.getCenterOfMass()).sub(at);
        double d2 = r.lengthSquared() - r.dot(n) * r.dot(n);
        return n.dot(mass.getInertiaTensor().transform(new Vector3d(n))) + mass.getMass() * d2;
    }

    public void letGo() {
        LIVE.remove(this);
        if (joint != null && joint.isValid())
            joint.remove();
        joint = null;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Body", body);
        put(tag, "Anchor", plotAnchor);
        put(tag, "Axis", plotAxis);
        put(tag, "Ref", plotRef);
        tag.putLong("HomeFrom", homeFrom.asLong());
        tag.putLong("HomeTo", homeTo.asLong());
        ListTag list = new ListTag();
        for (BlockPos p : cleared)
            list.add(LongTag.valueOf(p.asLong()));
        tag.put("Cleared", list);
        return tag;
    }

    @Nullable
    public static BearingJoint load(CompoundTag tag) {
        if (!tag.hasUUID("Body"))
            return null;
        BearingJoint j = new BearingJoint(tag.getUUID("Body"), BlockPos.of(tag.getLong("HomeFrom")), BlockPos.of(tag.getLong("HomeTo")));
        get(tag, "Anchor", j.plotAnchor);
        get(tag, "Axis", j.plotAxis);
        get(tag, "Ref", j.plotRef);
        for (Tag t : tag.getList("Cleared", Tag.TAG_LONG))
            j.cleared.add(BlockPos.of(((LongTag) t).getAsLong()));
        return j;
    }

    private static void put(CompoundTag tag, String key, Vector3d v) {
        tag.putDouble(key + "X", v.x);
        tag.putDouble(key + "Y", v.y);
        tag.putDouble(key + "Z", v.z);
    }

    private static void get(CompoundTag tag, String key, Vector3d v) {
        v.set(tag.getDouble(key + "X"), tag.getDouble(key + "Y"), tag.getDouble(key + "Z"));
    }
}
