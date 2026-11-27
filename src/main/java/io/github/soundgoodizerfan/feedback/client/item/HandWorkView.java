// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import io.github.soundgoodizerfan.feedback.client.render.GlowColours;
import io.github.soundgoodizerfan.feedback.item.HandWork;
import io.github.soundgoodizerfan.feedback.item.ItemTuning;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * First person: the piece held in a hand tool's {@code HAND_WORK} component is drawn in front of the view while the tool is
 * worked, since the other hand is empty and the piece would otherwise be invisible. It dips on every stroke, timed from the use
 * ticks against {@link ItemTuning#HAND_STROKE_TICKS}, and glows when it is hot ({@link GlowColours#heated}).
 * <p>
 * Prior art: Create's sandpaper (MIT, see THIRD-PARTY-LICENSES.md) shows the polished piece beside the paper while it is used,
 * read off the tool's own data component and bobbed from the remaining use ticks, mirrored by hand. Taken: that idea. Rejected:
 * its item-renderer wrapper (our hammers are modular models), the fixed eat cycle, the flat GUI sprite, the fixed tuck-aside
 * pose, the JEI clock, all assets. Vanilla's draw of the tool is left alone.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class HandWorkView {

    /** Where the piece sits, in camera space: across toward the empty hand, down and forward from the eyes. */
    private static final float ACROSS = 0.12f, DOWN = 0.22f, FORWARD = 0.65f;
    /** Drawn size, and the ground transform's own lift that the offset cancels. */
    private static final float SCALE = 0.7f, GROUND_LIFT = 0.125f;
    /** How far a blow knocks the piece down (blocks) and tilts it (degrees) at the stroke, easing back over the stroke. */
    private static final float KNOCK = 0.05f, TILT_DEG = 6f;
    /** How sharply the knock dies away across a stroke: higher is snappier. */
    private static final float KNOCK_SNAP = 4f;

    private HandWorkView() {
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent event) {
        var player = Minecraft.getInstance().player;
        if (player == null || !player.isUsingItem() || player.getUsedItemHand() != event.getHand())
            return;
        ItemStack tool = event.getItemStack();
        if (!HandWork.working(tool))
            return;
        ItemStack piece = tool.get(FDataComponents.HAND_WORK);
        if (piece == null && tool.has(FDataComponents.HAND_TONGS)) {
            ItemStack tongs = HandWork.tongsBeside(player);
            piece = tongs == null ? null : tongs.get(FDataComponents.TONGS_HELD);
        }
        if (piece == null || piece.isEmpty())
            return;
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == HumanoidArm.RIGHT ? 1f : -1f;
        // The stroke clock: 0 as a blow lands, rising to 1 as the next one does.
        int used = HandWork.useDuration() - player.getUseItemRemainingTicks();
        float phase = ((used % ItemTuning.HAND_STROKE_TICKS) + event.getPartialTick()) / ItemTuning.HAND_STROKE_TICKS;
        float knock = ClientSettings.reduceMotion() ? 0f : (float) Math.pow(1f - Math.min(phase, 1f), KNOCK_SNAP);

        int tint = GlowColours.heated(0xFFFFFF, piece);
        boolean hot = tint != 0xFFFFFF;
        int light = hot ? LightTexture.FULL_BRIGHT : event.getPackedLight();
        MultiBufferSource buffers = hot ? tinted(event.getMultiBufferSource(), tint) : event.getMultiBufferSource();

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-side * ACROSS, -DOWN - KNOCK * knock - GROUND_LIFT * SCALE, -FORWARD);
        pose.mulPose(Axis.XP.rotationDegrees(TILT_DEG * knock));
        pose.scale(SCALE, SCALE, SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(piece, ItemDisplayContext.GROUND, light,
                OverlayTexture.NO_OVERLAY, pose, buffers, player.level(), 0);
        pose.popPose();
    }

    /** {@code buffers} with every vertex colour multiplied by {@code rgb} (0xRRGGBB): the piece's own colours, lit by its heat. */
    private static MultiBufferSource tinted(MultiBufferSource buffers, int rgb) {
        return type -> new Tint(buffers.getBuffer(type), rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF);
    }

    /** Passes everything on, scaling the colour; the default bulk-quad writers all end in {@code setColor}. */
    private record Tint(VertexConsumer out, int r, int g, int b) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            out.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            out.setColor(red * r / 255, green * g / 255, blue * b / 255, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            out.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            out.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            out.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            out.setNormal(x, y, z);
            return this;
        }
    }
}
