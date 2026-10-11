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

import io.github.soundgoodizerfan.feedback.machine.pulley.PulleyBlock;
import io.github.soundgoodizerfan.feedback.machine.pulley.PulleyBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * A pulley turns like any shaft ({@link RotatingRenderer}), and draws its belt: two straight runs
 * tangent to both pulleys, from whichever end sorts first so each belt is drawn once. Placeholder
 * art -- vanilla black wool -- like everything else here.
 */
public class PulleyRenderer extends RotatingRenderer<PulleyBlockEntity> {

    private static final ResourceLocation BELT = ResourceLocation.withDefaultNamespace("textures/block/black_wool.png");
    private static final double HALF_WIDTH = 2 / 16.0;
    private static final double HALF_THICKNESS = 0.5 / 16.0;

    public PulleyRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PulleyBlockEntity pulley, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        super.render(pulley, partialTick, pose, buffers, light, overlay);

        BlockPos partner = pulley.getBeltPartner();
        BlockPos here = pulley.getBlockPos();
        if (partner == null || here.asLong() > partner.asLong()
                || !(pulley.getBlockState().getBlock() instanceof PulleyBlock block))
            return;

        Vec3 axis = Vec3.atLowerCornerOf(Direction.get(Direction.AxisDirection.POSITIVE,
                block.getRotationAxis(pulley.getBlockState())).getNormal());
        Vec3 delta = Vec3.atLowerCornerOf(partner.subtract(here));
        Vec3 across = axis.cross(delta);
        if (across.lengthSqr() < 1e-6)
            return;   // partner along the axle: no plane for a belt to run in
        across = across.normalize();
        Vec3 centre = new Vec3(0.5, 0.5, 0.5);
        double ownRadius = pulley.getRadius() / 16.0;
        double theirRadius = pulley.getLevel() != null
                && pulley.getLevel().getBlockEntity(partner) instanceof PulleyBlockEntity other
                ? other.getRadius() / 16.0 : ownRadius;

        VertexConsumer consumer = buffers.getBuffer(RenderType.entitySolid(BELT));
        for (int sign = -1; sign <= 1; sign += 2) {
            Vec3 from = centre.add(across.scale(sign * ownRadius));
            Vec3 to = centre.add(delta).add(across.scale(sign * theirRadius));
            strip(pose, consumer, from, to, axis, across, light);
        }
    }

    /** A thin box from {@code a} to {@code b}, {@code width} wide along the axle. */
    private static void strip(PoseStack pose, VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 width, Vec3 normal, int light) {
        Vec3 w = width.scale(HALF_WIDTH);
        Vec3 n = normal.scale(HALF_THICKNESS);
        quad(pose, consumer, a.add(n).subtract(w), a.add(n).add(w), b.add(n).add(w), b.add(n).subtract(w), normal, light);
        quad(pose, consumer, b.subtract(n).subtract(w), b.subtract(n).add(w), a.subtract(n).add(w), a.subtract(n).subtract(w), normal.reverse(), light);
        quad(pose, consumer, a.subtract(n).add(w), b.subtract(n).add(w), b.add(n).add(w), a.add(n).add(w), width, light);
        quad(pose, consumer, a.add(n).subtract(w), b.add(n).subtract(w), b.subtract(n).subtract(w), a.subtract(n).subtract(w), width.reverse(), light);
    }

    private static void quad(PoseStack pose, VertexConsumer consumer, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 normal, int light) {
        PoseStack.Pose last = pose.last();
        vertex(last, consumer, p0, 0, 0, normal, light);
        vertex(last, consumer, p1, 1, 0, normal, light);
        vertex(last, consumer, p2, 1, 1, normal, light);
        vertex(last, consumer, p3, 0, 1, normal, light);
    }

    private static void vertex(PoseStack.Pose last, VertexConsumer consumer, Vec3 p, float u, float v, Vec3 normal, int light) {
        consumer.addVertex(last, (float) p.x, (float) p.y, (float) p.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(last, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    @Override
    public boolean shouldRenderOffScreen(PulleyBlockEntity pulley) {
        return pulley.getBeltPartner() != null;   // a belt is longer than its block
    }
}
