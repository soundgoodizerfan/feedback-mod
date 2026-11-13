// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.BodyPush;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.Set;

/**
 * A wheel on a micro body (vehicles v2 spec, <em>wheels and tires</em>): a body turned by a hinge whose
 * voxels include tire stock ({@link #TIRE}). The tire is a raycast with a spring and a damper, not a
 * collider -- tire voxels are left out of the body's collider ({@link MicroCellBlockEntity#applyCollider}) --
 * and its radius is read off the body: the farthest tire voxel from the axle. Nothing says a tire is
 * round; a ring of voxels is. Grip is the tire voxels' friction against the ground's.
 *
 * <p>Taken from the frame drivetrain's hub (three ground rays under the axle, spring sized so the car's
 * weight is shared over its wheels, damper no stronger than the closing speed, grip cap, rolling
 * resistance, and the rim rays that keep a wheel out of walls), with the wheel's own spin read off the
 * body rather than a network: the contact's slip is the velocity of the point of the body that touches.
 */
public final class MicroWheels {

    /** Blocks a tire can be made of: a property, so any modded rubber joins by tag. */
    public static final TagKey<Block> TIRE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, "tire_stock"));

    private static final double REACH = 0.25;
    private static final int GUARD_RAYS = 12;
    private static final double GUARD_SKIP_COS = 0.85;

    private MicroWheels() {
    }

    public static boolean isTire(BlockState state) {
        return state.is(TIRE);
    }

    /** What a scan of a body finds: the tread radius (blocks) and grip (mean friction of the tire voxels). */
    public record Shape(double radius, double friction, double rolling) {
    }

    /**
     * The wheel in {@code body}, turning about the line through {@code anchor} along {@code axis}
     * (both in its plot), or null if it has no tire voxel.
     */
    @Nullable
    public static Shape scan(ServerLevel level, ServerSubLevel body, Vector3d anchor, Vector3d axis) {
        double far = 0, friction = 0, rolling = 0;
        int n = 0;
        var box = body.getPlot().getBoundingBox();
        Vector3d a = new Vector3d(axis).normalize(), d = new Vector3d();
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (!(level.getBlockEntity(p) instanceof MicroCellBlockEntity cell) || !cell.hasTire())
                continue;
            for (int i = 0; i < MicroCellBlockEntity.VOXELS; i++) {
                BlockState v = cell.get(i);
                if (v == null || !isTire(v))
                    continue;
                d.set(p.getX() + ((i & 3) + 0.5) / 4, p.getY() + ((i >> 2 & 3) + 0.5) / 4, p.getZ() + ((i >> 4) + 0.5) / 4).sub(anchor);
                double along = d.dot(a);
                // Distance from the axle line, and half a voxel more for the tread's outer face.
                far = Math.max(far, Math.sqrt(Math.max(0, d.lengthSquared() - along * along)) + 0.125);
                friction += PhysicsBlockPropertyHelper.getFriction(v);
                rolling += deformation(v.getBlock().defaultDestroyTime(), PhysicsBlockPropertyHelper.getFriction(v));
                n++;
            }
        }
        return n == 0 ? null : new Shape(far, friction / n, rolling / n);
    }

    /**
     * One side's rolling resistance: {@code ROLLING_SOFT / (1 + hardness / ROLLING_HARDNESS)}, and next to
     * nothing on a slicker surface than {@code ROLLING_SLICK} (ice wastes none).
     */
    private static double deformation(double hardness, double friction) {
        double r = VehicleTuning.ROLLING_SOFT / (1 + Math.max(0, hardness) / VehicleTuning.ROLLING_HARDNESS);
        return friction < VehicleTuning.ROLLING_SLICK ? r * friction / VehicleTuning.ROLLING_SLICK : r;
    }

    /** What the ground gives under the tread: its own deformation, more if it falls (sand) or slows a walker (soul sand). */
    private static double groundRolling(BlockState ground) {
        double r = deformation(ground.getBlock().defaultDestroyTime(), PhysicsBlockPropertyHelper.getFriction(ground));
        if (ground.getBlock() instanceof net.minecraft.world.level.block.Fallable)
            r += VehicleTuning.ROLLING_LOOSE;
        float speed = ground.getBlock().getSpeedFactor();
        if (speed > 0 && speed < 1)
            r += VehicleTuning.ROLLING_SINK * (1 / speed - 1);
        return r;
    }

    /** Offroad's floor under grip, so a slick block still holds a little. */
    private static double fudge(double friction) {
        return friction < 1 ? 0.1 + 0.9 * friction : friction;
    }

    /**
     * One physics step of one wheel: the ground under the tread pushes the wheel body along the
     * ground's normal and grips it sideways and along. {@code centre} and {@code axis} are the hub and
     * the axle in the world; {@code carried} the mass this wheel bears; {@code mine} the bodies the rays
     * pass through (the vehicle's own). Returns the road's torque on the wheel about {@code axis}, in
     * kpg x block^2 / s^2, for the rotation network to feel.
     */
    public static double step(ServerLevel level, ServerSubLevel wheel, Set<SubLevel> mine, Vector3d centre, Vector3d axis,
                              Shape shape, double carried, double brake, double dt) {
        Vector3d down = new Vector3d(0, -1, 0);
        down.fma(-down.dot(axis), axis);
        if (down.lengthSquared() < 0.01)
            return 0; // an axle stood on end has no tread under it
        down.normalize();
        double omegaN = 2 * Math.PI * FTuning.SUSPENSION_HZ[1];
        Vector3d along = axis.cross(down, new Vector3d());
        guard(level, wheel, mine, centre, axis, down, shape, carried, omegaN, dt);

        double reach = shape.radius() + REACH, h = Double.MAX_VALUE;
        BlockState ground = null;
        Vector3d normal = null;
        for (int k = -1; k <= 1; k++) {
            Vector3d from = new Vector3d(along).mul(k * 0.5 * shape.radius()).add(centre);
            Vec3 a = new Vec3(from.x, from.y, from.z);
            Vec3 b = a.add(down.x * reach, down.y * reach, down.z * reach);
            ClipContext ray = new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
            ((ClipContextExtension) ray).sable$setSubLevelIgnoring(mine::contains);
            BlockHitResult hit = level.clip(ray);
            if (hit.getType() == HitResult.Type.MISS)
                continue;
            Vec3 at = hit.getLocation();
            var face = hit.getDirection().getNormal();
            Vector3d n = new Vector3d(face.getX(), face.getY(), face.getZ());
            var other = SableCompanion.INSTANCE.getContaining(level, at);
            if (other != null) {
                at = other.logicalPose().transformPosition(at);
                other.logicalPose().transformNormal(n);
            }
            double d = (at.x - a.x) * down.x + (at.y - a.y) * down.y + (at.z - a.z) * down.z;
            if (d < h) {
                h = d;
                ground = level.getBlockState(BlockPos.containing(hit.getLocation()));
                normal = n;
            }
        }
        if (ground == null || h >= shape.radius())
            return 0; // airborne: no force
        double compression = shape.radius() - h;
        Vector3d lever = new Vector3d(down).mul(h);
        Vector3d contact = new Vector3d(centre).add(lever);
        double bearing = -normal.dot(down);
        if (bearing <= 0.05)
            return 0;

        Vector3d v = BodyPush.pointVelocity(wheel, contact);
        double share = BodyPush.effectiveMass(wheel, contact, normal);
        share = Math.min(share, carried);
        double damping = Math.min(2 * FTuning.TIRE_DAMPING_RATIO * omegaN * dt, 1) * share * v.dot(normal) / dt;
        double load = (carried * omegaN * omegaN * compression - damping) * bearing;
        if (load <= 0)
            return 0;
        double grip = shape.friction() * fudge(PhysicsBlockPropertyHelper.getFriction(ground));

        // The tread's own speed at the contact is the body's velocity there: spin and travel together.
        Vector3d slip = new Vector3d(v);
        slip.fma(-slip.dot(normal), normal);
        Vector3d friction = new Vector3d(slip).mul(-share * VehicleTuning.TIRE_SLIP_RESPONSE);
        Vector3d roll = axis.cross(lever, new Vector3d());
        double treadLength = roll.length();
        if (treadLength > 1e-9) {
            roll.div(treadLength);
            roll.fma(-roll.dot(normal), normal);
            // A brake holds the wheel against the road's turning of it, never more than stops it this step.
            double rolling = v.dot(roll);
            if (brake > 0) {
                double hold = Math.min(brake * VehicleTuning.BRAKE_CLAMP_SU.value() / io.github.soundgoodizerfan.feedback.core.unit.Units.SU_PER_KPG_TORQUE / h * dt, share * Math.abs(rolling));
                friction.fma(-Math.signum(rolling) * hold, roll);
            }
        }
        double cap = grip * load * dt;
        if (friction.length() > cap)
            friction.normalize(cap);
        // Rolling resistance: the tread flexing as it rolls, never more than stops it this step.
        if (treadLength > 1e-9) {
            double rolling = v.dot(roll);
            double resist = Math.min((shape.rolling() + groundRolling(ground)) * load * dt, share * Math.abs(rolling));
            friction.fma(-Math.signum(rolling) * resist, roll);
        }
        Vector3d impulse = new Vector3d(normal).mul(load * dt).add(friction);
        BodyPush.impulseAt(wheel, contact, impulse);
        return axis.dot(lever.cross(friction, new Vector3d())) / dt;
    }

    /**
     * The rest of the rim: rays out from the hub in the tire's plane, all round but the ground patch.
     * A hit inside the radius pushes the wheel out along the struck face, with the spring and damper and
     * no grip, so a wheel stops at a wall or a step instead of passing through it (tire voxels have no collider).
     */
    private static void guard(ServerLevel level, ServerSubLevel wheel, Set<SubLevel> mine, Vector3d centre, Vector3d axis,
                              Vector3d down, Shape shape, double carried, double omegaN, double dt) {
        Vector3d side = axis.cross(down, new Vector3d());
        for (int k = 0; k < GUARD_RAYS; k++) {
            double a = 2 * Math.PI * k / GUARD_RAYS;
            Vector3d dir = new Vector3d(down).mul(Math.cos(a)).fma(Math.sin(a), side);
            if (dir.dot(down) > GUARD_SKIP_COS)
                continue;
            Vec3 from = new Vec3(centre.x, centre.y, centre.z);
            Vec3 to = from.add(dir.x * shape.radius(), dir.y * shape.radius(), dir.z * shape.radius());
            ClipContext ray = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
            ((ClipContextExtension) ray).sable$setSubLevelIgnoring(mine::contains);
            BlockHitResult hit = level.clip(ray);
            if (hit.getType() == HitResult.Type.MISS)
                continue;
            Vec3 at = hit.getLocation();
            var face = hit.getDirection().getNormal();
            Vector3d n = new Vector3d(face.getX(), face.getY(), face.getZ());
            var other = SableCompanion.INSTANCE.getContaining(level, at);
            if (other != null) {
                at = other.logicalPose().transformPosition(at);
                other.logicalPose().transformNormal(n);
            }
            Vector3d contact = new Vector3d(at.x, at.y, at.z);
            double depth = shape.radius() - contact.distance(centre);
            double facing = -n.dot(dir);
            if (depth <= 0 || facing <= 0.05)
                continue;
            Vector3d v = BodyPush.pointVelocity(wheel, contact);
            if (other instanceof ServerSubLevel o)
                v.sub(BodyPush.pointVelocity(o, contact));
            double share = Math.min(BodyPush.effectiveMass(wheel, contact, n), carried);
            double damping = Math.min(2 * FTuning.TIRE_DAMPING_RATIO * omegaN * dt, 1) * share * v.dot(n) / dt;
            double push = (share * omegaN * omegaN * depth - damping) * facing;
            if (push <= 0)
                continue;
            Vector3d impulse = new Vector3d(n).mul(push * dt);
            BodyPush.impulseAt(wheel, contact, impulse);
            if (other instanceof ServerSubLevel o)
                BodyPush.impulseAt(o, contact, impulse.negate(new Vector3d()));
        }
    }
}
