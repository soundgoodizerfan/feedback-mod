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

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The part of a frame that lies inside its own block, as 1/16 voxels: what the block's outline and
 * collision are, so the box you target is the plate you see rather than a cube around it. Plates
 * and tubes mark every voxel whose centre is within half a voxel (a tube: its radius) of them;
 * a weld point marks its own cell. The same voxel-and-merge scheme as {@link ChassisCellBlock}.
 *
 * <p>Geometry beyond the block has no block of its own to carry a hitbox, so it has none until the
 * frame is assembled into cells ({@link FrameAssembly}).
 */
final class FrameShape {

    private static final double HALF = 0.5 / Frame.GRID;

    private FrameShape() {
    }

    /** The in-block voxels of {@code frame}; the full block if there are none, so it stays clickable. */
    static VoxelShape of(Frame frame) {
        int g = Frame.GRID;
        boolean[][][] on = new boolean[g][g][g];
        boolean any = false;

        for (Frame.Vertex v : frame.vertices().values())
            if (inside(v.x(), g) && inside(v.y(), g) && inside(v.z(), g)) {
                on[v.x()][v.y()][v.z()] = true;
                any = true;
            }
        for (Frame.Plate p : frame.plates())
            any |= mark(on, frame.pos(p.a()), frame.pos(p.b()), frame.pos(p.c()), HALF);
        for (var t : frame.tubes().entrySet())
            any |= mark(on, frame.pos(Frame.edgeA(t.getKey())), frame.pos(Frame.edgeB(t.getKey())), null,
                    Math.max(HALF, t.getValue().diameter() / 2));

        return any ? ChassisCellBlock.merge(on) : Shapes.block();
    }

    private static boolean inside(int i, int g) {
        return i >= 0 && i < g;
    }

    /** Marks voxels within {@code r} of triangle abc, or of segment ab if {@code c} is null. */
    private static boolean mark(boolean[][][] on, Vec3 a, Vec3 b, Vec3 c, double r) {
        int g = Frame.GRID;
        Vec3 lo = c == null ? min(a, b) : min(min(a, b), c), hi = c == null ? max(a, b) : max(max(a, b), c);
        int[] from = new int[3], to = new int[3];
        double[] l = {lo.x, lo.y, lo.z}, h = {hi.x, hi.y, hi.z};
        for (int i = 0; i < 3; i++) {
            from[i] = Math.max(0, (int) Math.floor((l[i] - r) * g));
            to[i] = Math.min(g - 1, (int) Math.floor((h[i] + r) * g));
            if (from[i] > to[i])
                return false;
        }
        boolean any = false;
        double r2 = r * r + 1e-9;
        for (int x = from[0]; x <= to[0]; x++)
            for (int y = from[1]; y <= to[1]; y++)
                for (int z = from[2]; z <= to[2]; z++) {
                    Vec3 p = new Vec3((x + 0.5) / g, (y + 0.5) / g, (z + 0.5) / g);
                    Vec3 q = c == null ? onSegment(p, a, b) : onTriangle(p, a, b, c);
                    if (q.distanceToSqr(p) <= r2) {
                        on[x][y][z] = true;
                        any = true;
                    }
                }
        return any;
    }

    private static Vec3 min(Vec3 a, Vec3 b) {
        return new Vec3(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));
    }

    private static Vec3 max(Vec3 a, Vec3 b) {
        return new Vec3(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    private static Vec3 onSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        double t = len2 < 1e-12 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
        return a.add(ab.scale(t));
    }

    /** Closest point on triangle abc to p (Ericson, Real-Time Collision Detection 5.1.5). */
    private static Vec3 onTriangle(Vec3 p, Vec3 a, Vec3 b, Vec3 c) {
        Vec3 ab = b.subtract(a), ac = c.subtract(a), ap = p.subtract(a);
        double d1 = ab.dot(ap), d2 = ac.dot(ap);
        if (d1 <= 0 && d2 <= 0)
            return a;
        Vec3 bp = p.subtract(b);
        double d3 = ab.dot(bp), d4 = ac.dot(bp);
        if (d3 >= 0 && d4 <= d3)
            return b;
        double vc = d1 * d4 - d3 * d2;
        if (vc <= 0 && d1 >= 0 && d3 <= 0)
            return a.add(ab.scale(d1 / (d1 - d3)));
        Vec3 cp = p.subtract(c);
        double d5 = ab.dot(cp), d6 = ac.dot(cp);
        if (d6 >= 0 && d5 <= d6)
            return c;
        double vb = d5 * d2 - d1 * d6;
        if (vb <= 0 && d2 >= 0 && d6 <= 0)
            return a.add(ac.scale(d2 / (d2 - d6)));
        double va = d3 * d6 - d5 * d4;
        if (va <= 0 && d4 - d3 >= 0 && d5 - d6 >= 0)
            return b.add(c.subtract(b).scale((d4 - d3) / ((d4 - d3) + (d5 - d6))));
        double denom = 1.0 / (va + vb + vc);
        return a.add(ab.scale(vb * denom)).add(ac.scale(vc * denom));
    }
}
