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
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderMode;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderCorner;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderSelected;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderSelection;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;
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
 * Draws a welded chassis: plates, tubes and tires in their material's block texture, the frame's edges as
 * lines, plates as
 * one-plate-thick slabs, and -- only while a Welder is held -- each vertex as a 1/16 cube filling its grid cell, the
 * selected one cyan, a carried one red, one chosen for a plate green, a bearing larger and
 * coloured by kind (also while a Pivot Setter is held). Welds to the base always show: a bead at
 * the point and a rod to the block's face. Placeholder look, like all the
 * art; the textured shapes take the entity shader's light from their normals so the shape reads.
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

        // Plates, tubes and tires wear their material's block texture, wrapped round the shape; the
        // atlas type is asked for first, as asking for another RenderType ends the previous builder.
        VertexConsumer tex = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        for (Frame.Plate p : frame.plates()) {
            PoseStack.Pose pose = groups.plate(p).last();
            TextureAtlasSprite sprite = sprite(p.materialState());
            Vec3 a = frame.pos(p.a()), b = frame.pos(p.b()), c = frame.pos(p.c());
            Vec3 n = frame.cross(p).normalize();
            // A slab one plate-thickness through, centred on the plate's plane.
            Vec3 h = n.scale(Frame.PLATE_THICKNESS / 2.0);
            Vec3 a1 = a.add(h), b1 = b.add(h), c1 = c.add(h), a0 = a.subtract(h), b0 = b.subtract(h), c0 = c.subtract(h);
            Vec3 mid = a.add(b).add(c).scale(1 / 3.0);
            plateFace(tex, pose, sprite, light, n, a1, b1, c1);
            plateFace(tex, pose, sprite, light, n.scale(-1), a0, c0, b0);
            for (Vec3[] e : new Vec3[][]{{a0, b0, b1, a1}, {b0, c0, c1, b1}, {c0, a0, a1, c1}}) {
                Vec3 out = e[1].subtract(e[0]).cross(n).normalize();
                if (out.dot(e[0].add(e[1]).scale(0.5).subtract(mid)) < 0)
                    out = out.scale(-1);
                double len = Math.min(1, e[1].subtract(e[0]).length());
                texQuad(tex, pose, light, out, e[0], sprite, 0, 0, e[1], sprite, len, 0, e[2], sprite, len, Frame.PLATE_THICKNESS, e[3], sprite, 0, Frame.PLATE_THICKNESS);
            }
        }

        // A tube the drive chain turns, turns: the spin comes from the server (FrameSpin), since it
        // is computed and never a body's. A square section shows it.
        double time = be.getLevel() == null ? 0 : be.getLevel().getGameTime() + partialTick;
        for (Map.Entry<Long, Frame.Tube> t : frame.tubes().entrySet())
            texRod(tex, groups.edge(t.getKey()).last(), sprite(t.getValue().materialState()), light,
                    frame.pos(Frame.edgeA(t.getKey())), frame.pos(Frame.edgeB(t.getKey())), t.getValue().diameter() / 2,
                    be.spin().tube(t.getKey(), time), 0);

        // A tire: a ring about its hub's axle, turning at the hub's spin. Each segment shows its own
        // slice of the texture, so the turning can be seen without a marked segment.
        for (Map.Entry<Integer, Frame.Tire> t : frame.tires().entrySet()) {
            Frame.Pivot hub = frame.pivots().get(t.getKey());
            if (hub == null)
                continue;
            PoseStack.Pose pose = groups.vertex(t.getKey()).last();
            Vec3 c = frame.pos(t.getKey()), a = hub.axis().normalize();
            Vec3 u = (Math.abs(a.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(a).normalize();
            Vec3 v = a.cross(u);
            double r = t.getValue().radius(), tread = r / 2;
            TextureAtlasSprite sprite = sprite(t.getValue().materialState());
            int sides = 16;
            double turn = be.spin().hub(t.getKey(), time);
            for (int i = 0; i < sides; i++) {
                double p = 2 * Math.PI * i / sides + turn, q = 2 * Math.PI * (i + 1) / sides + turn;
                Vec3 from = c.add(u.scale(Math.cos(p) * r)).add(v.scale(Math.sin(p) * r));
                Vec3 to = c.add(u.scale(Math.cos(q) * r)).add(v.scale(Math.sin(q) * r));
                texRod(tex, pose, sprite, light, from, to, tread / 2, 0, i / (double) sides);
            }
        }

        VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
        // Welds to the base are physical, so always drawn: a bead at the point and a short rod to
        // the nearest face of the block it is welded to (docs/specs/feedback_vehicles_spec.md section 4).
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
        WelderSelected chosen = welder == null ? null : WelderItem.selected(welder, be);
        java.util.Set<Integer> selected = chosen == null ? java.util.Set.of() : new java.util.HashSet<>(chosen.vertices());
        WelderMode mode = welder == null ? null : welder.getOrDefault(FVehicles.WELDER_MODE.get(), WelderMode.POINT);
        Vec3 origin = Vec3.atLowerCornerOf(be.getBlockPos());
        int looked = aim != null && aim.be() == be
                ? WelderItem.pick(frame, origin, aim.eye(), aim.look(), player.blockInteractionRange()) : -1;

        // The gizmo's drag, as it will land: the selection moved (or its copies, extruding).
        FrameGizmo.Arrow drag = FrameGizmo.dragging(be.getBlockPos());
        Vec3 shift = drag == null ? Vec3.ZERO : drag.dir().scale(drag.sign() * FrameGizmo.steps() / (double) Frame.GRID);

        VertexConsumer lines2 = buffers.getBuffer(RenderType.lines());
        // Mirror planes through the middle of the frame block, as wide as the frame.
        if (frame.mirror() != 0)
            mirrorPlanes(lines2, poseStack, frame);
        // Alignment: every point sharing two coordinates with the point looked at, or with where
        // the dragged selection is going, joined to it by a line in its axis's colour.
        java.util.List<Frame.Vertex> refs = new java.util.ArrayList<>();
        if (looked >= 0)
            refs.add(frame.vertices().get(looked));
        if (drag != null)
            for (int v : selected)
                refs.add(Frame.Vertex.snap(frame.pos(v).add(shift)));
        for (Frame.Vertex r : refs)
            for (Frame.Vertex o : frame.vertices().values()) {
                int same = (r.x() == o.x() ? 1 : 0) + (r.y() == o.y() ? 1 : 0) + (r.z() == o.z() ? 1 : 0);
                if (same != 2)
                    continue;
                float cr = r.x() != o.x() ? 1f : 0.2f, cg = r.y() != o.y() ? 1f : 0.2f, cb = r.z() != o.z() ? 1f : 0.2f;
                line(lines2, poseStack, r.local(), o.local(), cr, cg, cb);
            }
        // Select mode's box, from the first corner to where the second would go.
        WelderCorner corner = welder == null ? null : welder.get(FVehicles.WELDER_CORNER.get());
        if (corner != null && corner.frame().equals(be.getBlockPos()) && mode == WelderMode.SELECT && aim != null && aim.be() == be) {
            Frame.Vertex to = Frame.Vertex.snap(WelderItem.target(be.getLevel(), player, frame, origin, aim.eye(), aim.look(),
                    player.blockInteractionRange(), chosen != null ? chosen.last() : -1).subtract(origin));
            double g = Frame.GRID;
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                    Math.min(corner.x(), to.x()) / g, Math.min(corner.y(), to.y()) / g, Math.min(corner.z(), to.z()) / g,
                    (Math.max(corner.x(), to.x()) + 1) / g, (Math.max(corner.y(), to.y()) + 1) / g, (Math.max(corner.z(), to.z()) + 1) / g);
            net.minecraft.client.renderer.LevelRenderer.renderLineBox(poseStack, lines2, box, 0.4f, 1f, 1f, 1f);
        }
        if (drag != null && mode == WelderMode.EXTRUDE)
            for (int v : selected)
                line(lines2, poseStack, frame.pos(v), frame.pos(v).add(shift), 1f, 1f, 1f);

        // Asking for another RenderType ends the previous builder, so the lines above are finished
        // with: take the quad buffer afresh rather than reuse the stale one.
        VertexConsumer marks = buffers.getBuffer(RenderType.debugQuads());
        for (Map.Entry<Integer, Frame.Vertex> e : frame.vertices().entrySet()) {
            Vec3 p = e.getValue().local();
            boolean isHeld = e.getKey() == held;
            boolean isSelected = selected.contains(e.getKey());
            boolean picked = picks != null && picks.frame().equals(be.getBlockPos()) && picks.vertices().contains(e.getKey());
            float r = 1f, g = 0.8f, bl = 0.2f;              // vertex
            boolean welded = frame.anchors().containsKey(e.getKey());
            if (welded) { r = 0.15f; g = 0.3f; bl = 1f; }                   // welded to a block: blue
            Frame.Pivot pivot = frame.pivots().get(e.getKey());
            if (pivot != null)                                            // a bearing, by kind
                switch (pivot.kind()) {
                    case HINGE -> { r = 1f; g = 0.2f; bl = 1f; }          // hinge: magenta
                    case BALL -> { r = 1f; g = 0.5f; bl = 0f; }           // ball: orange
                    case SLIDER -> { r = 0.2f; g = 1f; bl = 0.8f; }       // slider (a spring): teal
                    default -> { r = 0.8f; g = 0.8f; bl = 0.8f; }         // locked: grey
                }
            if (isHeld || isSelected) { r = 0.2f; g = 0.9f; bl = 1f; }    // selected: cyan, over everything
            else if (picked) { r = 0.3f; g = 1f; bl = 0.3f; }             // chosen for a plate: green
            // One 1/16 cell, exactly: the point fills its grid cell, nothing more. A bearing is drawn
            // larger so it reads from across the chassis.
            cube(marks, groups.vertex(e.getKey()).last().pose(), p, MARK / 2 * (isHeld || isSelected || picked ? 1.15f : pivot != null || welded ? 1.6f : 1f), r, g, bl);
            if (isSelected && drag != null)                               // where it will land
                cube(marks, groups.vertex(e.getKey()).last().pose(), p.add(shift), MARK / 2, 1f, 1f, 1f);
        }

        // The gizmo: six arrows on the selection's middle, x red, y green, z blue; the one under
        // the look (or being dragged) bright.
        if (chosen != null && FrameGizmo.selection(player, be) != null) {
            Vec3 c = FrameGizmo.centre(frame, chosen).add(shift);
            FrameGizmo.Arrow over = drag != null ? drag
                    : aim != null && aim.be() == be ? FrameGizmo.hovered(c, aim.eye().subtract(origin), aim.look()) : null;
            for (int axis = 0; axis < 3; axis++)
                for (int sign = -1; sign <= 1; sign += 2) {
                    FrameGizmo.Arrow a = new FrameGizmo.Arrow(axis, sign);
                    boolean hot = a.equals(over);
                    int rgb = switch (axis) {
                        case 0 -> hot ? 0xFF8080 : 0xC03030;
                        case 1 -> hot ? 0x80FF80 : 0x30A030;
                        default -> hot ? 0x8080FF : 0x3050D0;
                    };
                    rod(marks, poseStack.last().pose(), c.add(a.dir().scale(FrameGizmo.FROM)), c.add(a.dir().scale(FrameGizmo.TO)),
                            hot ? 0.035 : 0.022, rgb, 0);
                    cube(marks, poseStack.last().pose(), c.add(a.dir().scale(FrameGizmo.TO)), hot ? 0.06 : 0.045,
                            ((rgb >> 16) & 0xff) / 255f, ((rgb >> 8) & 0xff) / 255f, (rgb & 0xff) / 255f);
                }
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

    private static TextureAtlasSprite sprite(BlockState state) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state);
    }

    /** A point of the sprite: {@code fu} and {@code fv} are fractions of its width and height. */
    private static void vertex(VertexConsumer c, PoseStack.Pose pose, Vec3 p, TextureAtlasSprite s, double fu, double fv, int light, Vec3 n) {
        c.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(0xFFFFFFFF)
                .setUv(s.getU0() + (s.getU1() - s.getU0()) * (float) Math.clamp(fu, 0, 1), s.getV0() + (s.getV1() - s.getV0()) * (float) Math.clamp(fv, 0, 1))
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }

    private static void texQuad(VertexConsumer c, PoseStack.Pose pose, int light, Vec3 n,
                                Vec3 p0, TextureAtlasSprite s0, double u0, double v0, Vec3 p1, TextureAtlasSprite s1, double u1, double v1,
                                Vec3 p2, TextureAtlasSprite s2, double u2, double v2, Vec3 p3, TextureAtlasSprite s3, double u3, double v3) {
        vertex(c, pose, p0, s0, u0, v0, light, n);
        vertex(c, pose, p1, s1, u1, v1, light, n);
        vertex(c, pose, p2, s2, u2, v2, light, n);
        vertex(c, pose, p3, s3, u3, v3, light, n);
    }

    /** One face of a plate: the sprite stretched over the triangle's box in its own plane. */
    private static void plateFace(VertexConsumer c, PoseStack.Pose pose, TextureAtlasSprite s, int light, Vec3 n, Vec3 a, Vec3 b, Vec3 d) {
        Vec3 ex = b.subtract(a).normalize(), ey = n.cross(ex);
        Vec3[] p = {a, b, d};
        double[] x = new double[3], y = new double[3];
        double x0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, y0 = Double.MAX_VALUE, y1 = -Double.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            x[i] = p[i].subtract(a).dot(ex);
            y[i] = p[i].subtract(a).dot(ey);
            x0 = Math.min(x0, x[i]); x1 = Math.max(x1, x[i]);
            y0 = Math.min(y0, y[i]); y1 = Math.max(y1, y[i]);
        }
        double w = Math.max(1e-6, x1 - x0), h = Math.max(1e-6, y1 - y0);
        double[] fu = new double[3], fv = new double[3];
        for (int i = 0; i < 3; i++) {
            fu[i] = (x[i] - x0) / w;
            fv[i] = (y[i] - y0) / h;
        }
        texQuad(c, pose, light, n, a, s, fu[0], fv[0], b, s, fu[1], fv[1], d, s, fu[2], fv[2], d, s, fu[2], fv[2]);
    }

    /**
     * A square rod from {@code a} to {@code b}, {@code half} across from its axis, turned {@code angle}
     * radians about its own axis, the block texture on its four sides: a block of length is one
     * sprite, repeated by pieces, and {@code phase} (0 to 1) slides where in the sprite a piece
     * starts, so the pieces of a ring differ.
     */
    private static void texRod(VertexConsumer c, PoseStack.Pose pose, TextureAtlasSprite s, int light, Vec3 a, Vec3 b, double half, double angle, double phase) {
        Vec3 d = b.subtract(a).normalize();
        Vec3 u0 = (Math.abs(d.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(d).normalize().scale(half);
        Vec3 v0 = d.cross(u0);
        double cs = Math.cos(angle), sn = Math.sin(angle);
        Vec3 u = u0.scale(cs).add(v0.scale(sn)), v = v0.scale(cs).subtract(u0.scale(sn));
        Vec3[] ring = {u.add(v), u.subtract(v), u.scale(-1).subtract(v), v.subtract(u)};
        double length = b.subtract(a).length(), across = Math.min(1, 2 * half * Math.sqrt(2));
        int pieces = Math.max(1, (int) Math.ceil(length));
        for (int k = 0; k < pieces; k++) {
            Vec3 from = a.add(d.scale(length * k / pieces)), to = a.add(d.scale(length * (k + 1) / pieces));
            double span = Math.min(1, length / pieces), start = phase * (1 - span);
            for (int i = 0; i < 4; i++) {
                Vec3 p = ring[i], q = ring[(i + 1) % 4];
                Vec3 n = p.add(q).normalize();
                double vs = (i * 0.25 + phase * 0.25) % (1 - across + 1e-9);
                texQuad(c, pose, light, n, from.add(p), s, start, vs, to.add(p), s, start + span, vs,
                        to.add(q), s, start + span, vs + across, from.add(q), s, start, vs + across);
            }
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

    /** Each mirror plane that is on, as a square through the middle of the frame block, as wide as the frame reaches. */
    private static void mirrorPlanes(VertexConsumer lines, PoseStack pose, Frame frame) {
        double lo = -1, hi = 2; // at least a block either side
        for (Frame.Vertex v : frame.vertices().values()) {
            Vec3 p = v.local();
            lo = Math.min(lo, Math.min(p.x, Math.min(p.y, p.z)) - 0.5);
            hi = Math.max(hi, Math.max(p.x, Math.max(p.y, p.z)) + 0.5);
        }
        for (int axis = 0; axis < 3; axis++) {
            if ((frame.mirror() & (1 << axis)) == 0)
                continue;
            Vec3[] c = new Vec3[4];
            double[][] uv = {{lo, lo}, {hi, lo}, {hi, hi}, {lo, hi}};
            for (int k = 0; k < 4; k++) {
                double u = uv[k][0], w = uv[k][1];
                c[k] = switch (axis) {
                    case 0 -> new Vec3(0.5, u, w);
                    case 1 -> new Vec3(u, 0.5, w);
                    default -> new Vec3(u, w, 0.5);
                };
            }
            for (int k = 0; k < 4; k++)
                line(lines, pose, c[k], c[(k + 1) % 4], 1f, 0.4f, 1f);
            line(lines, pose, c[0], c[2], 1f, 0.4f, 1f);
            line(lines, pose, c[1], c[3], 1f, 0.4f, 1f);
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
