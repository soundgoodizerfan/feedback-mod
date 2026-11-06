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

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The player's bare arms on a gun in first person: the shooting arm reaches in from the side to the grip, and the
 * other arm to the forend of a long gun when nothing else is in that hand. The vanilla arm model (skin, sleeve and
 * slim or wide build) is drawn in gun space, where one unit is one model pixel and so one arm-model unit, with its
 * shoulder placed behind, below and outside the hand so the forearm points along the line to it. Drawn inside the
 * gun's own pose, the arms follow its kick, sway and aim.
 */
final class GunArms {

    /** Shoulder-to-hand direction for the shooting arm, in gun space (x toward the gun's own side, -z forward, y up). */
    private static final Vector3f MAIN = new Vector3f(-0.35f, 0.45f, -0.85f).normalize();
    /** The support arm comes from the other side. */
    private static final Vector3f SUPPORT = new Vector3f(0.55f, 0.35f, -0.75f).normalize();
    /** From the arm's pivot to the middle of the fist, in model pixels (the arm box runs 10 below its pivot). */
    private static final float REACH = 8f;

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
        float[] at = view.hand();
        arm(pose, buffers, light, player, side > 0 ? HumanoidArm.RIGHT : HumanoidArm.LEFT, at[0], at[1], at[2], MAIN, side);
        if (twoHanded && view.longGun()) {
            float[] fore = GunDraw.forend(view);
            arm(pose, buffers, light, player, side > 0 ? HumanoidArm.LEFT : HumanoidArm.RIGHT, 0f, fore[0], fore[1], SUPPORT, side);
        }
    }

    private static void arm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, HumanoidArm which,
                            float x, float y, float z, Vector3f direction, float side) {
        var renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        var part = which == HumanoidArm.RIGHT ? renderer.getModel().rightArm : renderer.getModel().leftArm;
        // Mirror the direction's sideways part for the arm that is not the shooting arm's side.
        Vector3f d = new Vector3f(direction.x * side, direction.y, direction.z);
        pose.pushPose();
        pose.translate(x - d.x * REACH, y - d.y * REACH, z - d.z * REACH);
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), d));
        // The part draws itself about its own pivot on the body; bring that pivot to the origin.
        pose.translate(-part.x, -part.y, -part.z);
        if (which == HumanoidArm.RIGHT)
            renderer.renderRightHand(pose, buffers, light, player);
        else
            renderer.renderLeftHand(pose, buffers, light, player);
        pose.popPose();
    }
}
