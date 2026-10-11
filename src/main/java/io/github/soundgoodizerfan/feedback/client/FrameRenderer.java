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
package io.github.soundgoodizerfan.feedback.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.vehicle.frame.Frame;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderPicks;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderSelection;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws a welded chassis: plates and tubes in their material's map colour, the frame's edges as
 * lines, plates as
 * one-plate-thick slabs, and -- only while a Welder is held -- each vertex as a 1/16 cube filling its grid cell, the
 * selected one cyan, a carried one red, one chosen for a plate green. Placeholder look, like all the
 * art; the plates are flat-shaded by a fixed light so the shape reads.
 */
public class FrameRenderer implements BlockEntityRenderer<FrameBlockEntity> {

    private static final Vec3 LIGHT = new Vec3(0.4, 1.0, 0.25).normalize();
    private static final float MARK = 1f / 16;

    public FrameRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FrameBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int light, int overlay) {
        Frame frame = be.frame();
        Matrix4f m = poseStack.last().pose();

        VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
        for (Frame.Plate p : frame.plates()) {
            Vec3 a = frame.pos(p.a()), b = frame.pos(p.b()), c = frame.pos(p.c());
            Vec3 n = frame.cross(p).normalize();
            int rgb = p.materialState().getMapColor(be.getLevel(), be.getBlockPos()).col;
            float shade = (float) (0.55 + 0.45 * Math.abs(n.dot(LIGHT)));
            float r = ((rgb >> 16) & 0xff) / 255f * shade, g = ((rgb >> 8) & 0xff) / 255f * shade, bl = (rgb & 0xff) / 255f * shade;
            // A slab one plate-thickness through, centred on the plate's plane.
            Vec3 h = n.scale(Frame.PLATE_THICKNESS / 2.0);
            Vec3 a1 = a.add(h), b1 = b.add(h), c1 = c.add(h), a0 = a.subtract(h), b0 = b.subtract(h), c0 = c.subtract(h);
            face(quads, m, a1, b1, c1, r, g, bl);
            face(quads, m, a0, b0, c0, r * 0.8f, g * 0.8f, bl * 0.8f);
            for (Vec3[] e : new Vec3[][]{{a0, b0, b1, a1}, {b0, c0, c1, b1}, {c0, a0, a1, c1}}) {
                face(quads, m, e[0], e[1], e[2], r * 0.9f, g * 0.9f, bl * 0.9f);
                face(quads, m, e[0], e[2], e[3], r * 0.9f, g * 0.9f, bl * 0.9f);
            }
        }

        for (Map.Entry<Long, Frame.Tube> t : frame.tubes().entrySet()) {
            double half = t.getValue().diameter() / 2;
            Vec3 a = frame.pos(Frame.edgeA(t.getKey())), b = frame.pos(Frame.edgeB(t.getKey()));
            Vec3 d = b.subtract(a).normalize();
            Vec3 u = (Math.abs(d.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(d).normalize().scale(half);
            Vec3 v = d.cross(u);
            int rgb = t.getValue().materialState().getMapColor(be.getLevel(), be.getBlockPos()).col;
            Vec3[] ring = {u.add(v), u.subtract(v), u.scale(-1).subtract(v), v.subtract(u)};
            for (int i = 0; i < 4; i++) {
                Vec3 p = ring[i], q = ring[(i + 1) % 4];
                float shade = (float) (0.55 + 0.45 * Math.abs(p.add(q).normalize().dot(LIGHT)));
                float r = ((rgb >> 16) & 0xff) / 255f * shade, g = ((rgb >> 8) & 0xff) / 255f * shade, bl = (rgb & 0xff) / 255f * shade;
                quad(quads, m, a.add(p), b.add(p), b.add(q), r, g, bl);
                quad(quads, m, a.add(p), b.add(q), a.add(q), r, g, bl);
                quad(quads, m, a.add(p), b.add(q), b.add(p), r, g, bl);
                quad(quads, m, a.add(p), a.add(q), b.add(q), r, g, bl);
            }
        }

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (long key : frame.edgeKeys())
            line(lines, poseStack, frame.pos(Frame.edgeA(key)), frame.pos(Frame.edgeB(key)), 0.15f, 0.15f, 0.15f);

        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;
        ItemStack welder = player.getMainHandItem().is(FVehicles.WELDER.get()) ? player.getMainHandItem()
                : player.getOffhandItem().is(FVehicles.WELDER.get()) ? player.getOffhandItem() : null;
        if (welder == null)
            return;
        WelderSelection sel = welder.get(FVehicles.WELDER_SELECTION.get());
        int held = sel != null && sel.frame().equals(be.getBlockPos()) ? sel.vertex() : -1;
        WelderPicks picks = welder.get(FVehicles.WELDER_PICKS.get());
        // Asking for another RenderType ends the previous builder, so the lines above are finished
        // with: take the quad buffer afresh rather than reuse the stale one.
        VertexConsumer marks = buffers.getBuffer(RenderType.debugQuads());
        for (Map.Entry<Integer, Frame.Vertex> e : frame.vertices().entrySet()) {
            Vec3 p = e.getValue().local();
            boolean isHeld = e.getKey() == held;
            boolean picked = picks != null && picks.frame().equals(be.getBlockPos()) && picks.vertices().contains(e.getKey());
            float r = 1f, g = 0.8f, bl = 0.2f;              // vertex
            if (isHeld && sel.grabbed()) { g = 0.2f; bl = 0.2f; }         // carried: red
            else if (isHeld) { r = 0.2f; g = 0.9f; bl = 1f; }             // selected: cyan
            else if (picked) { r = 0.3f; g = 1f; bl = 0.3f; }             // chosen for a plate: green
            // One 1/16 cell, exactly: the point fills its grid cell, nothing more.
            cube(marks, m, p, MARK / 2 * (isHeld || picked ? 1.15f : 1f), r, g, bl);
        }
    }

    private static void cube(VertexConsumer c, Matrix4f m, Vec3 p, double h, float r, float g, float bl) {
        Vec3[] v = new Vec3[8];
        for (int i = 0; i < 8; i++)
            v[i] = p.add((i & 1) == 0 ? -h : h, (i & 2) == 0 ? -h : h, (i & 4) == 0 ? -h : h);
        int[][] quadsIdx = {{0, 1, 3, 2}, {4, 5, 7, 6}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}};
        for (int[] q : quadsIdx) {
            face(c, m, v[q[0]], v[q[1]], v[q[2]], r, g, bl);
            face(c, m, v[q[0]], v[q[2]], v[q[3]], r, g, bl);
        }
    }

    /** A triangle, drawn from both sides. */
    private static void face(VertexConsumer c, Matrix4f m, Vec3 a, Vec3 b, Vec3 d, float r, float g, float bl) {
        quad(c, m, a, b, d, r, g, bl);
        quad(c, m, a, d, b, r, g, bl);
    }

    private static void quad(VertexConsumer c, Matrix4f m, Vec3 a, Vec3 b, Vec3 d, float r, float g, float bl) {
        c.addVertex(m, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, 1f);
        c.addVertex(m, (float) b.x, (float) b.y, (float) b.z).setColor(r, g, bl, 1f);
        c.addVertex(m, (float) d.x, (float) d.y, (float) d.z).setColor(r, g, bl, 1f);
        c.addVertex(m, (float) d.x, (float) d.y, (float) d.z).setColor(r, g, bl, 1f);
    }

    private static void line(VertexConsumer c, PoseStack pose, Vec3 a, Vec3 b, float r, float g, float bl) {
        Vec3 n = b.subtract(a).normalize();
        PoseStack.Pose last = pose.last();
        c.addVertex(last, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, 1f).setNormal(last, (float) n.x, (float) n.y, (float) n.z);
        c.addVertex(last, (float) b.x, (float) b.y, (float) b.z).setColor(r, g, bl, 1f).setNormal(last, (float) n.x, (float) n.y, (float) n.z);
    }

    @Override
    public AABB getRenderBoundingBox(FrameBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(Frame.RANGE / (double) Frame.GRID + 1);
    }
}
