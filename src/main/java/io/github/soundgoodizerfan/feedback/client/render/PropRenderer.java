// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.type.MaterialColors;
import io.github.soundgoodizerfan.feedback.vehicle.air.PropBlock;
import io.github.soundgoodizerfan.feedback.vehicle.air.PropBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.air.PropDesign;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws a propeller's blades. The block model is the hub and shaft; the blades are generated per frame from the
 * baked {@link PropDesign}: {@code blades} thin slabs spaced round the shaft, each a few segments whose
 * angle to the disc follows the pitch (constant pitch, so the angle is steeper near the hub), tinted by the
 * blade metal. The node's visual angle turns the whole set and follows signed RPM, so reverse spin shows.
 *
 * <p>Texel density: the blade texture is a 16 by 16 sprite on its own (non-atlas) texture, which repeats, and every
 * face's UV extent is its world size in blocks (one repeat is 16 texels is one block), faces and edge strips
 * alike. A blade is tiled, never stretched across.
 */
public class PropRenderer implements BlockEntityRenderer<PropBlockEntity> {

    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, "textures/entity/propeller_blade.png");
    private static final float HUB_RADIUS = 0.2f, THICKNESS = 1f / 16f;
    /** Blade angle to the disc plane at the tip for fine, medium and coarse pitch (radians): 12, 22 and 34 degrees. */
    private static final float[] TIP_ANGLE = {0.21f, 0.38f, 0.59f};

    private final Vector3f nrm = new Vector3f();

    public PropRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(PropBlockEntity be) {
        PropDesign d = be.design();
        return d == null ? new net.minecraft.world.phys.AABB(be.getBlockPos())
                : new net.minecraft.world.phys.AABB(be.getBlockPos()).inflate(d.diameter() / 2); // the disc reaches past the block
    }

    @Override
    public void render(PropBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        PropDesign design = be.design();
        if (design == null)
            return;
        Direction facing = be.getBlockState().getValue(PropBlock.FACING);
        boolean positive = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        int rgb = MaterialColors.of(design.material());
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, bl = rgb & 255;
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutout(TEXTURE));

        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        // Spin about the positive axis like every rotation node, then stand the model's +Y on the facing.
        pose.mulPose(positiveAxis(facing.getAxis()).rotationDegrees(be.getVisualAngle(partialTick)));
        pose.mulPose(facing.getRotation());
        Matrix4f m = pose.last().pose();
        // World spin is local spin for a positive facing and the reverse for a negative one; the blade's handedness follows.
        float hand = positive ? 1f : -1f;

        float radius = (float) design.diameter() / 2f;
        float tipAngle = TIP_ANGLE[design.pitch()];
        float chord = 0.12f + 0.04f * (float) design.diameter();
        int segments = design.diameter() > 2.0 ? 3 : 2;
        float span = radius - HUB_RADIUS;
        for (int k = 0; k < design.blades(); k++) {
            double alpha = 2 * Math.PI * k / design.blades();
            float ex = (float) Math.cos(alpha), ez = (float) Math.sin(alpha);
            float tx = -ez, tz = ex;
            for (int s = 0; s < segments; s++) {
                float r0 = HUB_RADIUS + span * s / segments, r1 = HUB_RADIUS + span * (s + 1) / segments;
                segment(out, m, pose, ex, ez, tx, tz, r0, r1, radius, tipAngle, chord, hand, s == segments - 1, r, g, bl, light, overlay);
            }
        }
        pose.popPose();
    }

    /** Blade angle to the disc at a radius: constant pitch, so tan(angle) goes as 1 / r. */
    private static float angleAt(float rad, float radius, float tipAngle) {
        return (float) Math.atan(Math.tan(tipAngle) * radius / Math.max(rad, HUB_RADIUS));
    }

    /**
     * One radial slice of a blade as a slab, in a frame of radial direction {@code e}, chord direction {@code c}
     * and normal {@code n = e x c}: top and bottom faces (u along the chord, v along the length), leading and
     * trailing edge strips (u across the thickness, v along the length) and, on the outer slice, the tip cap.
     * Every UV is a size in blocks, which the repeating 16 texel texture turns into 16 texels per block.
     */
    private void segment(VertexConsumer out, Matrix4f m, PoseStack pose, float ex, float ez, float tx, float tz,
                         float r0, float r1, float radius, float tipAngle, float chord, float hand, boolean tip,
                         int r, int g, int bl, int light, int overlay) {
        float len = r1 - r0, h = chord / 2f, th = THICKNESS, t = th / 2f;
        float angle = angleAt((r0 + r1) / 2f, radius, tipAngle) * hand; // one angle per slice keeps it flat
        float cx = tx * (float) Math.cos(angle), cy = (float) Math.sin(angle), cz = tz * (float) Math.cos(angle);
        nrm.set(ex, 0, ez).cross(cx, cy, cz).normalize();
        float nx = nrm.x, ny = nrm.y, nz = nrm.z;
        Ctx k = new Ctx(out, m, pose, r, g, bl, light, overlay);
        // top face (+n): (r0,-h) (r1,-h) (r1,+h) (r0,+h); the bottom runs the same corners backwards
        for (int side = 0; side < 2; side++) {
            float sg = side == 0 ? 1 : -1;
            float ox = nx * t * sg, oy = ny * t * sg, oz = nz * t * sg;
            float[][] pt = {pt(ex, ez, cx, cy, cz, r0, -h, ox, oy, oz), pt(ex, ez, cx, cy, cz, r1, -h, ox, oy, oz),
                    pt(ex, ez, cx, cy, cz, r1, h, ox, oy, oz), pt(ex, ez, cx, cy, cz, r0, h, ox, oy, oz)};
            float[] u = {0, 0, chord, chord}, v = {0, len, len, 0};
            if (side == 0)
                quad(k, pt, u, v, nx, ny, nz, 0, 1, 2, 3);
            else
                quad(k, pt, u, v, -nx, -ny, -nz, 3, 2, 1, 0);
        }
        // leading edge (-c), seen CCW from -c as (e, n): (r0,-t) (r1,-t) (r1,+t) (r0,+t)
        edge(k, ex, ez, cx, cy, cz, nx, ny, nz, r0, r1, -h, -1, t, th, len);
        // trailing edge (+c): the same strip with the corner order reversed
        edge(k, ex, ez, cx, cy, cz, nx, ny, nz, r0, r1, h, 1, t, th, len);
        if (tip) {
            // outer cap (+e), seen CCW from +e as (c, n): (-h,-t) (+h,-t) (+h,+t) (-h,+t); u along the chord, v across the thickness
            float[][] pt = {cap(ex, ez, cx, cy, cz, nx, ny, nz, r1, -h, -t), cap(ex, ez, cx, cy, cz, nx, ny, nz, r1, h, -t),
                    cap(ex, ez, cx, cy, cz, nx, ny, nz, r1, h, t), cap(ex, ez, cx, cy, cz, nx, ny, nz, r1, -h, t)};
            quad(k, pt, new float[]{0, chord, chord, 0}, new float[]{0, 0, th, th}, ex, 0, ez, 0, 1, 2, 3);
        }
    }

    private void edge(Ctx k, float ex, float ez, float cx, float cy, float cz, float nx, float ny, float nz,
                      float r0, float r1, float chordOffset, int side, float t, float th, float len) {
        float[][] pt = {cap(ex, ez, cx, cy, cz, nx, ny, nz, r0, chordOffset, -t), cap(ex, ez, cx, cy, cz, nx, ny, nz, r1, chordOffset, -t),
                cap(ex, ez, cx, cy, cz, nx, ny, nz, r1, chordOffset, t), cap(ex, ez, cx, cy, cz, nx, ny, nz, r0, chordOffset, t)};
        float[] u = {0, 0, th, th}, v = {0, len, len, 0};
        if (side < 0)
            quad(k, pt, u, v, -cx, -cy, -cz, 0, 1, 2, 3);
        else
            quad(k, pt, u, v, cx, cy, cz, 3, 2, 1, 0);
    }

    private static float[] pt(float ex, float ez, float cx, float cy, float cz, float rad, float along, float ox, float oy, float oz) {
        return new float[]{ex * rad + cx * along + ox, cy * along + oy, ez * rad + cz * along + oz};
    }

    /** A point at radius, chord offset and normal offset. */
    private static float[] cap(float ex, float ez, float cx, float cy, float cz, float nx, float ny, float nz, float rad, float along, float up) {
        return new float[]{ex * rad + cx * along + nx * up, cy * along + ny * up, ez * rad + cz * along + nz * up};
    }

    private record Ctx(VertexConsumer out, Matrix4f m, PoseStack pose, int r, int g, int b, int light, int overlay) {
    }

    private static void quad(Ctx k, float[][] p, float[] u, float[] v, float nx, float ny, float nz, int i0, int i1, int i2, int i3) {
        for (int i : new int[]{i0, i1, i2, i3})
            k.out.addVertex(k.m, p[i][0], p[i][1], p[i][2]).setColor(k.r, k.g, k.b, 255).setUv(u[i], v[i])
                    .setOverlay(k.overlay).setLight(k.light).setNormal(k.pose.last(), nx, ny, nz);
    }

    private static Axis positiveAxis(Direction.Axis axis) {
        return switch (axis) {
            case X -> Axis.XP;
            case Y -> Axis.YP;
            case Z -> Axis.ZP;
        };
    }
}
