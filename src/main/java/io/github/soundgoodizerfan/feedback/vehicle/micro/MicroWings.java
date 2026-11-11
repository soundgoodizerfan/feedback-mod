// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.vehicle.AirDensityTable;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lift and drag on the thin flat sheets of a micro body, with stall (vehicles v2 spec, <em>wings</em>):
 * any voxel with nothing on either side of it along one axis and neighbours in its own plane is a piece
 * of a sheet one voxel thick, and each is an element of {@code 1/16} block^2 with that axis as its normal.
 * What a wing is, the shape of the build says; a plate on a hinged arm turns with the arm, since each
 * body carries its own. The force model is the frame wings': {@code q = 1/2 rho A s^2}, lift {@code k alpha}
 * to the stall angle and the flat plate's normal force past it, drag {@code CD0 + CDN sin^2 alpha},
 * applied at each element so the torque about the centre of mass comes free; per step the impulse is
 * capped at the body's momentum shared over the elements. Nothing is allocated per physics step.
 *
 * <p>The sheets are found on the game thread every {@link #RESCAN} ticks (a scan walks the plot) and
 * published whole; the physics thread only reads them.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class MicroWings {

    private static final int RESCAN = 40;
    private static final double ELEMENT_AREA = 1.0 / 16;
    private static final double[][] AXES = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};

    private final double[] nx, ny, nz, cx, cy, cz;
    private final int n;
    private final double stall, sinStall;
    private final ForceTotal total = new ForceTotal();
    private final Vector3d lin = new Vector3d(), ang = new Vector3d(), tmp = new Vector3d(), at = new Vector3d(), imp = new Vector3d();

    /** The sheets of each body with any, by Sable's id. Written on the game thread, read by the physics thread. */
    private static final Map<UUID, MicroWings> BODIES = new ConcurrentHashMap<>();

    private MicroWings(int n, double[] nx, double[] ny, double[] nz, double[] cx, double[] cy, double[] cz) {
        this.n = n;
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        // One voxel thick against a chord of about a block: the thinner the sheet the later it stalls.
        double r = Math.min(1, 0.25 / FTuning.WING_MAX_THIN);
        this.stall = FTuning.WING_STALL_MIN_RAD + (FTuning.WING_STALL_MAX_RAD - FTuning.WING_STALL_MIN_RAD) * r;
        this.sinStall = Math.sin(stall);
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }

    /** The sheets of {@code body}, or null if it has none. */
    static MicroWings scan(ServerLevel level, ServerSubLevel body) {
        var box = body.getPlot().getBoundingBox();
        LongOpenHashSet voxels = new LongOpenHashSet();
        // Voxel coordinates relative to the plot's box: a plot sits far from the origin and the key has 21 bits a side.
        int ox = box.minX() * 4, oy = box.minY() * 4, oz = box.minZ() * 4;
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()))
            if (level.getBlockEntity(p) instanceof MicroCellBlockEntity cell)
                for (int i = 0; i < MicroCellBlockEntity.VOXELS; i++)
                    if (cell.get(i) != null && !MicroWheels.isTire(cell.get(i)))
                        voxels.add(key(p.getX() * 4 + (i & 3) - ox, p.getY() * 4 + (i >> 2 & 3) - oy, p.getZ() * 4 + (i >> 4) - oz));
        if (voxels.isEmpty())
            return null;
        int cap = 64;
        double[][] out = new double[6][cap];
        int n = 0;
        for (long k : voxels) {
            int x = (int) (k >> 42 & 0x1FFFFF), y = (int) (k >> 21 & 0x1FFFFF), z = (int) (k & 0x1FFFFF);
            for (int a = 0; a < 3; a++) {
                int ax = (int) AXES[a][0], ay = (int) AXES[a][1], az = (int) AXES[a][2];
                if (voxels.contains(key(x + ax, y + ay, z + az)) || voxels.contains(key(x - ax, y - ay, z - az)))
                    continue;
                // The four neighbours in the plane: at least two make it a piece of a sheet.
                int around = 0;
                for (int b = 0; b < 3; b++) {
                    if (b == a)
                        continue;
                    int bx = (int) AXES[b][0], by = (int) AXES[b][1], bz = (int) AXES[b][2];
                    if (voxels.contains(key(x + bx, y + by, z + bz)))
                        around++;
                    if (voxels.contains(key(x - bx, y - by, z - bz)))
                        around++;
                }
                if (around < 2)
                    continue;
                if (n == cap) {
                    cap *= 2;
                    for (int j = 0; j < 6; j++)
                        out[j] = java.util.Arrays.copyOf(out[j], cap);
                }
                out[0][n] = AXES[a][0];
                out[1][n] = AXES[a][1];
                out[2][n] = AXES[a][2];
                out[3][n] = (x + ox + 0.5) / 4;
                out[4][n] = (y + oy + 0.5) / 4;
                out[5][n] = (z + oz + 0.5) / 4;
                n++;
                break;
            }
        }
        if (n == 0)
            return null;
        return new MicroWings(n, java.util.Arrays.copyOf(out[0], n), java.util.Arrays.copyOf(out[1], n), java.util.Arrays.copyOf(out[2], n),
                java.util.Arrays.copyOf(out[3], n), java.util.Arrays.copyOf(out[4], n), java.util.Arrays.copyOf(out[5], n));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % RESCAN != 0)
            return;
        java.util.Set<UUID> live = new java.util.HashSet<>();
        for (ServerLevel level : event.getServer().getAllLevels()) {
            var container = ServerSubLevelContainer.getContainer(level);
            if (container == null)
                continue;
            for (ServerSubLevel body : container.getAllSubLevels()) {
                if (body.isRemoved())
                    continue;
                live.add(body.getUniqueId());
                MicroWings wings = scan(level, body);
                if (wings == null)
                    BODIES.remove(body.getUniqueId());
                else
                    BODIES.put(body.getUniqueId(), wings);
            }
        }
        // Bodies put back or unloaded are forgotten.
        BODIES.keySet().retainAll(live);
    }

    @SubscribeEvent
    public static void onPhysicsTick(ForgeSablePrePhysicsTickEvent event) {
        if (BODIES.isEmpty())
            return;
        ServerLevel level = event.getPhysicsSystem().getLevel();
        var container = ServerSubLevelContainer.getContainer(level);
        double dt = event.getTimeStep();
        for (var e : BODIES.entrySet()) {
            if (!(container.getSubLevel(e.getKey()) instanceof ServerSubLevel body) || body.isRemoved())
                continue;
            RigidBodyHandle handle = RigidBodyHandle.of(body);
            if (handle == null)
                continue;
            double halfRho = FTuning.AIR_HALF_DENSITY_KPG_PER_BLOCK3 * AirDensityTable.at(level, body.logicalPose().position().y());
            e.getValue().tick(body, handle, halfRho, dt);
        }
    }

    private void tick(ServerSubLevel body, RigidBodyHandle handle, double halfRho, double dt) {
        MassData mass = body.getMassTracker();
        Pose3dc pose = body.logicalPose();
        Vector3dc com = mass.getCenterOfMass();
        if (halfRho <= 0 || com == null)
            return;
        handle.getLinearVelocity(tmp);
        pose.transformNormalInverse(tmp, lin);
        handle.getAngularVelocity(tmp);
        pose.transformNormalInverse(tmp, ang);
        double cap = mass.getMass() / n, k = FTuning.WING_LIFT_SLOPE, cdn = FTuning.WING_CD_NORMAL;
        boolean any = false;
        for (int i = 0; i < n; i++) {
            double rx = cx[i] - com.x(), ry = cy[i] - com.y(), rz = cz[i] - com.z();
            double vx = lin.x + ang.y * rz - ang.z * ry;
            double vy = lin.y + ang.z * rx - ang.x * rz;
            double vz = lin.z + ang.x * ry - ang.y * rx;
            double s2 = vx * vx + vy * vy + vz * vz;
            if (s2 < 1e-6)
                continue;
            double s = Math.sqrt(s2);
            double vn = (vx * nx[i] + vy * ny[i] + vz * nz[i]) / s; // signed sin(alpha)
            double sinA = Math.min(1, Math.abs(vn)), cosA = Math.sqrt(1 - sinA * sinA);
            double cl = sinA <= sinStall ? k * Math.asin(sinA) : cdn * sinA * cosA;
            double cd = FTuning.WING_CD0 + cdn * sinA * sinA;
            double q = halfRho * ELEMENT_AREA * s2 * dt;
            double sc = Math.min(1, cap * s / (q * (cl + cd)));
            double sg = vn < 0 ? 1 : -1, lift = cosA > 1e-3 ? q * cl * sc / cosA : 0, drag = q * cd * sc / s;
            imp.set(lift * (sg * nx[i] + sinA * vx / s) - drag * vx,
                    lift * (sg * ny[i] + sinA * vy / s) - drag * vy,
                    lift * (sg * nz[i] + sinA * vz / s) - drag * vz);
            total.applyImpulseAtPoint(mass, at.set(cx[i], cy[i], cz[i]), imp);
            any = true;
        }
        if (any)
            handle.applyForcesAndReset(total);
    }
}
