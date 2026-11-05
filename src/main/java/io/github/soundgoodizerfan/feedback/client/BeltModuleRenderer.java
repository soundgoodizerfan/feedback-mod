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

    @Override
    public void render(BeltBlockEntity belt, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        DataNodeRenderer.drawNodes(belt);
        if (belt.module() == null)
            return;
        Direction f = belt.face();
        pose.pushPose();
        pose.translate(0.5 + f.getStepX() * 0.28, 0.62, 0.5 + f.getStepZ() * 0.28);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-f.toYRot()));
        pose.scale(0.45f, 0.45f, 0.45f);
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(FLogistics.MODULE_ITEMS.get(belt.module()).get()),
                ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, belt.getLevel(), 0);
        pose.popPose();
    }
}
