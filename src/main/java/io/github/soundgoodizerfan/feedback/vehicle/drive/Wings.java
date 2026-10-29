package io.github.soundgoodizerfan.feedback.vehicle.drive;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.vehicle.frame.Frame;
import net.minecraft.world.phys.Vec3;

/**
 * Lift and drag on every thin plate of a frame, with stall ({@code docs/feedback_vehicles_spec.md}
 * section 4, <em>Wings</em>). Design from Create Simulated (MIT; see {@code THIRD-PARTY-LICENSES.md}):
 * air speed at each element's own position, force applied there so the torque about the centre of
 * mass comes free. No code taken.
 *
 * <p>Build time ({@link #build}, when the chassis changes): everything a plate owns -- unit normal,
 * area, centroid, stall angle -- goes into flat primitive arrays. Per physics step ({@link #tick}):
 * the body's velocities rotated to the frame once, then per plate a cross product, two dot products,
 * one {@code asin}, a branch and one impulse. Nothing is allocated.
 *
 * <p>Forces: {@code q = 1/2 rho A s^2}. Lift {@code q CL} acts square to the flow, drag
 * {@code q CD} against it, {@code CD = CD0 + CDN sin^2(alpha)}. {@code CL = k alpha} up to the plate's
 * stall angle; past it the flow has let go and the plate is a bluff sheet, {@code CL = CDN sin(alpha) cos(alpha)}
 * (flat-plate normal force), which is far lower, so lift collapses. Dissipative: lift is perpendicular to
 * the element's velocity so does no work, and drag {@code >= CD0 q > 0} always opposes it, so a plate
 * never adds energy. Per step the impulse is capped at the body's momentum shared across plates, so the
 * explicit step cannot overshoot either.
 */
final class Wings {
    private int n;
    private double[] nx = new double[0], ny, nz, cx, cy, cz, area, stall, sinStall;

    /** Rebuild from the frame; run only when the frame changed. */
    void build(Frame f) {
        var list = f.plates().stream().filter(f::isWing).toList();
        n = list.size();
        nx = new double[n]; ny = new double[n]; nz = new double[n];
        cx = new double[n]; cy = new double[n]; cz = new double[n];
        area = new double[n]; stall = new double[n]; sinStall = new double[n];
        for (int i = 0; i < n; i++) {
            Frame.Plate p = list.get(i);
            Vec3 c = f.cross(p);
            double len = c.length();
            nx[i] = c.x / len; ny[i] = c.y / len; nz[i] = c.z / len;
            area[i] = len / 2;
            Vec3 m = f.pos(p.a()).add(f.pos(p.b())).add(f.pos(p.c())).scale(1.0 / 3);
            cx[i] = m.x; cy[i] = m.y; cz[i] = m.z;
            double r = Math.min(1, f.thin(p) / FTuning.WING_MAX_THIN);
            stall[i] = FTuning.WING_STALL_MIN_RAD + (FTuning.WING_STALL_MAX_RAD - FTuning.WING_STALL_MIN_RAD) * r;
            sinStall[i] = Math.sin(stall[i]);
        }
    }

    private final Vector3d lin = new Vector3d(), ang = new Vector3d(), tmp = new Vector3d(), at = new Vector3d(),
            imp = new Vector3d();

    /** @return whether any impulse was added. {@code halfRho} is half the air density in kpg per block^3. */
    boolean tick(ForceTotal out, MassData mass, Pose3dc pose, RigidBodyHandle handle, Vec3 origin, double halfRho, double dt) {
        Vector3dc com = mass.getCenterOfMass();
        if (n == 0 || halfRho <= 0 || com == null)
            return false;
        handle.getLinearVelocity(tmp);
        pose.transformNormalInverse(tmp, lin);
        handle.getAngularVelocity(tmp);
        pose.transformNormalInverse(tmp, ang);
        double ox = origin.x - com.x(), oy = origin.y - com.y(), oz = origin.z - com.z();
        double cap = mass.getMass() / n, k = FTuning.WING_LIFT_SLOPE, cdn = FTuning.WING_CD_NORMAL;
        boolean any = false;
        for (int i = 0; i < n; i++) {
            double rx = cx[i] + ox, ry = cy[i] + oy, rz = cz[i] + oz;
            double vx = lin.x + ang.y * rz - ang.z * ry;
            double vy = lin.y + ang.z * rx - ang.x * rz;
            double vz = lin.z + ang.x * ry - ang.y * rx;
            double s2 = vx * vx + vy * vy + vz * vz;
            if (s2 < 1e-6)
                continue;
            double s = Math.sqrt(s2);
            double vn = (vx * nx[i] + vy * ny[i] + vz * nz[i]) / s; // signed sin(alpha)
            double sinA = Math.min(1, Math.abs(vn)), cosA = Math.sqrt(1 - sinA * sinA);
            double cl = sinA <= sinStall[i] ? k * Math.asin(sinA) : cdn * sinA * cosA;
            double cd = FTuning.WING_CD0 + cdn * sinA * sinA;
            double q = halfRho * area[i] * s2 * dt; // impulse scale: 1/2 rho A s^2 dt
            double sc = Math.min(1, cap * s / (q * (cl + cd)));
            // lift direction: the plate's windward normal turned square to the flow, (m + sinA v^) / cosA, m = -sign(vn) n
            double sg = vn < 0 ? 1 : -1, lift = cosA > 1e-3 ? q * cl * sc / cosA : 0, drag = q * cd * sc / s;
            imp.set(lift * (sg * nx[i] + sinA * vx / s) - drag * vx,
                    lift * (sg * ny[i] + sinA * vy / s) - drag * vy,
                    lift * (sg * nz[i] + sinA * vz / s) - drag * vz);
            out.applyImpulseAtPoint(mass, at.set(cx[i] + origin.x, cy[i] + origin.y, cz[i] + origin.z), imp);
            any = true;
        }
        return any;
    }
}
