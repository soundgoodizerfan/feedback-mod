// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.combat;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Eases the local view toward a direction a cast asked for, instead of snapping it with a position correction. It runs
 * every rendered frame, not every tick, so the turn is as smooth as the frame rate: each frame covers a share of the
 * remaining angle that depends on the real time since the last one (the same glide at 30 fps or 240). Gives up when
 * close, or when the player is in a screen.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class CameraTurn {

    /** Per tick (50 ms), the rate the remaining angle closes at: e^-RATE of it is left after a tick (a stand-in, to be felt in play); and the degrees at which it is done. */
    private static final float RATE = 0.55f, DONE = 0.15f;

    private static float yaw, pitch;
    private static boolean turning;

    private CameraTurn() {
    }

    public static void aim(float toYaw, float toPitch) {
        yaw = toYaw;
        pitch = toPitch;
        turning = true;
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (!turning || p == null || mc.screen != null) {
            turning = false;
            return;
        }
        float dy = Mth.wrapDegrees(yaw - p.getYRot()), dp = pitch - p.getXRot();
        if (Math.abs(dy) < DONE && Math.abs(dp) < DONE) {
            turning = false;
            return;
        }
        float share = 1f - (float) Math.exp(-RATE * event.getPartialTick().getRealtimeDeltaTicks());
        p.setYRot(p.getYRot() + dy * share);
        p.setXRot(Mth.clamp(p.getXRot() + dp * share, -90f, 90f));
        p.yRotO = p.getYRot(); // no tick interpolation fighting the per-frame steps
        p.xRotO = p.getXRot();
    }
}
