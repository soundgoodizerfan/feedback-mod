// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;


import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * The gun item's renderer: draws {@link GunDraw}'s shape in every display context. In the hand it is
 * placed by the grip and moved by {@link GunMotion}; in a slot, a frame or on the ground it is fitted
 * to the square. Built every frame, since a gun's shape is its parts' and no fixed model can be.
 */
public final class GunRenderer extends BlockEntityWithoutLevelRenderer {

    /** Blocks per model pixel: a gun is drawn at item scale in first person, a little larger in third, as Scorched Guns' display transforms do. */
    static final float FIRST_PERSON_SCALE = 1f / 16f;
    private static final float THIRD_PERSON_SCALE = 1.3f / 16f;

    /**
     * Where the grip sits from the hand's pivot in first person, pixels, right hand ({@code x} mirrors
     * for the left): a little toward the middle of the screen and raised, as Scorched Guns places its
     * guns (its first-person translation applied to where its grips are).
     */
    static float[] firstPersonGrip(GunView view) {
        return view.longGun() ? new float[]{-5.5f, 2.5f, 7.75f} : new float[]{-4.75f, 3.5f, 6f};
    }

    /** The aimed eye point ({@link GunView#eye}) from the hand's pivot in first person, blocks. */
    static float[] firstPersonEye(GunView view, float side) {
        float[] grip = firstPersonGrip(view), at = view.hand(), eye = view.eye();
        return new float[]{(grip[0] * side + eye[0] - at[0]) / 16f, (grip[1] + eye[1] - at[1]) / 16f, (grip[2] + eye[2] - at[2]) / 16f};
    }

    /** Draw a gun held in first person from the hand's pivot: placed by its grip, kicked and tilted by {@link GunMotion}. */
    static void renderHeld(ItemStack stack, GunView view, GunMotion.Hand hand, float side, PoseStack pose,
                           MultiBufferSource buffers, int light) {
        long now = System.currentTimeMillis();
        float[] grip = firstPersonGrip(view), at = view.hand();
        pose.pushPose();
        // The arms, from the hand pivot: the support hand only when nothing else is held in the other hand.
        var player = Minecraft.getInstance().player;
        if (player != null && io.github.soundgoodizerfan.feedback.client.ClientSettings.gunArms())
            GunArms.draw(pose, buffers, light, view, side, player.getOffhandItem().isEmpty() && player.getMainHandItem() == stack);
        pose.translate(grip[0] * side / 16f, grip[1] / 16f, grip[2] / 16f);
        pose.scale(FIRST_PERSON_SCALE, FIRST_PERSON_SCALE, FIRST_PERSON_SCALE);
        pose.translate(-at[0], -at[1], -at[2]);
        GunMotion.item(pose, view, hand, now);
        var mesh = new GunMesh(pose, outputs(buffers, stack), light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        mesh.sighted = hand == GunMotion.hand(false) && io.github.soundgoodizerfan.feedback.client.GunClient.aim() > 0.5f;
        GunDraw.draw(mesh, view, GunMotion.parts(view, hand, now));
        pose.popPose();
    }

    /** A consumer per texture: the gun sheet on the block atlas, or a wood image. */
    static java.util.function.Function<net.minecraft.resources.ResourceLocation, com.mojang.blaze3d.vertex.VertexConsumer> outputs(
            MultiBufferSource buffers, ItemStack stack) {
        boolean foil = stack.hasFoil();
        return image -> image == GunMesh.GLASS ? buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS))
                : ItemRenderer.getFoilBufferDirect(buffers,
                        RenderType.entityCutoutNoCull(image == null ? InventoryMenu.BLOCK_ATLAS : image), true, foil);
    }

    /** The grip in third person: in the fist, a touch forward. */
    private static final float[] THIRD_PERSON_GRIP = {0f, -1.65f, 3.1f};

    private static GunRenderer instance;

    private GunRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /** Lets the gun item draw through this renderer, and holds a long gun two-handed in third person. */
    public static final class Extension implements IClientItemExtensions {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (instance == null)
                instance = new GunRenderer();
            return instance;
        }

        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            return GunView.of(stack).longGun() ? HumanoidModel.ArmPose.CROSSBOW_HOLD : null;
        }
    }

    /** Whether the stack is a long gun, which is held in two hands. */
    public static boolean longGun(ItemStack stack) {
        return GunView.of(stack).longGun();
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        GunView view = GunView.of(stack);
        long now = System.currentTimeMillis();
        GunMotion.Hand hand = null;
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        float[] at = view.hand();
        switch (context) {
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                // Our own hand pass (GunMotion.onHand) draws held guns; this is for anything else that asks.
                var player = Minecraft.getInstance().player;
                HumanoidArm arm = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
                boolean off = player != null && arm != player.getMainArm();
                renderHeld(stack, view, GunMotion.hand(off), arm == HumanoidArm.RIGHT ? 1f : -1f, pose, buffers, light);
                pose.popPose();
                return;
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                pose.translate(THIRD_PERSON_GRIP[0] / 16f, THIRD_PERSON_GRIP[1] / 16f, THIRD_PERSON_GRIP[2] / 16f);
                pose.scale(THIRD_PERSON_SCALE, THIRD_PERSON_SCALE, THIRD_PERSON_SCALE);
                pose.translate(-at[0], -at[1], -at[2]);
            }
            default -> fit(pose, view, context);
        }
        var mesh = new GunMesh(pose, outputs(buffers, stack), light, overlay);
        GunDraw.draw(mesh, view, GunMotion.parts(view, hand, now));
        pose.popPose();
    }

    /** Fit the whole gun into the item's square: side on, muzzle to the right, long guns laid on the diagonal. */
    private static void fit(PoseStack pose, GunView view, ItemDisplayContext context) {
        float front = -(view.muzzle() + view.muzzleReach()), back = view.rear();
        float top = view.top() + (view.has("S") ? 1.75f : 0.5f), bottom = view.bottom();
        float length = back - front, height = top - bottom;
        boolean flat = context == ItemDisplayContext.GUI || context == ItemDisplayContext.FIXED;
        float tilt = flat && view.longGun() ? 30f : flat ? 8f : 0f;
        // A diagonal holds about 1.3 of the square's side for a slim gun.
        float room = flat ? (view.longGun() ? 1.25f : 0.92f) : context == ItemDisplayContext.GROUND ? 0.75f : 0.7f;
        float s = room / Math.max(length, height * 1.6f);
        if (flat)
            pose.mulPose(Axis.ZP.rotationDegrees(tilt));
        pose.mulPose(Axis.YP.rotationDegrees(-90));
        pose.scale(s, s, s);
        pose.translate(0, -(top + bottom) / 2, -(front + back) / 2);
    }
}
