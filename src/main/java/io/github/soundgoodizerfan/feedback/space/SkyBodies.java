// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The planets seen from the Overworld at night: points that ride the night's turning like the stars but sit at a spot among them that
 * drifts with the day count, so a planet is up on some nights and not on others. The client draws them from this table and the server
 * checks a sighting against it, so both work out the same direction from the same time. Nothing is stored; the direction is a function
 * of the day time.
 */
public final class SkyBodies {

    /**
     * One planet. {@code ra0} is where it stands in the star frame at day time zero and {@code dec} how far from the sun's track it sits
     * (both degrees); {@code period} is the days it takes to go once round the star frame; {@code minMagnification} is the telescope
     * magnification that resolves its disc.
     */
    public record Body(String id, double ra0, double dec, double period, float minMagnification, int tint) {
    }

    public static final Body ACID_CLOUD = new Body("acid_cloud", 40, -12, 23, 8f, 0xFFD6CC5C);
    public static final Body FROZEN_MOON = new Body("frozen_moon", 200, 17, 11, 16f, 0xFFB9D9F2);
    public static final Body CRYSTAL = new Body("crystal", 315, 6, 37, 12f, 0xFFB584E6);

    /** The End's gas giant, the biggest and slowest wanderer: it resolves at the lowest magnification. */
    public static final Body GAS_GIANT = new Body("gas_giant", 120, -4, 61, 6f, 0xFFE0B27A);

    public static final Body[] ALL = { ACID_CLOUD, FROZEN_MOON, CRYSTAL, GAS_GIANT };

    /** The telescope's blur at or under which a planet's disc shows; above it only a smudge does. */
    public static final float MAX_BLUR = 0.3f;
    /** Degrees, at 1x, inside which a planet counts as in the middle of the view (a third of a 70 degree half-width); the view narrows by the magnification. */
    public static final double AIM_DEG_AT_1X = 11.7;
    /** The server's room over what the client tests, for rounding and latency. */
    public static final double SERVER_SLACK = 1.5;
    /** A planet at or under this height (the sine of its altitude) is below the horizon. */
    public static final double HORIZON = 0.02;

    private SkyBodies() {
    }

    public static Body byId(String id) {
        for (Body b : ALL)
            if (b.id().equals(id))
                return b;
        return null;
    }

    /** The unit direction in the world where {@code body} is now: east is +x, up is +y, south is +z, as the sun and stars turn. */
    public static Vec3 direction(Level level, Body body, float partial) {
        double days = level.getDayTime() / 24000.0;
        double ra = Math.toRadians(body.ra0() + 360.0 * days / body.period() + level.getTimeOfDay(partial) * 360.0);
        double dec = Math.toRadians(body.dec());
        double c = Math.cos(dec);
        return new Vec3(-c * Math.sin(ra), c * Math.cos(ra), Math.sin(dec));
    }

    /** The star-frame angle, degrees, at which a planet is lined up for a cheap transfer; the share of its period either side of that in which the window counts as open. */
    public static final double WINDOW_DEG = 0.0, WINDOW_OPEN_SHARE = 0.04;

    /**
     * Days until {@code body}'s star-frame angle next reaches {@link #WINDOW_DEG}: its transfer window, from its period alone. 0 while it is within
     * {@link #WINDOW_OPEN_SHARE} of a period either side of the alignment (the window is open).
     */
    public static double daysToWindow(Level level, Body body) {
        double days = level.getDayTime() / 24000.0;
        double angle = (body.ra0() + 360.0 * days / body.period()) % 360.0;
        double toGo = ((WINDOW_DEG - angle) % 360.0 + 360.0) % 360.0;
        double share = toGo / 360.0;
        return share <= WINDOW_OPEN_SHARE || share >= 1.0 - WINDOW_OPEN_SHARE ? 0.0 : share * body.period();
    }

    /** True when {@code body} stands above the horizon. */
    public static boolean up(Vec3 direction) {
        return direction.y > HORIZON;
    }

    /** Degrees between a look direction and a planet's direction. */
    public static double offBy(Vec3 look, Vec3 direction) {
        double dot = Math.max(-1, Math.min(1, look.normalize().dot(direction)));
        return Math.toDegrees(Math.acos(dot));
    }

    /** The most a planet may be off the view's middle, degrees, at magnification {@code magnification}. */
    public static double aim(float magnification) {
        return AIM_DEG_AT_1X / Math.max(1f, magnification);
    }
}
