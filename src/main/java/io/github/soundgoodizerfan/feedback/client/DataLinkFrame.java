// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Flushes {@link DeferredDataRenderer} once per frame, after block entities have queued
 * everything they want drawn -- the same hook MrCrayfish's Furniture Mod: Refurbished's
 * {@code 1.21.1} branch uses for its own electricity overlay.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class DataLinkFrame {

    @SubscribeEvent
    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
            return;

        DataSelection.queueWire(mc, event.getPartialTick().getGameTimeDeltaPartialTick(true));
        AimHud.queueLinkLine(mc, event.getPartialTick().getGameTimeDeltaPartialTick(true));
        PoseStack stack = event.getPoseStack();
        stack.pushPose();
        Vec3 camera = event.getCamera().getPosition();
        stack.translate(-camera.x(), -camera.y(), -camera.z());
        DeferredDataRenderer.get().draw(stack);
        stack.popPose();
    }
}
