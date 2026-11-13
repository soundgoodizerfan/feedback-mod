// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import io.github.soundgoodizerfan.feedback.item.DataConnectorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Renders every {@link DataNode} a block entity hosts -- itself, if it is one, or one per
 * attached sided fitting if it is a {@link Fittable} -- but only while the player holds the data
 * connector. See {@link DeferredDataRenderer} for why this makes them visible through walls.
 */
public class DataNodeRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    private static final int NODE_COLOUR = 0xFF66CCFF;
    private static final double LINE_THICKNESS = 0.03125;

    public DataNodeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        drawNodes(entity);
    }

    /**
     * Queues {@code entity}'s nodes and links. Static so a block entity that already has a
     * renderer of its own (a clutch, a lathe) can call it from there -- one renderer per type.
     */
    public static void drawNodes(BlockEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof DataConnectorItem))
            return;

        if (entity instanceof DataNode node)
            drawNodeAndLinks(node);
        if (entity instanceof Fittable fittable)
            for (Direction side : Direction.values())
                if (fittable.getSidedFitting(side) instanceof DataNode node)
                    drawNodeAndLinks(node);
    }

    private static void drawNodeAndLinks(DataNode node) {
        DeferredDataRenderer renderer = DeferredDataRenderer.get();
        BlockPos pos = node.getNodePos();
        AABB box = node.getNodeBox();

        renderer.deferDraw((pose, consumer) -> {
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            renderer.drawColouredBox(pose.last().pose(), consumer, box, NODE_COLOUR, 0.8f);
            pose.popPose();
        });

        DataNodeRef self = node.getNodeRef();
        Vec3 from = Vec3.atLowerCornerOf(pos).add(box.getCenter());
        for (DataNodeRef other : node.getNodeLinks()) {
            // Each link is stored on both ends; draw it from whichever end sorts first so it is
            // queued exactly once even when both ends are loaded and rendering this frame.
            if (self.compareTo(other) >= 0)
                continue;
            Vec3 to = Vec3.atLowerCornerOf(other.pos()).add(DataNode.boxFor(other.side()).getCenter());
            renderer.deferDraw((pose, consumer) -> drawLine(renderer, pose, consumer, from, to));
        }
    }

    private static void drawLine(DeferredDataRenderer renderer, PoseStack pose, VertexConsumer consumer,
                                 Vec3 from, Vec3 to) {
        drawLine(renderer, pose, consumer, from, to, NODE_COLOUR);
    }

    /** A line of any colour between two world points, for the connector's rubber-band wire too. */
    public static void drawLine(DeferredDataRenderer renderer, PoseStack pose, VertexConsumer consumer,
                         Vec3 from, Vec3 to, int colour) {
        pose.pushPose();
        pose.translate(from.x, from.y, from.z);
        Vec3 delta = to.subtract(from);
        double yaw = Math.atan2(-delta.z, delta.x) + Math.PI;
        double pitch = Math.atan2(delta.horizontalDistance(), delta.y) + Mth.HALF_PI;
        pose.mulPose(Axis.YP.rotation((float) yaw));
        pose.mulPose(Axis.ZP.rotation((float) pitch));
        Matrix4f matrix = pose.last().pose();
        AABB box = new AABB(0, -LINE_THICKNESS, -LINE_THICKNESS, delta.length(), LINE_THICKNESS, LINE_THICKNESS);
        renderer.drawColouredBox(matrix, consumer, box, colour, 0.8f);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(T entity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
