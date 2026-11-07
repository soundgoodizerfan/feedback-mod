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
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.machine.pivot.PivotMastBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** The pivot arm: a long thin beam from the mast to its length, turned to the arm's angle, and a block at the tip for the seated tip. Drawn, not built. */
public class PivotMastRenderer implements BlockEntityRenderer<PivotMastBlockEntity> {

    private static final ResourceLocation BEAM = Feedback.id("textures/block/industrial_metal_duct_sides_1.png");

    public PivotMastRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(PivotMastBlockEntity mast) {
        return true;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(PivotMastBlockEntity mast) {
        int r = Math.max(1, mast.armLength()) + 1;
        return new net.minecraft.world.phys.AABB(mast.getBlockPos()).inflate(r, 1, r);
    }

    @Override
    public void render(PivotMastBlockEntity mast, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        DataNodeRenderer.drawNodes(mast);
        int len = mast.armLength();
        if (len == 0)
            return;
        float angle = mast.armAngle(partialTick);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        // The cell maths puts angle 0 along +x and counts toward +z, which is a clockwise turn seen from above.
        pose.mulPose(Axis.YP.rotationDegrees(-angle));
        var vc = buffers.getBuffer(RenderType.entityCutoutNoCull(BEAM));
        BeltDraw.box(pose, vc, 0f, -1 / 16f, -1 / 16f, len + 0.5f, 1 / 16f, 1 / 16f, light);
        if (mast.tipKind() != null)
            BeltDraw.box(pose, vc, len - 0.2f, -3 / 16f, -3 / 16f, len + 0.5f, 3 / 16f, 3 / 16f, light);
        pose.popPose();
    }
}
