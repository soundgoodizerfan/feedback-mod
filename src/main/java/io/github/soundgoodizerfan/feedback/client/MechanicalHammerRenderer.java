// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.machine.hammer.MechanicalHammerBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the workpiece lying on the anvil.
 * <p>
 * Not decoration. Beat 1 ships no measuring tool because a part-worked ingot is meant to be
 * readable by eye, and that requires the player be able to see it sitting in the machine rather
 * than having to pull it out to find out what it has become.
 */
public class MechanicalHammerRenderer implements BlockEntityRenderer<MechanicalHammerBlockEntity> {

    /** Top of the anvil slab, which is three pixels tall, plus a hair so it does not z-fight. */
    private static final float ANVIL_SURFACE = 3.05f / 16f;

    private final ItemRenderer itemRenderer;

    public MechanicalHammerRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(MechanicalHammerBlockEntity hammer, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        ItemStack workpiece = hammer.getWorkpiece();
        if (workpiece.isEmpty())
            return;

        pose.pushPose();
        pose.translate(0.5f, ANVIL_SURFACE, 0.5f);
        pose.mulPose(Axis.XP.rotationDegrees(90));   // lay it flat instead of standing it upright
        pose.scale(0.5f, 0.5f, 0.5f);
        itemRenderer.renderStatic(workpiece, ItemDisplayContext.FIXED, light, overlay, pose, buffers,
                hammer.getLevel(), 0);
        pose.popPose();
    }
}
