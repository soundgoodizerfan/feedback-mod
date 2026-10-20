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
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Mirror planes through the middle of the frame block, on x, y or z ({@link Frame#mirror()}). With
 * one or more on, the Welder and the Pivot Setter make every edit at each mirror image of the
 * points it touches too: a car is built half and appears whole (author, 2026-10-16, Main
 * Assembly's way of building). Two planes give three images, three give seven.
 *
 * <p>A point sits mid-cell, so the plane through the block's middle maps cell {@code c} to
 * {@code 15 - c}; a block offset {@code o} maps to {@code -o}. A point on a plane is its own image.
 */
public final class Symmetry {

    /** One reflection: which axes it flips, bit 0 x, 1 y, 2 z. */
    public record Image(int flips) {

        public Frame.Vertex apply(Frame.Vertex v) {
            int g = Frame.GRID - 1;
            return new Frame.Vertex((flips & 1) != 0 ? g - v.x() : v.x(), (flips & 2) != 0 ? g - v.y() : v.y(),
                    (flips & 4) != 0 ? g - v.z() : v.z());
        }

        public BlockPos apply(BlockPos o) {
            return new BlockPos((flips & 1) != 0 ? -o.getX() : o.getX(), (flips & 2) != 0 ? -o.getY() : o.getY(),
                    (flips & 4) != 0 ? -o.getZ() : o.getZ());
        }

        /** A direction (a hinge's axis, a move): flipped on the mirrored axes. */
        public Vec3 apply(Vec3 d) {
            return new Vec3((flips & 1) != 0 ? -d.x : d.x, (flips & 2) != 0 ? -d.y : d.y, (flips & 4) != 0 ? -d.z : d.z);
        }

        /** A move by whole cells along each axis. */
        public int[] apply(int dx, int dy, int dz) {
            return new int[]{(flips & 1) != 0 ? -dx : dx, (flips & 2) != 0 ? -dy : dy, (flips & 4) != 0 ? -dz : dz};
        }

        /**
         * The vertex at the image of vertex {@code id}: the one there, or with {@code make} a new one;
         * -1 if none, and {@code id} itself if the point lies on the plane.
         */
        public int of(Frame frame, int id, boolean make) {
            Frame.Vertex v = frame.vertices().get(id);
            if (v == null)
                return -1;
            Frame.Vertex m = apply(v);
            if (m.equals(v))
                return id;
            int there = frame.idAt(m);
            return there >= 0 || !make ? there : frame.add(m);
        }
    }

    private Symmetry() {
    }

    /**
     * Makes each image of point {@code v} what {@code v} is: there, welded to the mirrored block (or
     * not), the same bearing about the mirrored axis (or none), the same tire (or none).
     */
    public static void copyPoint(Frame frame, int v) {
        for (Image img : images(frame)) {
            int m = img.of(frame, v, true);
            if (m < 0 || m == v)
                continue;
            BlockPos off = frame.anchors().get(v);
            frame.setAnchor(m, off == null ? null : img.apply(off));
            Frame.Pivot p = frame.pivots().get(v);
            frame.setPivot(m, p == null ? null : p.withAxis(img.apply(p.axis())));
            Frame.Tire t = frame.tires().get(v);
            if (t != null)
                frame.setTire(m, t);
        }
    }

    /** Removes every image of point {@code v} -- call before removing {@code v} itself. */
    public static void removeImages(Frame frame, int v) {
        List<Integer> gone = new ArrayList<>();
        for (Image img : images(frame)) {
            int m = img.of(frame, v, false);
            if (m >= 0 && m != v)
                gone.add(m);
        }
        gone.forEach(frame::remove);
    }

    /**
     * Makes each image of edge {@code a-b} match it: there or not, a tube of the same material and
     * size or bare; and its ends' bearings. Returns how many tubes the mirror newly made, for the
     * Welder to charge.
     */
    public static int copyEdge(Frame frame, int a, int b) {
        int made = 0;
        Frame.Tube tube = frame.tube(a, b);
        for (Image img : images(frame)) {
            int ma = img.of(frame, a, true), mb = img.of(frame, b, true);
            if (ma < 0 || mb < 0 || ma == mb || (Math.min(ma, mb) == Math.min(a, b) && Math.max(ma, mb) == Math.max(a, b)))
                continue;
            if (frame.hasEdge(a, b) != frame.hasEdge(ma, mb))
                frame.toggleEdge(ma, mb);
            Frame.Tube had = frame.tube(ma, mb);
            if (tube == null) {
                if (had != null)
                    frame.removeTube(ma, mb);
            } else if (frame.hasEdge(ma, mb)) {
                if (had == null || !had.material().equals(tube.material())) {
                    frame.setTube(ma, mb, tube.material());
                    made++;
                }
                for (int i = 0; i < 4 && frame.tube(ma, mb).size() != tube.size(); i++)
                    frame.thicken(ma, mb);
            }
        }
        copyPoint(frame, a);
        copyPoint(frame, b);
        return made;
    }

    /** Makes each image of triangle {@code a-b-c} match it: plated in the same block, or not. Returns plates newly made. */
    public static int copyPlate(Frame frame, int a, int b, int c) {
        int made = 0;
        Frame.Plate plate = frame.hasPlate(a, b, c) ? frame.plate(a, b, c) : null;
        for (Image img : images(frame)) {
            int ma = img.of(frame, a, plate != null), mb = img.of(frame, b, plate != null), mc = img.of(frame, c, plate != null);
            if (ma < 0 || mb < 0 || mc < 0 || ma == mb || mb == mc || ma == mc)
                continue;
            if (plate == null) {
                frame.removePlate(ma, mb, mc);
                continue;
            }
            for (int[] e : new int[][]{{ma, mb}, {mb, mc}, {ma, mc}})
                if (!frame.hasEdge(e[0], e[1]))
                    frame.toggleEdge(e[0], e[1]);
            Frame.Plate had = frame.hasPlate(ma, mb, mc) ? frame.plate(ma, mb, mc) : null;
            if (had == null || !had.material().equals(plate.material())) {
                frame.removePlate(ma, mb, mc);
                if (frame.addPlate(ma, mb, mc, plate.material()) && had == null)
                    made++;
            }
        }
        return made;
    }

    /** Splits each image of edge {@code a-b} as {@code a-b} was split. */
    public static void copySplit(Frame frame, int a, int b) {
        for (Image img : images(frame)) {
            int ma = img.of(frame, a, false), mb = img.of(frame, b, false);
            if (ma >= 0 && mb >= 0 && frame.hasEdge(ma, mb)
                    && !(Math.min(ma, mb) == Math.min(a, b) && Math.max(ma, mb) == Math.max(a, b)))
                frame.split(ma, mb);
        }
    }

    /** Every image but the identity, for the planes that are on. */
    public static List<Image> images(Frame frame) {
        List<Image> out = new ArrayList<>();
        int m = frame.mirror();
        for (int f = 1; f < 8; f++)
            if ((f & ~m) == 0)
                out.add(new Image(f));
        return out;
    }
}
