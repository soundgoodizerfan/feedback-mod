// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.logistics.belt.BeltHeadBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** The head roller, turning with the shaft's RPM (and backwards when it reverses), and the module fitted on the head. */
public class BeltHeadRenderer implements BlockEntityRenderer<BeltHeadBlockEntity> {

    private static final ResourceLocation ROLLER = Feedback.id("textures/block/industrial_metal_duct_sides_1.png");

    public BeltHeadRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Quarter turns from the north-facing model to {@code facing}, as the blockstate files turn it. */
    static int turns(Direction facing) {
        return (facing.get2DDataValue() + 2) % 4;
    }

    @Override
    public void render(BeltHeadBlockEntity head, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        DataNodeRenderer.drawNodes(head);
        Direction facing = head.getBlockState().getValue(io.github.soundgoodizerfan.feedback.logistics.belt.BeltHeadBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-90f * turns(facing)));
        pose.translate(-0.5, 0, -0.5);
        float cy = 3 / 16f, cz = 3 / 16f;
        pose.pushPose();
        pose.translate(0, cy, cz);
        pose.mulPose(Axis.XP.rotationDegrees(-head.getVisualAngle(partialTick)));
        pose.translate(0, -cy, -cz);
        BeltDraw.roller(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(ROLLER)), 0f, 1f, cy, cz, 3 / 16f, 8, light);
        pose.popPose();
        pose.popPose();
        BeltModuleRenderer.drawModule(head, partialTick, pose, buffers, light);
    }
}
