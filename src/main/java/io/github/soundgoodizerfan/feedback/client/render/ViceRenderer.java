// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.machine.vice.ViceBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The piece closed in a vice's jaws, standing between them so a part-worked piece can be read by eye. */
public class ViceRenderer implements BlockEntityRenderer<ViceBlockEntity> {

    private final ItemRenderer itemRenderer;

    public ViceRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ViceBlockEntity vice, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack piece = vice.getWorkpiece();
        if (piece.isEmpty())
            return;
        pose.pushPose();
        // Between the jaws (z 5 to 8), standing on the base, its face toward the player who stands at the screw end.
        pose.translate(0.5f, 0.62f, 0.4f);
        pose.mulPose(Axis.XP.rotationDegrees(0));
        pose.scale(0.4f, 0.4f, 0.4f);
        itemRenderer.renderStatic(piece, ItemDisplayContext.FIXED, light, overlay, pose, buffers, vice.getLevel(), 0);
        pose.popPose();
    }
}
