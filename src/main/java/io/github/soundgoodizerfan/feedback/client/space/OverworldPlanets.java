// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.space;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.space.SkyBodies;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * The planets in the Overworld's night sky: a few faint unnamed points a little brighter than the stars, each where
 * {@link SkyBodies#direction} puts it, drawn once the sky is. They fade with the stars and with rain, and are not drawn by day, below the
 * horizon, or in any other dimension. Plain coloured quads, no textures.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class OverworldPlanets {

    /** How far off the quads sit; the sky's own radius. */
    private static final float R = 100f;
    /** Half the width of a point, in blocks at {@link #R} (about a third of a degree). */
    private static final float HALF = 0.35f;

    private OverworldPlanets() {
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY)
            return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().equals(Level.OVERWORLD))
            return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        // The stars' own brightness runs 0 by day to 0.5 at night, less in rain; a planet shines through twice as strongly as a star.
        float shine = Math.min(1f, level.getStarBrightness(partial) * 2f * 1.6f);
        if (shine <= 0.01f)
            return;
        Matrix4f view = event.getModelViewMatrix();
        BufferBuilder b = null;
        for (SkyBodies.Body body : SkyBodies.ALL) {
            Vec3 d = SkyBodies.direction(level, body, partial);
            if (!SkyBodies.up(d))
                continue;
            if (b == null)
                b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            point(b, view, d, body.tint(), shine);
        }
        if (b == null)
            return;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    /** One square point facing the camera: right and up are at right angles to the direction. */
    private static void point(BufferBuilder b, Matrix4f view, Vec3 d, int tint, float shine) {
        Vec3 right = d.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(d).normalize();
        int a = Math.round(255f * shine) << 24;
        int colour = a | (tint & 0xFFFFFF);
        for (int k = 0; k < 4; k++) {
            double sx = k == 1 || k == 2 ? 1 : -1, sy = k >= 2 ? 1 : -1;
            b.addVertex(view, (float) (d.x * R + (right.x * sx + up.x * sy) * HALF), (float) (d.y * R + (right.y * sx + up.y * sy) * HALF),
                    (float) (d.z * R + (right.z * sx + up.z * sy) * HALF)).setColor(colour);
        }
    }
}
