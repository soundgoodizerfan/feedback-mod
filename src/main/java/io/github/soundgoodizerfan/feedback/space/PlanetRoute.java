// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import java.util.List;
import java.util.Optional;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

/**
 * The rocket route to a destination planet that is a dimension of its own (the acid-cloud world, the frozen moon, the crystal world), built as
 * {@link EndRoute} is: the planet is a point in the Gap, the Gap's origin point plus {@code distance} along a fixed bearing (the one the sky draws it on). A
 * body that gets inside {@code capture} of it, heading toward it, is taken to the planet's dimension at full size, high above it, coming in fast and
 * descending; a body that leaves the planet's air upward comes back to the Gap just short of the point, scaled again. No rim: a craft that misses coasts on.
 */
public record PlanetRoute(String id, ResourceKey<Level> dimension, float yawDeg, float pitchDeg, double distance, double capture, double arriveY,
                          double offsetShare, double descentDeg, double returnClearance) {

    public static final PlanetRoute ACID_CLOUD = new PlanetRoute("acid_cloud", key("acid_cloud"), 205f, 18f, 650, 80, 300, 0.1, 14, 12);
    public static final PlanetRoute FROZEN_MOON = new PlanetRoute("frozen_moon", key("frozen_moon"), 112f, 27f, 700, 70, 300, 0.1, 12, 12);
    public static final PlanetRoute CRYSTAL = new PlanetRoute("crystal", key("crystal"), 305f, 41f, 800, 90, 300, 0.1, 12, 12);

    public static final List<PlanetRoute> ALL = List.of(ACID_CLOUD, FROZEN_MOON, CRYSTAL);

    private static ResourceKey<Level> key(String path) {
        return ResourceKey.create(Registries.DIMENSION, Feedback.id(path));
    }

    /** The route whose dimension is {@code dimension}, if it is one. */
    public static Optional<PlanetRoute> in(ResourceKey<Level> dimension) {
        return ALL.stream().filter(p -> p.dimension().equals(dimension)).findFirst();
    }

    public Vector3d direction() {
        double y = Math.toRadians(yawDeg), p = Math.toRadians(pitchDeg);
        return new Vector3d(Math.cos(p) * Math.sin(y), Math.sin(p), -Math.cos(p) * Math.cos(y));
    }

    /** Where the planet is in the Gap. */
    public Vector3d planet() {
        return direction().mul(distance).add(0, SpaceTuning.ENTRY_Y, 0);
    }

    /** Inside the capture sphere and closing on the point. */
    public boolean captured(ServerSubLevel body) {
        Vector3d to = planet().sub(body.logicalPose().position());
        return to.lengthSquared() <= capture * capture && body.latestLinearVelocity.dot(to) > 0;
    }

    /** Takes {@code body} from the Gap to the planet. Bodies moved, as {@link SpaceTravel#cross}. */
    public int toPlanet(ServerLevel gap, ServerSubLevel body) {
        ServerLevel target = gap.getServer().getLevel(dimension);
        if (target == null)
            return -2;
        double k = BodyScale.of(body);
        Vector3d off = new Vector3d(body.logicalPose().position()).sub(planet()).div(k).mul(offsetShare);
        double descent = Math.toRadians(descentDeg);
        return SpaceTravel.cross(gap, target, body, new Vector3d(off.x, arriveY, off.z), true, 1.0, v -> {
            double speed = v.length();
            double flat = Math.hypot(v.x, v.z);
            return flat < 1e-6 ? new Vector3d(0, -speed, 0)
                    : new Vector3d(v.x / flat * Math.cos(descent), -Math.sin(descent), v.z / flat * Math.cos(descent)).mul(speed);
        });
    }

    /** Takes {@code body} from the planet up to the Gap, short of the point on the Overworld's side of it, shrunk again. */
    public int toGap(ServerLevel from, ServerSubLevel body) {
        ServerLevel gap = from.getServer().getLevel(SpaceTravel.GAP);
        if (gap == null)
            return -2;
        Vector3d at = planet().sub(direction().mul(capture + returnClearance));
        return SpaceTravel.cross(from, gap, body, at, true, SpaceTuning.GAP_SCALE, v -> v);
    }
}
