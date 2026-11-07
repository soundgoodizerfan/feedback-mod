// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws a rotation node the slow way, for players who have turned Flywheel's backend off.
 *
 * <h2>Why this has to exist</h2>
 * A spinning block keeps itself out of the chunk mesh, because otherwise the mesh would draw a
 * motionless copy underneath {@link RotatingVisual}'s turning one. That makes the block entity the
 * <em>only</em> thing that draws it — so with the backend off and no renderer here, every shaft in
 * the world would simply disappear. This is not a nicety; it is the other half of leaving the mesh.
 *
 * <p>One draw call per block, which is exactly the cost Flywheel exists to avoid. That is the
 * bargain: correctness without the backend, speed with it.
 */
public class RotatingRenderer<T extends RotationNode> implements BlockEntityRenderer<T> {

    /** Any fixed seed will do; block models are not meant to vary between frames. */
    private static final RandomSource MODEL_RANDOM = RandomSource.create(42L);

    private final BlockRenderDispatcher blocks;

    public RotatingRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(T node, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        DataNodeRenderer.drawNodes(node);   // a clutch is a node; nothing else draws its overlay

        // The visual has it in hand whenever the backend is running. Drawing here too would
        // double every shaft, which is the same bug as forgetting RenderShape.INVISIBLE.
        if (VisualizationManager.supportsVisualization(node.getLevel()))
            return;

        BlockState state = node.getBlockState();
        Direction.Axis axis = ((Rotatable) state.getBlock()).getRotationAxis(state);
        BakedModel model = blocks.getBlockModel(state);

        pose.pushPose();
        // Spin about the block's centre, not its corner.
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.mulPose(positiveAxis(axis).rotationDegrees(node.getVisualAngle(partialTick)));
        pose.translate(-0.5f, -0.5f, -0.5f);

        for (RenderType type : model.getRenderTypes(state, MODEL_RANDOM, ModelData.EMPTY)) {
            // The chunk render type says how to shade the quads; the buffer has to be its entity
            // equivalent, because we are drawing in the block entity pass and not into a chunk.
            blocks.getModelRenderer().renderModel(pose.last(),
                    buffers.getBuffer(RenderTypeHelper.getEntityRenderType(type, false)),
                    state, model, 1f, 1f, 1f, light, overlay, ModelData.EMPTY, type);
        }

        pose.popPose();
    }

    private static Axis positiveAxis(Direction.Axis axis) {
        return switch (axis) {
            case X -> Axis.XP;
            case Y -> Axis.YP;
            case Z -> Axis.ZP;
        };
    }
}
