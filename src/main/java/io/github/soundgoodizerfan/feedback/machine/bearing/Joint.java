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

import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
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
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroWheels;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
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
 * One bearing's hold on the body it turns ({@code docs/specs/feedback_micro_spec.md}, <em>Bearings and
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
public final class Joint {

    private static final Logger LOG = LoggerFactory.getLogger("feedback.joint");
    /** Ticks between diagnostic lines while a bearing holds a body (the frame bearings' cadence). */
    private static final int LOG_EVERY = 40;
    /** Joints with a live constraint, driven by the physics step. Server thread only. */
    private static final Set<Joint> LIVE = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** World cells whose physics collider a micro bearing cleared (see MicroCellBlockEntity#clear). */
    public final List<BlockPos> cleared = new ArrayList<>();
    private int ticks;

    /**
     * What the joint allows between parent and body. A hinge turns about the facing and is driven
     * (the rest of this class); a ball swings inside a cone; a lock holds the two as one rigid piece;
     * a slider moves along the facing against a spring; a piston is a slider a servo holds at a level.
     */
    public enum Kind { HINGE, BALL, LOCKED, SLIDER, PISTON }

    private Kind kind = Kind.HINGE;
    /** A linkage: it ties a body that already existed to the joint's own, instead of holding one the joint made. Putting it back only lets it go. */
    private boolean linked;
    /** The cone (ball) or spring band (slider) the player chose: an index into {@link FTuning#BALL_CONE_RAD} or {@link FTuning#SUSPENSION_HZ}. */
    private int variant = 1;
    /** Reach either side of rest of a slider or piston, blocks. */
    private float travel = FTuning.SPRING_TRAVEL;
    /** 1 as built, falling to {@link FTuning#JOINT_CONDITION_FLOOR} by misuse only; it cuts the torque cap and the piston's force. */
    private volatile float condition = 1;
    /** The wheel this hinge turns (tire voxels on its body), rescanned every few seconds on the game thread; null if none. */
    @Nullable
    private volatile MicroWheels.Shape wheel;
    /** The vehicle's bodies (rays pass through them) and the mass each of its wheels bears. */
    private volatile Set<dev.ryanhcode.sable.sublevel.SubLevel> vehicle = Set.of();
    private volatile double carried;
    /** Brake level 0 to 1, from a card; the road's torque on the wheel summed over physics steps, kpg x block^2 / s. */
    private volatile float brake;
    /** The arm's weight about the hinge's axis this tick, kpg x block^2 / s^2; read with the road's. */
    private volatile double gravityTorque;
    private long scanEdits = -1;
    /** 1 as built, falling to the floor when the drive pushes against a held brake past the grace; it scales the brake's grip. */
    private volatile float pad = 1;
    private final java.util.concurrent.atomic.AtomicInteger padWorn = new java.util.concurrent.atomic.AtomicInteger();
    /** Game ticks a ball, slider or piston has been forced against its stop; game thread only. */
    private int strainRun;
    /** How the body sat against its parent when the constraint was made, in the parent's frame (ball swing zero). */
    private final Quaterniond relative0 = new Quaterniond();
    private double roadImpulse;
    /** Physics steps the hinge's torque sat at its cap with no relative turning, summed for the game tick. */
    private final java.util.concurrent.atomic.AtomicInteger stalledTicks = new java.util.concurrent.atomic.AtomicInteger();
    /** Consecutive physics steps the drive has sat at its cap without the arm turning; physics thread only. */
    private int stallRun;
    /** Held angle (rad, from the constraint's own zero) when a hinge rests; NaN while it is driven. */
    private double holdAngle = Double.NaN;
    /** Angle (deg) the body had turned against its parent when the live constraint was made: the servo's zero. */
    private float zeroTurn;
    private float angleLevel;
    /** A piston's level, -1 to 1 of its travel, from the card; a slider ignores it. */
    private float pistonLevel;
    /** The live constraint as a Generic one, to move its motor; null for the other kinds. */
    @Nullable
    private GenericConstraintHandle generic;

    private final UUID body;
    /** The front face's centre in the body's plot, the axis there, and a direction across it there. */
    private final Vector3d plotAnchor = new Vector3d(), plotAxis = new Vector3d(), plotRef = new Vector3d();
    /** A block of the body in its plot, and where it stood in the world: the way back. */
    private final BlockPos homeFrom;
    private BlockPos homeTo;
    /** The body's moment of inertia about the axis, kpg x block^2; read when the joint is made. */
    private volatile double bodyInertia;
    @Nullable
    private volatile PhysicsConstraintHandle joint;

    /** Set each game tick, read each physics step: where, which way, how fast (rad/s about the facing). */
    @Nullable
    private volatile ServerLevel level;
    /** The axis in the parent's frame (the bearing's facing), and the parent the live joint was made to; null is the world. */
    private final Vector3d localAxis = new Vector3d();
    @Nullable
    private volatile UUID parent;
    private volatile double targetRadPerS;
    /** The reduced inertia the drive pulls, kpg x block^2: the child's, or child and parent's in series. */
    private volatile double driveInertia;
    /** The parent's moment of inertia about the axis through its own centre of mass: what its reaction spins. */
    private volatile double parentInertia;
    /** The last physics step's drive impulse about the axis, for the diagnostics. */
    private double lastDrive;

    private Joint(UUID body, BlockPos homeFrom, BlockPos homeTo) {
        this.body = body;
        this.homeFrom = homeFrom;
        this.homeTo = homeTo;
    }

    /**
     * The hold on {@code made}, just assembled, with {@code home} one of its blocks' position before
     * (in the bearing's level space), {@code localAnchor} the hinge there, and {@code parent} the body
     * the bearing is in (null: the world).
     */
    public static Joint of(ServerSubLevel made, Vector3d localAnchor, Direction facing, BlockPos home,
                                  @Nullable ServerSubLevel parent) {
        return of(Kind.HINGE, 0, made, localAnchor, facing, home, parent);
    }

    /** As {@link #of(ServerSubLevel, Vector3d, Direction, BlockPos, ServerSubLevel)} for any kind; {@code variant} is the cone or spring band. */
    public static Joint of(Kind kind, int variant, ServerSubLevel made, Vector3d localAnchor, Direction facing, BlockPos home,
                           @Nullable ServerSubLevel parent) {
        Vec3 homeWorld = parent == null ? Vec3.atCenterOf(home) : parent.logicalPose().transformPosition(Vec3.atCenterOf(home));
        Joint j = new Joint(made.getUniqueId(),
                BlockPos.containing(made.logicalPose().transformPositionInverse(homeWorld)), home);
        j.kind = kind;
        j.variant = Math.clamp(variant, 0, kind == Kind.HINGE ? 4 : 2);
        if (kind == Kind.PISTON)
            j.travel = FTuning.PISTON_TRAVEL;
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

    /** The body this joint holds, by Sable's id. */
    public UUID bodyId() {
        return body;
    }

    /**
     * The parent moved by {@code delta} blocks inside its space (a Lift carried it into a body's plot):
     * the way back and the cleared cells follow it.
     */
    public void shift(BlockPos delta) {
        homeTo = homeTo.offset(delta);
        cleared.replaceAll(p -> p.offset(delta));
    }

    public Kind kind() {
        return kind;
    }

    public boolean linked() {
        return linked;
    }

    public void markLinked() {
        linked = true;
    }

    public float condition() {
        return condition;
    }

    /** The brake pad's condition, 1 to the floor. */
    public float pad() {
        return pad;
    }

    /** A new bearing fitted: the joint and its pad are as built. */
    public void repair() {
        condition = 1;
        pad = 1;
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
     * Makes the joint once the body is loaded. A hinge turns at {@code command} RPM about its axis's
     * positive direction (the speed the physics step drives it toward); a piston is held at
     * {@code command} of its travel, -1 to 1; the other kinds ignore it. True when the body's inertia
     * moved and the run should be rebuilt to count it.
     */
    public boolean tick(ServerLevel level, BlockPos bearingPos, Vector3d localAnchor, Direction facing, float command) {
        // A hinge's role: 0 turns at the shaft's speed; 1 steers and 2 to 4 are control surfaces, held at an angle by a level.
        float rpm = kind == Kind.HINGE && variant == 0 ? command : 0;
        angleLevel = kind == Kind.HINGE && variant > 0 ? Math.clamp(command, -1f, 1f) : 0;
        pistonLevel = kind == Kind.PISTON ? Math.clamp(command, -1f, 1f) : 0;
        // Nothing pushes the run while this joint cannot say what its arm weighs (set again below).
        gravityTorque = 0;
        int worn = padWorn.getAndSet(0);
        if (worn > 0)
            pad = Math.max(FTuning.JOINT_CONDITION_FLOOR, pad - worn * FTuning.BRAKE_PAD_WEAR_PER_STEP);
        int stalled = stalledTicks.getAndSet(0);
        if (stalled > 0) {
            condition = Math.max(FTuning.JOINT_CONDITION_FLOOR, condition - stalled * FTuning.JOINT_STALL_WEAR_PER_TICK);
        }
        ServerSubLevel s = held(level);
        if (s == null || s.isRemoved())
            return false; // not loaded yet: wait
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
            made(pipeline, p, s, localAnchor, facing);
            parent = parentId;
            zeroTurn = turned(s, p, facing);
            holdAngle = Double.NaN;
            // The two sides meet at the joint by design, as the frame bearings and Simulated's do.
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
        servo(s, p, facing, localAnchor);
        if (kind == Kind.HINGE && ticks % 20 == 0 && editsMoved())
            scanWheel(level, s);
        if (kind == Kind.HINGE && variant == 0)
            gravityOn(s, p, facing);
        if (++ticks % LOG_EVERY == 0)
            diagnose(s, p, localAnchor, facing, rpm);
        return rebuild;
    }

    /** The Sable constraint for this kind, between {@code p} (null: the world) and {@code s}, anchors in each one's own space. */
    private void made(dev.ryanhcode.sable.api.physics.PhysicsPipeline pipeline, @Nullable ServerSubLevel p, ServerSubLevel s,
                      Vector3d localAnchor, Direction facing) {
        Vector3d anchor = new Vector3d(plotAnchor);
        // How each side is turned now, so a rebuilt joint (a reload) keeps the pose and closes no gap by snapping.
        Quaterniond qp = p == null ? new Quaterniond() : new Quaterniond(p.logicalPose().orientation());
        Quaterniond qs = new Quaterniond(s.logicalPose().orientation());
        Quaterniond relative = new Quaterniond(qp).conjugate().mul(qs);
        // The frame whose x is the facing, so a slider's free axis is the facing.
        Quaterniond twist = new Quaterniond().rotationTo(1, 0, 0, facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Quaterniond twistChild = new Quaterniond(relative).conjugate().mul(twist);
        generic = null;
        relative0.set(relative);
        switch (kind) {
            case HINGE -> joint = pipeline.addConstraint(p, s,
                    new RotaryConstraintConfiguration(localAnchor, anchor, normal(facing), new Vector3d(plotAxis)));
            case LOCKED -> joint = pipeline.addConstraint(p, s, new FixedConstraintConfiguration(localAnchor, anchor, relative));
            case BALL -> {
                joint = pipeline.addConstraint(p, s, new GenericConstraintConfiguration(localAnchor, anchor,
                        new Quaterniond(), new Quaterniond(relative).conjugate(), Set.of(ConstraintJointAxis.LINEAR)));
                generic = (GenericConstraintHandle) joint;
                float cone = FTuning.BALL_CONE_RAD[variant];
                // Swing limited either side, twist free: a ball joint turns about its own shaft.
                generic.setLimit(ConstraintJointAxis.ANGULAR_Y, -cone, cone);
                generic.setLimit(ConstraintJointAxis.ANGULAR_Z, -cone, cone);
            }
            case SLIDER, PISTON -> {
                joint = pipeline.addConstraint(p, s, new GenericConstraintConfiguration(localAnchor, anchor,
                        twist, twistChild, EnumSet.of(ConstraintJointAxis.LINEAR_Y, ConstraintJointAxis.LINEAR_Z,
                        ConstraintJointAxis.ANGULAR_X, ConstraintJointAxis.ANGULAR_Y, ConstraintJointAxis.ANGULAR_Z)));
                generic = (GenericConstraintHandle) joint;
                generic.setLimit(ConstraintJointAxis.LINEAR_X, -travel, travel);
            }
        }
    }

    /**
     * Once a game tick: the slider's spring, the piston's servo and the hinge's hold at rest. Gains
     * scale with the moving mass (inertia for a turn), so a heavy arm is held as firmly as a light one.
     * The motor is Sable's PD motor and stays set in between.
     */
    private void servo(ServerSubLevel s, @Nullable ServerSubLevel p, Direction facing, Vector3d localAnchor) {
        if (joint == null || !joint.isValid())
            return;
        // Wear by misuse only: forced against the stop, or a piston that cannot reach its level, for longer than a working load lasts.
        if (strained(s, p, facing, localAnchor)) {
            if (++strainRun > FTuning.JOINT_STRAIN_GRACE_TICKS)
                condition = Math.max(FTuning.JOINT_CONDITION_FLOOR, condition - FTuning.JOINT_STRAIN_WEAR_PER_TICK);
        } else
            strainRun = 0;
        switch (kind) {
            case BALL -> {
                if (generic != null) {
                    // A worn ball lets the cone open: slack, never a sudden loss.
                    float cone = FTuning.BALL_CONE_RAD[variant] * (2 - condition);
                    generic.setLimit(ConstraintJointAxis.ANGULAR_Y, -cone, cone);
                    generic.setLimit(ConstraintJointAxis.ANGULAR_Z, -cone, cone);
                }
            }
            case SLIDER, PISTON -> {
                double mb = s.getMassTracker().getMass(), ma = p == null ? 0 : p.getMassTracker().getMass();
                double reduced = ma <= 0 ? mb : mb <= 0 ? 0 : ma * mb / (ma + mb);
                if (reduced <= 0 || generic == null)
                    return;
                double hz = kind == Kind.SLIDER ? FTuning.SUSPENSION_HZ[variant] : FTuning.PISTON_SERVO_RATE / (2 * Math.PI);
                double omega = 2 * Math.PI * hz;
                double target = kind == Kind.PISTON ? pistonLevel * travel : 0;
                double limit = kind == Kind.PISTON ? FTuning.ROTARY_BEARING_MAX_TORQUE * condition : 0;
                generic.setMotor(ConstraintJointAxis.LINEAR_X, target, reduced * omega * omega,
                        2 * FTuning.TIRE_DAMPING_RATIO * omega * reduced, kind == Kind.PISTON, limit);
            }
            case HINGE -> {
                double inertia = bodyInertia > 0 ? bodyInertia : FTuning.ROTARY_BEARING_MIN_INERTIA;
                if (variant > 0) {
                    // Steering and control surfaces: a position servo to the level, its force the driver's arm.
                    double rate = FTuning.STEER_SERVO_RATE, max = variant == 1 ? FTuning.STEER_MAX_ANGLE : FTuning.SURFACE_MAX_ANGLE;
                    joint.setMotor(RotaryConstraintHandle.DEFAULT_AXIS, angleLevel * max, rate * rate * inertia,
                            2 * FTuning.STEER_SERVO_DAMPING_RATIO * rate * inertia, true,
                            FTuning.STEER_ARM_SU.value() / io.github.soundgoodizerfan.feedback.core.unit.Units.SU_PER_KPG_TORQUE);
                    return;
                }
                double w = relativeSpin(s, p, facing);
                if (targetRadPerS == 0 && Math.abs(w) < FTuning.JOINT_HOLD_BELOW_RAD_PER_S) {
                    // At rest and asked for nothing: hold the angle it settled at, so an unbalanced arm does not creep.
                    if (Double.isNaN(holdAngle))
                        holdAngle = Math.toRadians(turned(s, p, facing) - zeroTurn);
                    double rate = FTuning.JOINT_HOLD_RATE;
                    joint.setMotor(RotaryConstraintHandle.DEFAULT_AXIS, holdAngle, rate * rate * inertia,
                            2 * FTuning.STEER_SERVO_DAMPING_RATIO * rate * inertia, true,
                            FTuning.STEER_ARM_SU.value() / io.github.soundgoodizerfan.feedback.core.unit.Units.SU_PER_KPG_TORQUE);
                } else {
                    holdAngle = Double.NaN;
                    // A free bearing is not frictionless (BEARING_DAMPING_RATE): damping scaled by the arm's inertia.
                    joint.setMotor(RotaryConstraintHandle.DEFAULT_AXIS, 0, 0, FTuning.BEARING_DAMPING_RATE * inertia, false, 0);
                }
            }
            default -> {
            }
        }
    }

    /**
     * True when a ball, slider or piston is being forced: a ball at the edge of its cone, a slider
     * bottomed against either stop, a piston more than a third of its travel from the level it was asked for.
     */
    private boolean strained(ServerSubLevel s, @Nullable ServerSubLevel p, Direction facing, Vector3d localAnchor) {
        switch (kind) {
            case BALL -> {
                if (generic == null)
                    return false;
                Quaterniond now = new Quaterniond(p == null ? new Quaterniond() : new Quaterniond(p.logicalPose().orientation())).conjugate()
                        .mul(s.logicalPose().orientation());
                Quaterniond turn = now.mul(new Quaterniond(relative0).conjugate());
                Vector3d x = turn.transform(new Vector3d(1, 0, 0));
                double swing = Math.acos(Math.clamp(x.x, -1.0, 1.0));
                return swing >= 0.97 * FTuning.BALL_CONE_RAD[variant] * (2 - condition);
            }
            case SLIDER, PISTON -> {
                Vector3d axis = toWorld(p, normal(facing), false);
                Vector3d child = s.logicalPose().transformPosition(new Vector3d(plotAnchor), new Vector3d());
                Vector3d mount = p == null ? new Vector3d(localAnchor) : p.logicalPose().transformPosition(new Vector3d(localAnchor), new Vector3d());
                double offset = child.sub(mount).dot(axis);
                if (kind == Kind.SLIDER)
                    return Math.abs(offset) >= travel - 0.02;
                return Math.abs(offset - pistonLevel * travel) > travel / 3;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Looks for tire voxels on the turned body, and counts the wheels the vehicle shares its weight
     * over. Game thread, once a second: a build changes rarely, and a scan walks the plot.
     */
    private void scanWheel(ServerLevel level, ServerSubLevel s) {
        MicroWheels.Shape shape = MicroWheels.scan(level, s, plotAnchor, plotAxis);
        wheel = shape;
        if (shape == null) {
            vehicle = Set.of();
            return;
        }
        var all = io.github.soundgoodizerfan.feedback.vehicle.Superobject.of(level, s);
        double mass = 0;
        int wheels = 0;
        for (ServerSubLevel b : all) {
            mass += b.getMassTracker().getMass();
            for (var h : io.github.soundgoodizerfan.feedback.vehicle.Superobject.holds(level, b))
                if (h.joint().kind() == Kind.HINGE && MicroWheels.scan(level, h.child(), h.joint().plotAnchor, h.joint().plotAxis) != null)
                    wheels++;
        }
        carried = mass / Math.max(1, wheels);
        vehicle = new java.util.HashSet<dev.ryanhcode.sable.sublevel.SubLevel>(all);
    }

    /** True once per change of the world's micro edits: a plot is scanned for wheels only after something was built or broken. */
    private boolean editsMoved() {
        long e = io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity.edits();
        if (e == scanEdits)
            return false;
        scanEdits = e;
        return true;
    }

    /**
     * The arm's weight about the hinge, as a torque the run feels: an off-centre arm is load going up and a help coming down. Only while
     * the arm is driven or turning: one held at rest by its servo pushes nothing.
     */
    private void gravityOn(ServerSubLevel s, @Nullable ServerSubLevel p, Direction facing) {
        var mass = s.getMassTracker();
        if (mass.getCenterOfMass() == null || targetRadPerS == 0 && Math.abs(relativeSpin(s, p, facing)) < FTuning.JOINT_HOLD_BELOW_RAD_PER_S) {
            gravityTorque = 0;
            return;
        }
        Vector3d com = s.logicalPose().transformPosition(new Vector3d(mass.getCenterOfMass()), new Vector3d());
        Vector3d anchor = s.logicalPose().transformPosition(new Vector3d(plotAnchor), new Vector3d());
        Vector3d axis = toWorld(p, localAxis, false).normalize();
        Vector3d lever = com.sub(anchor);
        // r x (m g down) . axis
        gravityTorque = lever.cross(new Vector3d(0, -mass.getMass() * FTuning.GRAVITY_BLOCKS_PER_S2, 0), new Vector3d()).dot(axis);
    }

    /** Sets the brake level, 0 to 1 (a card). */
    public void setBrake(float level) {
        brake = Math.clamp(level, 0f, 1f);
    }

    /**
     * The road's torque on the wheel this game tick, in Su, along the rotation network's positive sense,
     * and the sum cleared. The network feels it as a push: a wheel the road drives faster than the shaft
     * helps it, one the road holds back loads it.
     */
    public float takeRoadSu(float facingSign, double ticks) {
        double impulse;
        synchronized (this) {
            impulse = roadImpulse;
            roadImpulse = 0;
        }
        return (float) (facingSign * (impulse / Math.max(ticks / 20.0, 1e-6) + gravityTorque) * io.github.soundgoodizerfan.feedback.core.unit.Units.SU_PER_KPG_TORQUE);
    }

    /** The body's spin about the facing relative to its parent's, rad/s (positive about the positive axis). */
    private double relativeSpin(ServerSubLevel s, @Nullable ServerSubLevel p, Direction facing) {
        Vector3d axis = toWorld(p, normal(facing), false);
        double w = RigidBodyHandle.of(s).getAngularVelocity(new Vector3d()).dot(axis);
        return p == null ? w : w - RigidBodyHandle.of(p).getAngularVelocity(new Vector3d()).dot(axis);
    }

    @SubscribeEvent
    public static void onPhysicsTick(ForgeSablePrePhysicsTickEvent event) {
        ServerLevel level = event.getPhysicsSystem().getLevel();
        double dt = event.getTimeStep();
        for (Joint j : LIVE)
            if (j.level == level)
                j.drive(event.getPhysicsSystem(), dt);
    }

    /**
     * One physics step's pull of the child's spin about the axis, relative to the parent's, toward
     * the target speed; the parent takes the opposite impulse.
     */
    private void drive(SubLevelPhysicsSystem physics, double dt) {
        ServerSubLevel s = level == null ? null : held(level);
        if (kind != Kind.HINGE || variant != 0 || s == null || s.isRemoved() || joint == null || !joint.isValid())
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
        MicroWheels.Shape wh = wheel;
        if (wh != null) {
            Vector3d centre = s.logicalPose().transformPosition(new Vector3d(plotAnchor), new Vector3d());
            double road = MicroWheels.step(level, s, vehicle, centre, axis, wh, carried, brake * pad, dt);
            if (road != 0)
                synchronized (this) {
                    roadImpulse += road * dt;
                }
        }
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
        double cap = FTuning.ROTARY_BEARING_MAX_TORQUE * condition * dt;
        // Wear by misuse only: a drive pinned at its cap while the arm does not turn (a stalled drive).
        // Starting a heavy arm, or a drive held by its brake, sits at the cap too: only a stall that goes on past the grace is misuse.
        if (Math.abs(impulse) >= cap && Math.abs(w) < FTuning.JOINT_HOLD_BELOW_RAD_PER_S) {
            // The drive pushing against a held brake wears the pad; the same push with no brake wears the joint.
            if (++stallRun > FTuning.JOINT_STALL_GRACE_STEPS) {
                if (brake > 0 && wheel != null)
                    padWorn.incrementAndGet();
                else
                    stalledTicks.incrementAndGet();
            }
        } else
            stallRun = 0;
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
        tag.putString("Kind", kind.name());
        tag.putBoolean("Linked", linked);
        tag.putInt("Variant", variant);
        tag.putFloat("Condition", condition);
        tag.putFloat("Pad", pad);
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
    public static Joint load(CompoundTag tag) {
        if (!tag.hasUUID("Body"))
            return null;
        Joint j = new Joint(tag.getUUID("Body"), BlockPos.of(tag.getLong("HomeFrom")), BlockPos.of(tag.getLong("HomeTo")));
        if (tag.contains("Kind"))
            j.kind = parseKind(tag.getString("Kind"));
        j.linked = tag.getBoolean("Linked");
        j.variant = Math.clamp(tag.getInt("Variant"), 0, 4);
        j.condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1;
        j.pad = tag.contains("Pad") ? tag.getFloat("Pad") : 1;
        if (j.kind == Kind.PISTON)
            j.travel = FTuning.PISTON_TRAVEL;
        get(tag, "Anchor", j.plotAnchor);
        get(tag, "Axis", j.plotAxis);
        get(tag, "Ref", j.plotRef);
        for (Tag t : tag.getList("Cleared", Tag.TAG_LONG))
            j.cleared.add(BlockPos.of(((LongTag) t).getAsLong()));
        return j;
    }

    private static Kind parseKind(String name) {
        try {
            return Kind.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Kind.HINGE;
        }
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
