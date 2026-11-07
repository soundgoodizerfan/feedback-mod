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
     * {@code side} is +1 for the right arm; {@code twoHanded} says whether the other hand is free to hold the forend.
     */
    static void draw(PoseStack pose, MultiBufferSource buffers, int light, GunView view, float side, boolean twoHanded) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.isInvisible())
            return;
        float[] grip = GunRenderer.firstPersonGrip(view), at = view.hand();
        arm(pose, buffers, light, tune(view), side, grip[0] * side / 16f, grip[1] / 16f, grip[2] / 16f);
        if (twoHanded && view.longGun()) {
            float[] fore = GunDraw.forend(view);
            arm(pose, buffers, light, tune(view), -side, (grip[0] * side - at[0]) / 16f, (grip[1] + fore[0] - at[1]) / 16f, (grip[2] + fore[1] - at[2]) / 16f);
        }
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
    private static void arm(PoseStack pose, MultiBufferSource buffers, int light, float[] t, float f, float x, float y, float z) {
        var player = Minecraft.getInstance().player;
        var renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        pose.pushPose();
        pose.translate(x + f * t[0] / 16f, y + t[1] / 16f, z + t[2] / 16f);
        pose.mulPose(Axis.YP.rotationDegrees(f * t[3]));
        pose.mulPose(Axis.XP.rotationDegrees(t[4]));
        pose.mulPose(Axis.ZP.rotationDegrees(f * t[5]));
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
