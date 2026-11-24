// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;

/**
 * The rocket route to the acid-cloud world (roadmap 6.10), built as {@link EndRoute} is: the world is a point in the Gap, the Gap's origin
 * point plus {@link SpaceTuning#ACID_DISTANCE} along a fixed bearing (the one the sky draws it on). A body that gets inside
 * {@link SpaceTuning#ACID_CAPTURE_RADIUS} of it, heading toward it, is taken to the acid-cloud dimension at full size, high above the cloud
 * deck and coming in fast and descending; a body that leaves the thick air upward comes back to the Gap just short of the point, scaled
 * again. No rim: a craft that misses coasts on.
 */
public final class AcidRoute {
    private AcidRoute() {
    }

    public static Vector3d direction() {
        double y = Math.toRadians(SpaceTuning.ACID_YAW_DEG), p = Math.toRadians(SpaceTuning.ACID_PITCH_DEG);
        return new Vector3d(Math.cos(p) * Math.sin(y), Math.sin(p), -Math.cos(p) * Math.cos(y));
    }

    /** Where the acid-cloud world is in the Gap. */
    public static Vector3d planet() {
        return direction().mul(SpaceTuning.ACID_DISTANCE).add(0, SpaceTuning.ENTRY_Y, 0);
    }

    /** Inside the capture sphere and closing on the point. */
    public static boolean captured(ServerSubLevel body) {
        Vector3d to = planet().sub(body.logicalPose().position());
        return to.lengthSquared() <= SpaceTuning.ACID_CAPTURE_RADIUS * SpaceTuning.ACID_CAPTURE_RADIUS
                && body.latestLinearVelocity.dot(to) > 0;
    }

    /** Takes {@code body} from the Gap to the acid-cloud world. Bodies moved, as {@link SpaceTravel#cross}. */
    public static int toAcid(ServerLevel gap, ServerSubLevel body) {
        ServerLevel acid = gap.getServer().getLevel(SpaceTravel.ACID);
        if (acid == null)
            return -2;
        double k = BodyScale.of(body);
        Vector3d off = new Vector3d(body.logicalPose().position()).sub(planet()).div(k).mul(SpaceTuning.ACID_OFFSET_SHARE);
        double descent = Math.toRadians(SpaceTuning.ACID_DESCENT_DEG);
        return SpaceTravel.cross(gap, acid, body, new Vector3d(off.x, SpaceTuning.ACID_ARRIVE_Y, off.z), true, 1.0, v -> {
            double speed = v.length();
            double flat = Math.hypot(v.x, v.z);
            return flat < 1e-6 ? new Vector3d(0, -speed, 0)
                    : new Vector3d(v.x / flat * Math.cos(descent), -Math.sin(descent), v.z / flat * Math.cos(descent)).mul(speed);
        });
    }

    /** Takes {@code body} from the acid-cloud world up to the Gap, short of the point on the Overworld's side of it, shrunk again. */
    public static int toGap(ServerLevel acid, ServerSubLevel body) {
        ServerLevel gap = acid.getServer().getLevel(SpaceTravel.GAP);
        if (gap == null)
            return -2;
        Vector3d at = planet().sub(direction().mul(SpaceTuning.ACID_CAPTURE_RADIUS + SpaceTuning.ACID_RETURN_CLEARANCE));
        return SpaceTravel.cross(acid, gap, body, at, true, SpaceTuning.GAP_SCALE, v -> v);
    }
}
