// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

/**
 * The player's bare arms on a gun in first person. The arm is drawn with vanilla's own empty-hand arm transform
 * ({@code ItemInHandRenderer.renderPlayerArm}), which is known to put a forearm in view from the lower corner of the
 * screen, moved as a whole so its fist lands on the grip (and, for a long gun with a free off hand, the other arm's
 * on the forend). Drawn in the hand-pivot space {@link GunRenderer#renderHeld} starts from.
 */
final class GunArms {

    private GunArms() {
    }

    /**
     * Draw the shooting arm at the grip and, for a long gun with a free off hand, the support arm at the forend.
     * {@code side} is +1 for the right arm; {@code twoHanded} says whether the other hand is free to hold the forend; {@code parts} moves the hands with the gun's working parts (bolt, lever, forend, magazine, cylinder, cover).
     */
    static void draw(PoseStack pose, MultiBufferSource buffers, int light, GunView view, float side, boolean twoHanded, GunMotion.Parts parts,
                     PoseStack gunFrame) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.isInvisible())
            return;
        float[] at = view.hand();
        // The shooting hand holds the gun at its hold point, and goes to the bolt or lever while one works.
        float[] hand = at, shoot = GunDraw.shootTarget(view, parts);
        if (shoot != null)
            hand = new float[]{at[0] + (shoot[1] - at[0]) * shoot[0], at[1] + (shoot[2] - at[1]) * shoot[0], at[2] + (shoot[3] - at[2]) * shoot[0]};
        float[] p = frame(gunFrame, hand[0], hand[1], hand[2]);
        arm(pose, buffers, light, tune(view), side, p[0], p[1], p[2]);
        // A one-handed gun's reload (or fanning) takes both hands: the free hand comes in from the side, if nothing is in it.
        float[] free = twoHanded ? GunDraw.handgunSupport(view, parts) : null;
        if (free != null) {
            float[] rest = GunDraw.HAND_WAITS_AT, w = {free[0]};
            float[] q = frame(gunFrame, rest[0] + (free[1] - rest[0]) * w[0], rest[1] + (free[2] - rest[1]) * w[0], rest[2] + (free[3] - rest[2]) * w[0]);
            // The pose tuned for a long gun's support hand: in from the lower left, the forearm's far end clear of the camera.
            // A fanning hand is near the back of the gun, so the forearm is tipped down and in from the left to keep its far end off the camera.
            boolean fan = parts.fan() >= 0f;
            arm(pose, buffers, light, TUNES.get("long_gun"), -side, q[0], q[1], q[2], fan ? 20f : 0f, fan ? 40f : 0f, 0f);
        }
        if (twoHanded && view.longGun()) {
            float[] t = GunDraw.supportTarget(view, parts), turn = GunDraw.supportTurn(view, parts);
            float[] q = frame(gunFrame, t[0], t[1], t[2]);
            arm(pose, buffers, light, tune(view), -side, q[0], q[1], q[2], turn[0], turn[1], turn[2]);
        }
    }

    /** A gun-space point (pixels) in the hand pivot's frame, blocks, through the gun's own scale, kick and tilt. */
    private static float[] frame(PoseStack gunFrame, float x, float y, float z) {
        var v = gunFrame.last().pose().transformPosition(x, y, z, new org.joml.Vector3f());
        return new float[]{v.x, v.y, v.z};
    }

    /**
     * Arm nudge per gun shape ({@link #shape}): x, y, z in pixels (x mirrors for the left arm), then yaw, pitch, roll
     * in degrees. Baked values go in here; {@link GunArmTuneScreen} edits them live.
     */
    static final java.util.Map<String, float[]> TUNES = new java.util.HashMap<>();

    static {
        TUNES.put("handgun", new float[]{2.5f, -0.25f, 3.75f, 40f, 0f, -65f});
        TUNES.put("long_gun", new float[]{3.75f, -4.25f, 1f, 73f, 38f, -65f});
        TUNES.put("machine_gun", new float[]{6f, -1.25f, 2.5f, 71f, 2f, -69f});
    }

    /** The pose class a gun's arm is tuned for: its layout. */
    static String shape(GunView view) {
        return view.type().getSerializedName();
    }

    static float[] tune(GunView view) {
        return TUNES.computeIfAbsent(shape(view), k -> new float[6]);
    }

    /** One arm, vanilla's way, with its frame moved to ({@code x, y, z}) blocks from the hand pivot first. */
    private static void arm(PoseStack pose, MultiBufferSource buffers, int light, float[] t, float f, float x, float y, float z, float... turn) {
        float dyaw = turn.length > 0 ? turn[0] : 0f, dpitch = turn.length > 1 ? turn[1] : 0f, droll = turn.length > 2 ? turn[2] : 0f;
        var player = Minecraft.getInstance().player;
        var renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        pose.pushPose();
        pose.translate(x + f * t[0] / 16f, y + t[1] / 16f, z + t[2] / 16f);
        pose.mulPose(Axis.YP.rotationDegrees(f * (t[3] + dyaw)));
        pose.mulPose(Axis.XP.rotationDegrees(t[4] + dpitch));
        pose.mulPose(Axis.ZP.rotationDegrees(f * (t[5] + droll)));
        pose.mulPose(Axis.YP.rotationDegrees(f * 45f));
        pose.translate(f * -1f, 3.6f, 3.5f);
        pose.mulPose(Axis.ZP.rotationDegrees(f * 120f));
        pose.mulPose(Axis.XP.rotationDegrees(200f));
        pose.mulPose(Axis.YP.rotationDegrees(f * -135f));
        pose.translate(f * 5.6f, 0f, 0f);
        if (f > 0)
            renderer.renderRightHand(pose, buffers, light, player);
        else
            renderer.renderLeftHand(pose, buffers, light, player);
        pose.popPose();
    }
}
