// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle;

import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Vector3d;

/** Pushing a Sable body at a world point: the one place an impulse is turned into the body's own frame. */
public final class BodyPush {

    private BodyPush() {
    }

    /** A world impulse at a world point, straight onto {@code body}. */
    public static void impulseAt(ServerSubLevel body, Vector3d at, Vector3d impulse) {
        RigidBodyHandle h = RigidBodyHandle.of(body);
        if (h == null)
            return;
        Pose3dc pose = body.logicalPose();
        ForceTotal one = new ForceTotal();
        one.applyImpulseAtPoint(body.getMassTracker(), pose.transformPositionInverse(at, new Vector3d()),
                pose.transformNormalInverse(impulse, new Vector3d()));
        h.applyForcesAndReset(one);
    }

    /** World velocity of the world point {@code at} on {@code body}: its linear velocity plus the spin about its centre of mass. */
    public static Vector3d pointVelocity(ServerSubLevel body, Vector3d at) {
        RigidBodyHandle h = RigidBodyHandle.of(body);
        if (h == null)
            return new Vector3d();
        return h.getAngularVelocity(new Vector3d()).cross(new Vector3d(at).sub(body.logicalPose().position()), new Vector3d())
                .add(h.getLinearVelocity(new Vector3d()));
    }

    /** Mass the body presents to a push at {@code at} along the unit world direction {@code dir} (spin included). */
    public static double effectiveMass(ServerSubLevel body, Vector3d at, Vector3d dir) {
        Pose3dc pose = body.logicalPose();
        return 1 / body.getMassTracker().getInverseNormalMass(pose.transformPositionInverse(at, new Vector3d()),
                pose.transformNormalInverse(dir, new Vector3d()));
    }
}
