// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.machine.ShopMachineBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The machine-shop machines' workpiece, lying on top of the machine -- {@link
 * MechanicalHammerRenderer}'s reason, six more times: a part-worked piece is meant to be readable
 * by eye. Also draws the data-node overlay, since the Lathe is a node and has only this renderer.
 */
public class WorkpieceRenderer<T extends ShopMachineBlockEntity> implements BlockEntityRenderer<T> {

    /** Just above the top face, so it does not z-fight. */
    private static final float SURFACE = 1.01f;

    private final ItemRenderer itemRenderer;

    public WorkpieceRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(T machine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        DataNodeRenderer.drawNodes(machine);
        ItemStack workpiece = machine.getWorkpiece();
        if (workpiece.isEmpty())
            return;
        pose.pushPose();
        pose.translate(0.5f, SURFACE, 0.5f);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(0.5f, 0.5f, 0.5f);
        itemRenderer.renderStatic(workpiece, ItemDisplayContext.FIXED, light, overlay, pose, buffers,
                machine.getLevel(), 0);
        pose.popPose();
    }
}
