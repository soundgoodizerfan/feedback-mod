// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.soundgoodizerfan.feedback.combat.tether.GrappleHook;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The hook as a small billboarded copy of the grapple's sprite, and its line from the thrower while
 * it flies. Once it has bitten, the line is the tether's ({@link TetherRopes}).
 */
public class GrappleHookRenderer extends EntityRenderer<GrappleHook> {

    private final ItemStack stack = new ItemStack(FItems.GRAPPLE.get());

    public GrappleHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(GrappleHook entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(GrappleHook entity, float entityYaw, float partial, PoseStack pose, MultiBufferSource buffer, int light) {
        pose.pushPose();
        pose.scale(0.5f, 0.5f, 0.5f);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, pose, buffer, entity.level(), entity.getId());
        pose.popPose();
        Entity owner = entity.getOwner();
        if (owner != null && !entity.anchored()) {
            Vec3 hand = owner.getEyePosition(partial).add(0, -0.45, 0);
            Vec3 here = entity.getPosition(partial);
            pose.pushPose();
            TetherRopes.strip(buffer.getBuffer(RenderType.lineStrip()), pose.last(),
                    (float) (hand.x - here.x), (float) (hand.y - here.y), (float) (hand.z - here.z));
            pose.popPose();
        }
        super.render(entity, entityYaw, partial, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(GrappleHook entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
