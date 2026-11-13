// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.client.combat.CombatPose;
import io.github.soundgoodizerfan.feedback.client.combat.CombatPoseTuning;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/**
 * How a long gun is held in third person: a rifle stance. The body turns toward the gun side, the shooting arm
 * points the gun along the look direction (lowered by a ready drop until aimed), and the support arm reaches onto
 * the gun's axis, at the forend if that is within reach and as far forward as the arm goes if not, so a long gun
 * never leaves the hand floating off it. The arm angles are worked out from the look direction and the shoulders'
 * real positions, not tuned per gun, so length does not tip the muzzle off the line of sight.
 *
 * <p>Design from Scorched Guns' held-weapon poses (a pose per grip with up, forward and down variants and a body
 * yaw offset; GPL-3.0, see {@code THIRD-PARTY-LICENSES.md}); the maths and code are written anew. Two added
 * {@code ArmPose} values carry the arm angles (their transformers run inside {@code setupAnim}, after the head
 * and before the swing and crouch); {@link CombatPose} calls {@link #begin} and {@link #end} around the draw.
 * Nothing here touches the player's own rotation for longer than one draw: the body yaw is put back in {@link #end}.
 */
public final class GunStance {

    /** What this draw needs, set in {@link #begin} and read by the transformers; render thread only. */
    private static float drop, reach;
    private static boolean active;
    private static float savedBody, savedBodyO;

    private static HumanoidModel.ArmPose mainPose, supportPose;

    public static final EnumProxy<HumanoidModel.ArmPose> MAIN_PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, false,
            (IArmPoseTransformer) (model, entity, arm) -> main(model, arm));
    public static final EnumProxy<HumanoidModel.ArmPose> SUPPORT_PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, false,
            (IArmPoseTransformer) (model, entity, arm) -> support(model, arm));

    private GunStance() {
    }

    /**
     * Sets the stance for a player holding a long gun: arm poses on the model, body turned. Returns whether it did,
     * so the caller can leave other guns to the plain bow pose. Call from {@code RenderLivingEvent.Pre}.
     */
    public static boolean begin(RenderLivingEvent.Pre<?, ?> event) {
        active = false;
        LivingEntity e = event.getEntity();
        if (!(e instanceof Player) || !io.github.soundgoodizerfan.feedback.client.gun.GunLike.is(e.getMainHandItem().getItem()) || e.isPassenger()
                || e.getPose() != Pose.STANDING && e.getPose() != Pose.CROUCHING
                || !GunRenderer.longGun(e.getMainHandItem()) || !(event.getRenderer().getModel() instanceof HumanoidModel<?> model))
            return false;
        float aim = GunClient.aimAmount(e);
        drop = (float) Math.toRadians(Mth.lerp(aim, CombatPoseTuning.GUN_READY_DROP_DEG, 0f));
        reach = GunRenderer.forendReach(e.getMainHandItem());
        boolean right = e.getMainArm() == HumanoidArm.RIGHT;
        if (mainPose == null) {
            mainPose = HumanoidModel.ArmPose.valueOf("FEEDBACK_GUN_MAIN");
            supportPose = HumanoidModel.ArmPose.valueOf("FEEDBACK_GUN_SUPPORT");
        }
        if (right)
            model.rightArmPose = mainPose;
        else
            model.leftArmPose = mainPose;
        if (e.getOffhandItem().isEmpty()) {
            if (right)
                model.leftArmPose = supportPose;
            else
                model.rightArmPose = supportPose;
        }
        float turn = Mth.lerp(aim, CombatPoseTuning.GUN_BODY_TURN_IDLE_DEG, CombatPoseTuning.GUN_BODY_TURN_AIM_DEG) * (right ? 1f : -1f);
        savedBody = e.yBodyRot;
        savedBodyO = e.yBodyRotO;
        e.yBodyRot = e.getYRot() + turn;
        e.yBodyRotO = e.yRotO + turn;
        active = true;
        return true;
    }

    /** Puts the body yaw back after the draw. Call from {@code RenderLivingEvent.Post}. */
    public static void end(LivingEntity e) {
        if (!active)
            return;
        e.yBodyRot = savedBody;
        e.yBodyRotO = savedBodyO;
        active = false;
    }

    /** The unit direction the head looks along, in model space, pitched {@code pitch} and turned {@code yaw} (radians). */
    private static float[] look(float pitch, float yaw) {
        float c = Mth.cos(pitch);
        return new float[]{-c * Mth.sin(yaw), Mth.sin(pitch), -c * Mth.cos(yaw)};
    }

    /** Points an arm along {@code d} (model space, from the shoulder); the arm's own axis is down its length. */
    private static void point(ModelPart arm, float[] d) {
        arm.xRot = -(float) Math.acos(Mth.clamp(d[1], -1f, 1f));
        arm.yRot = (float) Mth.atan2(-d[0], -d[2]);
        arm.zRot = 0f;
    }

    private static ModelPart arm(HumanoidModel<?> model, HumanoidArm side) {
        return side == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
    }

    /** The gun's direction in model space: the look direction, dropped by the ready angle. */
    private static float[] aim(HumanoidModel<?> model) {
        float pitch = Mth.clamp(model.head.xRot + drop, -Mth.HALF_PI, Mth.HALF_PI);
        return look(pitch, model.head.yRot);
    }

    private static void main(HumanoidModel<?> model, HumanoidArm side) {
        point(arm(model, side), aim(model));
    }

    private static void support(HumanoidModel<?> model, HumanoidArm side) {
        ModelPart supp = arm(model, side), mainArm = arm(model, side.getOpposite());
        float[] d = aim(model);
        // Where the gun is held: the shooting arm's shoulder out along the gun by the grip's place on the arm.
        float gx = mainArm.x + d[0] * CombatPoseTuning.GUN_GRIP_PX, gy = mainArm.y + d[1] * CombatPoseTuning.GUN_GRIP_PX,
                gz = mainArm.z + d[2] * CombatPoseTuning.GUN_GRIP_PX;
        // How far along the gun the support hand lands: the forend, or where the arm runs out of length.
        float wx = gx - supp.x, wy = gy - supp.y, wz = gz - supp.z;
        float b = wx * d[0] + wy * d[1] + wz * d[2];
        float disc = b * b - (wx * wx + wy * wy + wz * wz) + CombatPoseTuning.GUN_SUPPORT_REACH_PX * CombatPoseTuning.GUN_SUPPORT_REACH_PX;
        float t = Math.max(0f, Math.min(reach, disc >= 0f ? -b + Mth.sqrt(disc) : -b));
        float tx = gx + d[0] * t - supp.x, ty = gy + d[1] * t - supp.y, tz = gz + d[2] * t - supp.z;
        float len = Mth.sqrt(tx * tx + ty * ty + tz * tz);
        if (len < 1e-3f)
            return;
        point(supp, new float[]{tx / len, ty / len, tz / len});
    }
}
