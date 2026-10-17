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
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderItem;
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
import org.joml.Matrix3f;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameAssembly;
import java.util.HashMap;

/**
 * Draws a welded chassis: plates and tubes in their material's map colour, the frame's edges as
 * lines, plates as
 * one-plate-thick slabs, and -- only while a Welder is held -- each vertex as a 1/16 cube filling its grid cell, the
 * selected one cyan, a carried one red, one chosen for a plate green, a bearing larger and
 * coloured by kind (also while a Pivot Setter is held). Welds to the base always show: a bead at
 * the point and a rod to the block's face. Placeholder look, like all the
 * art; the plates are flat-shaded by a fixed light so the shape reads.
 */
public class FrameRenderer implements BlockEntityRenderer<FrameBlockEntity> {

    private static final Vec3 LIGHT = new Vec3(0.4, 1.0, 0.25).normalize();
    private static final float MARK = 1f / 16;
    /** Weld bead and rod: dark, slightly warm steel. */
    private static final int WELD = 0x6b625a;

    public FrameRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FrameBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int light, int overlay) {
        Frame frame = be.frame();
        Matrix4f m = poseStack.last().pose();
        Groups groups = new Groups(be, poseStack, partialTick);

        VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
        for (Frame.Plate p : frame.plates()) {
            m = groups.plate(p).last().pose();
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

        // A tube the drive chain turns, turns: the spin comes from the server (FrameSpin), since it
        // is computed and never a body's. A square section shows it.
        double time = be.getLevel() == null ? 0 : be.getLevel().getGameTime() + partialTick;
        for (Map.Entry<Long, Frame.Tube> t : frame.tubes().entrySet())
            rod(quads, groups.edge(t.getKey()).last().pose(), frame.pos(Frame.edgeA(t.getKey())), frame.pos(Frame.edgeB(t.getKey())), t.getValue().diameter() / 2,
                    t.getValue().materialState().getMapColor(be.getLevel(), be.getBlockPos()).col, be.spin().tube(t.getKey(), time));

        // A tire: a ring about its hub's axle, in its material's colour, turning at the hub's spin.
        // One darker segment, so the turning can be seen at all on a ring.
        for (Map.Entry<Integer, Frame.Tire> t : frame.tires().entrySet()) {
            Frame.Pivot hub = frame.pivots().get(t.getKey());
            if (hub == null)
                continue;
            m = groups.vertex(t.getKey()).last().pose();
            Vec3 c = frame.pos(t.getKey()), a = hub.axis().normalize();
            Vec3 u = (Math.abs(a.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(a).normalize();
            Vec3 v = a.cross(u);
            double r = t.getValue().radius(), tread = r / 2;
            int rgb = t.getValue().materialState().getMapColor(be.getLevel(), be.getBlockPos()).col;
            int sides = 16;
            double turn = be.spin().hub(t.getKey(), time);
            int dark = ((rgb >> 1) & 0x7f7f7f);
            for (int i = 0; i < sides; i++) {
                double p = 2 * Math.PI * i / sides + turn, q = 2 * Math.PI * (i + 1) / sides + turn;
                Vec3 from = c.add(u.scale(Math.cos(p) * r)).add(v.scale(Math.sin(p) * r));
                Vec3 to = c.add(u.scale(Math.cos(q) * r)).add(v.scale(Math.sin(q) * r));
                rod(quads, m, from, to, tread / 2, i == 0 ? dark : rgb, 0);
            }
        }

        // Welds to the base are physical, so always drawn: a bead at the point and a short rod to
        // the nearest face of the block it is welded to (feedback_vehicles_spec.md section 4).
        for (Map.Entry<Integer, net.minecraft.core.BlockPos> w : frame.anchors().entrySet()) {
            if (!frame.vertices().containsKey(w.getKey()))
                continue;
            m = groups.vertex(w.getKey()).last().pose();
            Vec3 at = frame.pos(w.getKey());
            net.minecraft.core.BlockPos off = w.getValue();
            Vec3 face = new Vec3(Math.clamp(at.x, off.getX(), off.getX() + 1), Math.clamp(at.y, off.getY(), off.getY() + 1),
                    Math.clamp(at.z, off.getZ(), off.getZ() + 1));
            if (face.distanceToSqr(at) > 1e-6)
                rod(quads, m, at, face, MARK / 2, WELD, 0);
            float r = ((WELD >> 16) & 0xff) / 255f, g = ((WELD >> 8) & 0xff) / 255f, bl = (WELD & 0xff) / 255f;
            cube(quads, m, at, MARK, r, g, bl);
        }

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (long key : frame.edgeKeys())
            line(lines, groups.edge(key), frame.pos(Frame.edgeA(key)), frame.pos(Frame.edgeB(key)), 0.15f, 0.15f, 0.15f);

        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;
        ItemStack welder = player.getMainHandItem().is(FVehicles.WELDER.get()) ? player.getMainHandItem()
                : player.getOffhandItem().is(FVehicles.WELDER.get()) ? player.getOffhandItem() : null;
        boolean setter = player.getMainHandItem().is(FVehicles.PIVOT_SETTER.get())
                || player.getOffhandItem().is(FVehicles.PIVOT_SETTER.get());
        if (welder == null && !setter)
            return;
        // The point under the crosshair, if welded: a plain box round the block it is welded to.
        WelderItem.Aim aim = WelderItem.aim(be.getLevel(), player);
        if (aim != null && aim.be() == be) {
            int looked = WelderItem.pick(frame, Vec3.atLowerCornerOf(be.getBlockPos()), aim.eye(), aim.look(),
                    player.blockInteractionRange());
            net.minecraft.core.BlockPos to = looked < 0 ? null : frame.anchors().get(looked);
            if (to != null)
                net.minecraft.client.renderer.LevelRenderer.renderLineBox(poseStack, buffers.getBuffer(RenderType.lines()),
                        new net.minecraft.world.phys.AABB(to).inflate(0.002), 0.3f, 0.5f, 1f, 1f);
        }
        WelderSelection sel = welder == null ? null : welder.get(FVehicles.WELDER_SELECTION.get());
        int held = sel != null && sel.frame().equals(be.getBlockPos()) ? sel.vertex() : -1;
        WelderPicks picks = welder == null ? null : welder.get(FVehicles.WELDER_PICKS.get());
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
            boolean welded = frame.anchors().containsKey(e.getKey());
            if (welded && !isHeld && !picked) { r = 0.15f; g = 0.3f; bl = 1f; } // welded to a block: blue
            Frame.Pivot pivot = frame.pivots().get(e.getKey());
            if (pivot != null)                                            // a bearing, by kind
                switch (pivot.kind()) {
                    case HINGE -> { r = 1f; g = 0.2f; bl = 1f; }          // hinge: magenta
                    case BALL -> { r = 1f; g = 0.5f; bl = 0f; }           // ball: orange
                    default -> { r = 0.8f; g = 0.8f; bl = 0.8f; }         // locked: grey
                }
            // One 1/16 cell, exactly: the point fills its grid cell, nothing more. A bearing is drawn
            // larger so it reads from across the chassis.
            cube(marks, groups.vertex(e.getKey()).last().pose(), p, MARK / 2 * (isHeld || picked ? 1.15f : pivot != null || welded ? 1.6f : 1f), r, g, bl);
        }
    }

    /**
     * Where each group of an assembled frame is drawn. A group lifted into a body of its own
     * (spec section 4, <em>cells, again</em>) moves with that body, so its plates are drawn
     * through it: frame point {@code p} sits at {@code origin + p} in that body's plot, which its
     * pose takes into the world, which this frame's body's pose takes back into this block's plot.
     * Everything else is drawn where the frame is.
     */
    private static final class Groups {
        private final PoseStack here;
        private final Map<Integer, PoseStack> byGroup = new HashMap<>();
        private final Map<Frame.Plate, Integer> plateGroup = new HashMap<>();
        private final Map<Long, Integer> edgeGroup = new HashMap<>();
        private final FrameBlockEntity be;

        Groups(FrameBlockEntity be, PoseStack here, float partialTick) {
            this.be = be;
            this.here = here;
            if (be.groupBodies().isEmpty() || be.getLevel() == null)
                return;
            var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(be.getLevel());
            var own = dev.ryanhcode.sable.companion.SableCompanion.INSTANCE.getContaining(be);
            if (container == null || !(own instanceof dev.ryanhcode.sable.sublevel.ClientSubLevel mine))
                return;
            Matrix4d back = mine.renderPose().bakeIntoMatrix(new Matrix4d()).invert();
            be.groupBodies().forEach((group, body) -> {
                if (!(container.getSubLevel(body.body()) instanceof dev.ryanhcode.sable.sublevel.ClientSubLevel theirs))
                    return;
                var pos = be.getBlockPos();
                Matrix4d rel = new Matrix4d().translation(-pos.getX(), -pos.getY(), -pos.getZ()).mul(back)
                        .mul(theirs.renderPose().bakeIntoMatrix(new Matrix4d()))
                        .translate(body.origin().getX(), body.origin().getY(), body.origin().getZ());
                Matrix4f f = new Matrix4f(rel);
                PoseStack ps = new PoseStack();
                ps.last().pose().set(here.last().pose()).mul(f);
                ps.last().normal().set(here.last().normal()).mul(new Matrix3f(f));
                byGroup.put(group, ps);
            });
            for (FrameAssembly.Part part : be.cut()) {
                part.plates().forEach(p -> plateGroup.put(p, part.id()));
                part.edges().forEach(e -> edgeGroup.putIfAbsent(e, part.id()));
            }
        }

        PoseStack plate(Frame.Plate p) {
            return of(plateGroup.get(p));
        }

        PoseStack edge(long key) {
            return of(edgeGroup.get(key));
        }

        PoseStack vertex(int v) {
            return byGroup.isEmpty() ? here : of(be.groupOf(v));
        }

        private PoseStack of(Integer group) {
            return group == null ? here : byGroup.getOrDefault(group, here);
        }
    }

    /**
     * A square rod from {@code a} to {@code b}, {@code half} across from its axis, flat-shaded in
     * {@code rgb}, turned {@code angle} radians about its own axis.
     */
    private static void rod(VertexConsumer quads, Matrix4f m, Vec3 a, Vec3 b, double half, int rgb, double angle) {
        Vec3 d = b.subtract(a).normalize();
        Vec3 u0 = (Math.abs(d.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(d).normalize().scale(half);
        Vec3 v0 = d.cross(u0);
        double c = Math.cos(angle), sn = Math.sin(angle);
        Vec3 u = u0.scale(c).add(v0.scale(sn)), v = v0.scale(c).subtract(u0.scale(sn));
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
