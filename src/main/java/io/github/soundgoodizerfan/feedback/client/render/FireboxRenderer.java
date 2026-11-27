// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.machine.firebox.FireboxBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The piece lying in a firebox's flame, flat in the opening so a bar being reheated can be seen. */
public class FireboxRenderer implements BlockEntityRenderer<FireboxBlockEntity> {

    private final ItemRenderer itemRenderer;

    public FireboxRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(FireboxBlockEntity firebox, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack piece = firebox.getPiece();
        if (piece.isEmpty())
            return;
        pose.pushPose();
        // Lying flat just under the lip of the opening.
        pose.translate(0.5f, 0.8f, 0.5f);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(0.5f, 0.5f, 0.5f);
        itemRenderer.renderStatic(piece, ItemDisplayContext.FIXED, light, overlay, pose, buffers, firebox.getLevel(), 0);
        pose.popPose();
    }
}
