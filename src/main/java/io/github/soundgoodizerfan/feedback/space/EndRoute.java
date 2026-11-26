// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

/**
 * The rocket route to the End (docs/specs/feedback_space_spec.md 4.5). The End is a point in the Gap: the Gap's origin point
 * ({@code 0, ENTRY_Y, 0}) plus {@link SpaceTuning#END_DISTANCE} along a fixed bearing. A body that gets inside
 * {@link SpaceTuning#END_CAPTURE_RADIUS} of it, heading toward it, is taken to the End at full size, over the main island, coming in
 * fast and descending; a body that leaves the End's air upward comes back to the Gap just short of the point, scaled again. A craft that
 * misses coasts on until the Gap's rim ({@link GapRim}) brings it to rest.
 */
public final class EndRoute {
    private EndRoute() {
    }

    /** The unit vector a bearing points along: yaw turns about the vertical, pitch 0 is level. The same figure the sky draws the gas giant on. */
    public static Vector3d direction() {
        double y = Math.toRadians(SpaceTuning.END_YAW_DEG), p = Math.toRadians(SpaceTuning.END_PITCH_DEG);
        return new Vector3d(Math.cos(p) * Math.sin(y), Math.sin(p), -Math.cos(p) * Math.cos(y));
    }

    /** Where the End is in the Gap. */
    public static Vector3d planet() {
        return direction().mul(SpaceTuning.END_DISTANCE).add(0, SpaceTuning.ENTRY_Y, 0);
    }

    /** Inside the capture sphere and closing on the point (a craft just returned from the End starts outside it and moving away). */
    public static boolean captured(ServerSubLevel body) {
        Vector3d to = planet().sub(body.logicalPose().position());
        return to.lengthSquared() <= SpaceTuning.END_CAPTURE_RADIUS * SpaceTuning.END_CAPTURE_RADIUS
                && body.latestLinearVelocity.dot(to) > 0;
    }

    /** Takes {@code body} from the Gap to the End. Bodies moved, as {@link SpaceTravel#cross}. */
    public static int toEnd(ServerLevel gap, ServerSubLevel body) {
        ServerLevel end = gap.getServer().getLevel(Level.END);
        if (end == null)
            return -2;
        double k = BodyScale.of(body);
        Vector3d off = new Vector3d(body.logicalPose().position()).sub(planet()).div(k).mul(SpaceTuning.END_OFFSET_SHARE);
        double descent = Math.toRadians(SpaceTuning.END_DESCENT_DEG);
        return SpaceTravel.cross(gap, end, body, new Vector3d(off.x, SpaceTuning.END_ARRIVE_Y, off.z), true, 1.0, v -> {
            double speed = v.length();
            double flat = Math.hypot(v.x, v.z);
            return flat < 1e-6 ? new Vector3d(0, -speed, 0)
                    : new Vector3d(v.x / flat * Math.cos(descent), -Math.sin(descent), v.z / flat * Math.cos(descent)).mul(speed);
        });
    }

    /** Takes {@code body} from the End up to the Gap, short of the point on the Overworld's side of it, shrunk again. */
    public static int toGap(ServerLevel end, ServerSubLevel body) {
        ServerLevel gap = end.getServer().getLevel(SpaceTravel.GAP);
        if (gap == null)
            return -2;
        Vector3d at = planet().sub(direction().mul(SpaceTuning.END_CAPTURE_RADIUS + SpaceTuning.END_RETURN_CLEARANCE));
        return SpaceTravel.cross(end, gap, body, at, true, SpaceTuning.GAP_SCALE, v -> v);
    }
}
