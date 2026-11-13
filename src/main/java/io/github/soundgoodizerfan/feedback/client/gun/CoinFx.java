// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.net.CoinFlipPayload;
import io.github.soundgoodizerfan.feedback.weapon.GunFuTuning;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The flipped coin (skill {@code coin_flip}), drawn on the client only: a small square of shiny metal, tinted by the coin's material,
 * spinning end over end on the server's arc ({@link io.github.soundgoodizerfan.feedback.weapon.CoinShot}). It is never an entity and decides nothing.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class CoinFx {

    private record Coin(Vec3 origin, long born, int rgb) {
    }

    private static final Map<Integer, Coin> COINS = new HashMap<>();
    /** Half the coin's side, in blocks; and how fast it turns end over end, in radians a tick. */
    private static final float HALF = 0.1f, SPIN = 1.35f;
    private static final ResourceLocation METAL = ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");

    private CoinFx() {
    }

    public static void on(CoinFlipPayload p) {
        if (p.gone())
            COINS.remove(p.id());
        else
            COINS.put(p.id(), new Coin(new Vec3(p.x(), p.y(), p.z()), p.born(), p.rgb()));
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || COINS.isEmpty())
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            COINS.clear();
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float now = mc.level.getGameTime() + partial;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(METAL));
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        PoseStack.Pose last = pose.last();
        COINS.values().removeIf(c -> now - c.born > GunFuTuning.COIN_TICKS + 4);
        for (Coin c : COINS.values()) {
            float t = Math.max(0f, now - c.born);
            Vec3 at = c.origin.add(0, GunFuTuning.COIN_UP * t - 0.5 * GunFuTuning.COIN_GRAVITY * t * t, 0);
            // Turning about the horizontal axis across the flipper's view: the face swings from flat to edge-on and round again.
            float angle = t * SPIN;
            double up = Math.cos(angle) * HALF, out = Math.sin(angle) * HALF;
            Vec3 yawAxis = Vec3.directionFromRotation(0f, mc.player == null ? 0f : mc.player.getYRot()).cross(new Vec3(0, 1, 0)).normalize();
            Vec3 across = yawAxis.scale(HALF);
            Vec3 face = new Vec3(0, up, 0).add(Vec3.directionFromRotation(0f, mc.player == null ? 0f : mc.player.getYRot()).scale(out));
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(at));
            float r = (c.rgb >> 16 & 255) / 255f, g = (c.rgb >> 8 & 255) / 255f, b = (c.rgb & 255) / 255f;
            Vec3 n = face.lengthSqr() < 1e-8 ? new Vec3(0, 1, 0) : across.cross(face).normalize();
            vertex(vc, last, at.subtract(across).subtract(face), 0, 1, r, g, b, light, n);
            vertex(vc, last, at.add(across).subtract(face), 1, 1, r, g, b, light, n);
            vertex(vc, last, at.add(across).add(face), 1, 0, r, g, b, light, n);
            vertex(vc, last, at.subtract(across).add(face), 0, 0, r, g, b, light, n);
        }
        buffers.endBatch(RenderType.entityCutoutNoCull(METAL));
        pose.popPose();
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 at, float u, float v, float r, float g, float b, int light, Vec3 n) {
        vc.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setColor(r, g, b, 1f).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }
}
