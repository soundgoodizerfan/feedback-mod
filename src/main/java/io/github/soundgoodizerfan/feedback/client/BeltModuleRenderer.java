// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.soundgoodizerfan.feedback.logistics.belt.BeltBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FLogistics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the module fitted into a belt cell as its item, turned toward the side it acts on, and the data-link marks. */
public class BeltModuleRenderer implements BlockEntityRenderer<BeltBlockEntity> {

    public BeltModuleRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static final net.minecraft.resources.ResourceLocation CLEAT = io.github.soundgoodizerfan.feedback.Feedback.id("textures/block/belt_cleat.png");
    /** Cleats ride the band at this spacing (blocks). */
    private static final float CLEAT_SPACING = 0.5f;
    /** Top of the band at each quarter of a cell from its tail, per slope, in sixteenths (the model's steps). */
    private static final float[][] TOPS = {{4, 4, 4, 4}, {8, 12, 16, 20}, {0, -4, -8, -12}};

    @Override
    public void render(BeltBlockEntity belt, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        DataNodeRenderer.drawNodes(belt);
        if (belt.spec().cleat())
            drawCleats(belt, partialTick, pose, buffers, light);
        drawModule(belt, partialTick, pose, buffers, light);
    }

    /** Cleat bars sliding along the band with the run: positions are one global comb along the travel axis, so cells line up. */
    private void drawCleats(BeltBlockEntity belt, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        net.minecraft.core.BlockPos bp = belt.getBlockPos();
        net.minecraft.world.level.block.state.BlockState st = belt.getBlockState();
        Direction f = st.getValue(io.github.soundgoodizerfan.feedback.logistics.belt.BeltBlock.FACING);
        float[] tops = TOPS[st.getValue(io.github.soundgoodizerfan.feedback.logistics.belt.BeltBlock.SLOPE).ordinal()];
        double t0 = f.getStepX() + f.getStepZ() > 0 ? bp.getX() * Math.abs(f.getStepX()) + bp.getZ() * Math.abs(f.getStepZ())
                : -((bp.getX() + 1) * Math.abs(f.getStepX()) + (bp.getZ() + 1) * Math.abs(f.getStepZ()));
        double off = ClientBeltRuns.cleatPhase(bp, partialTick);
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90f * BeltHeadRenderer.turns(f)));
        pose.translate(-0.5, 0, -0.5);
        com.mojang.blaze3d.vertex.VertexConsumer vc = buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(CLEAT));
        for (double t = Math.ceil((t0 - off) / CLEAT_SPACING) * CLEAT_SPACING + off; t < t0 + 1; t += CLEAT_SPACING) {
            float g = (float) (t - t0);
            float top = tops[Math.min(3, (int) (g * 4))] / 16f;
            float z = 1f - g;
            BeltDraw.box(pose, vc, 1 / 16f, top, z - 1 / 32f, 15 / 16f, top + 2 / 16f, z + 1 / 32f, light);
        }
        pose.popPose();
    }

    /** The fitted module as its item, turned toward the side it acts on. */
    static void drawModule(io.github.soundgoodizerfan.feedback.logistics.belt.ModuleHost belt, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (belt.module() == null)
            return;
        Direction f = belt.face();
        pose.pushPose();
        pose.translate(0.5 + f.getStepX() * 0.28, 0.62, 0.5 + f.getStepZ() * 0.28);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-f.toYRot()));
        pose.scale(0.45f, 0.45f, 0.45f);
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(FLogistics.MODULE_ITEMS.get(belt.module()).get()),
                ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, ((net.minecraft.world.level.block.entity.BlockEntity) belt).getLevel(), 0);
        pose.popPose();
    }
}
