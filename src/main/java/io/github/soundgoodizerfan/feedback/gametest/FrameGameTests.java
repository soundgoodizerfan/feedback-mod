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
package io.github.soundgoodizerfan.feedback.gametest;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.frame.Frame;
import io.github.soundgoodizerfan.feedback.vehicle.frame.Frame.Vertex;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The welded chassis model ({@code feedback_vehicles_spec.md} section 4): plates form only on a
 * closed triangle, cutting an edge takes its plates, and {@code Vdrag} is the angle-weighted
 * silhouette -- a slab beats a wedge, and a closed box is not counted twice.
 */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class FrameGameTests {

    private static final ResourceLocation IRON = ResourceLocation.withDefaultNamespace("iron_block");
    private static final int B = Frame.GRID; // one block, in grid units

    @GameTest(template = "empty")
    public static void plateNeedsAClosedTriangle(GameTestHelper helper) {
        Frame f = new Frame();
        int a = f.add(new Vertex(0, 0, 0)), b = f.add(new Vertex(B, 0, 0)), c = f.add(new Vertex(0, B, 0));
        f.toggleEdge(a, b);
        f.toggleEdge(b, c);
        helper.assertFalse(f.addPlate(a, b, c, IRON), "two edges are not a plate");
        f.toggleEdge(a, c);
        helper.assertTrue(f.addPlate(a, b, c, IRON), "three edges close a plate");
        helper.assertTrue(Math.abs(f.area(f.plates().iterator().next()) - 0.5) < 1e-9, "half-block triangle");
        f.toggleEdge(a, c);
        helper.assertTrue(f.plates().isEmpty(), "cutting an edge takes the plate");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rakedFrontHasLessVdragThanASlab(GameTestHelper helper) {
        float slab = square(new Vertex(0, 0, 0), new Vertex(0, B, 0), new Vertex(0, B, B), new Vertex(0, 0, B))
                .vdrag(new Vec3(1, 0, 0)).raw();
        float raked = square(new Vertex(0, 0, 0), new Vertex(B, B, 0), new Vertex(B, B, B), new Vertex(0, 0, B))
                .vdrag(new Vec3(1, 0, 0)).raw();
        helper.assertTrue(Math.abs(slab - 1f) < 0.02f, "a 1x1 slab facing the air is 1 block², got " + slab);
        helper.assertTrue(Math.abs(raked - (float) Math.sqrt(0.5)) < 0.02f, "a 45° rake is cos45 of the slab, got " + raked);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void closedBoxIsCountedOnce(GameTestHelper helper) {
        Frame f = new Frame();
        int[] v = new int[8];
        for (int i = 0; i < 8; i++)
            v[i] = f.add(new Vertex((i & 1) * B, ((i >> 1) & 1) * B, ((i >> 2) & 1) * B));
        int[][] faces = {{0, 1, 3, 2}, {4, 5, 7, 6}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}};
        for (int[] q : faces)
            plateQuad(f, v[q[0]], v[q[1]], v[q[2]], v[q[3]]);
        float drag = f.vdrag(new Vec3(1, 0, 0)).raw();
        helper.assertTrue(Math.abs(drag - 1f) < 0.02f, "a unit box meets the air with one face, got " + drag);
        helper.succeed();
    }

    private static Frame square(Vertex a, Vertex b, Vertex c, Vertex d) {
        Frame f = new Frame();
        plateQuad(f, f.add(a), f.add(b), f.add(c), f.add(d));
        return f;
    }

    /** Two triangles sharing the a-c diagonal. */
    private static void plateQuad(Frame f, int a, int b, int c, int d) {
        for (int[] e : new int[][]{{a, b}, {b, c}, {c, d}, {d, a}, {a, c}})
            if (!f.hasEdge(e[0], e[1]))
                f.toggleEdge(e[0], e[1]);
        f.addPlate(a, b, c, IRON);
        f.addPlate(a, c, d, IRON);
    }
}
