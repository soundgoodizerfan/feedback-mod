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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import com.mojang.math.Axis;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryStats;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.registry.FCombat;
import io.github.soundgoodizerfan.feedback.combat.kick.KickTuning;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/**
 * Procedural combat poses from {@link CombatState}, client only, no authored clips. Nothing here writes
 * a rotation of the player, so the crosshair never moves.
 *
 * <p>Hooks, all events, no mixin:
 * <ul>
 * <li>{@link RenderLivingEvent.Pre} (third person, self and others): fires after vanilla has set the arm
 * poses and before {@code setupAnim}. It tips the {@code PoseStack} for the stagger
 * recoil, and sets the weapon arm's pose to the added {@code ArmPose} {@code FEEDBACK_PARRY} (declared in
 * {@code META-INF/enumextensions.json}, as {@link SpearPose} is), whose transformer then raises the arm.</li>
 * <li>{@link RenderHandEvent} (first person, self): offsets the weapon hand for the parry, and both hands
 * for a stagger. A roll is not drawn here: it is the {@code fist_tumble_right} clip ({@link SwingClips}).</li>
 * </ul>
 *
 * <p>The heft the poses scale by is the parry window's own: read off {@link ParryStats}, which holds the
 * recovery lockout {@link ParryTuning#recovery}; heaviness is that recovery's place between
 * {@link ParryTuning#RECOVERY_LIGHT} and {@link ParryTuning#RECOVERY_HEAVY}, i.e. the curve inverted, not
 * a second curve. Timing: the server writes the state every tick and it syncs, so {@code ticksLeft} is
 * current to about a tick; the render partial tick smooths between. A parry's active window runs
 * {@code window + ping pad} on the server; the pose counts from {@code window} only, so a padded press
 * holds the ready position for the pad's few ticks before rising.
 * Stagger carries no total on the state, so its recoil fades over the last
 * {@link CombatPoseTuning#RECOIL_FADE_TICKS} ticks. The head does not recoil on its own (that needs
 * {@code setupAnim}, which no event reaches); the whole body tips.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class CombatPose {

    /** One entity's pose this frame. {@code hand} is the weapon hand, null if the pose is not a parry. */
    private record Frame(InteractionHand hand, float heaviness, float raise, float droop, float pitchDeg, float recoil) {
    }

    private static final Frame NONE = new Frame(null, 0f, 0f, 0f, 0f, 0f);

    /** Set in {@link #onLiving} for the entity being drawn, read by the arm transformer in the same call; render thread only. */
    private static Frame current = NONE;

    private static HumanoidModel.ArmPose parryPose;

    public static final EnumProxy<HumanoidModel.ArmPose> PARRY_PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, false,
            (IArmPoseTransformer) (model, entity, arm) -> {
                Frame f = current;
                var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                float side = arm == HumanoidArm.RIGHT ? 1f : -1f;
                float lift = Mth.lerp(f.heaviness, CombatPoseTuning.LIFT_LIGHT_RAD, CombatPoseTuning.LIFT_HEAVY_RAD);
                part.xRot = -lift * f.raise + CombatPoseTuning.DROOP_RAD * f.droop;
                part.yRot = -side * CombatPoseTuning.ACROSS_RAD * f.raise;
                part.zRot = 0f;
            });

    private CombatPose() {
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    private static Frame frame(LivingEntity e, float partial) {
        if (!e.hasData(FCombat.STATE))
            return NONE;
        CombatState s = CombatState.of(e);
        CombatPhase phase = s.phase();
        if (phase == CombatPhase.IDLE)
            return NONE;
        float left = s.ticksLeft() - partial;
        switch (phase) {
            case PARRY_ACTIVE, PARRY_RECOVERY -> {
                ParryStats st = ParryStats.of(e).orElse(null);
                if (st == null || st.bare())
                    return NONE;
                float h = Mth.clamp(Mth.inverseLerp(st.recovery(), ParryTuning.RECOVERY_LIGHT, ParryTuning.RECOVERY_HEAVY), 0f, 1f);
                if (phase == CombatPhase.PARRY_ACTIVE) {
                    float ticks = Mth.lerp(h, CombatPoseTuning.RAISE_TICKS_LIGHT, CombatPoseTuning.RAISE_TICKS_HEAVY);
                    float raise = smooth(Math.max(0f, st.window() - left) / ticks);
                    return new Frame(st.hand(), h, raise, 0f, 0f, 0f);
                }
                float r = Mth.clamp(1f - left / Math.max(1, st.recovery()), 0f, 1f);
                return new Frame(st.hand(), h, 1f - smooth(r), h * Mth.sin(Mth.PI * r), 0f, 0f);
            }
            case STAGGER -> {
                float recoil = smooth(left / CombatPoseTuning.RECOIL_FADE_TICKS);
                return new Frame(null, 0f, 0f, 0f, -recoil * CombatPoseTuning.RECOIL_DEG, recoil);
            }
            default -> {
                return NONE;
            }
        }
    }

    /** 0..1 how far out the kicking leg is (a half sine over the KICK phase), 0 when not kicking. */
    static float kick(LivingEntity e, float partial) {
        if (!e.hasData(FCombat.STATE))
            return 0f;
        CombatState s = CombatState.of(e);
        if (s.phase() != CombatPhase.KICK)
            return 0f;
        return Mth.sin(Mth.PI * Mth.clamp(1f - (s.ticksLeft() - partial) / KickTuning.SWING, 0f, 1f));
    }

    /** Third person: the body tip, and the parry arm. */
    @SubscribeEvent
    public static void onLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity e = event.getEntity();
        Frame f = frame(e, event.getPartialTick());
        current = f;
        if (kick(e, event.getPartialTick()) > 0f && event.getRenderer().getModel() instanceof PlayerModel<?> pm) {
            // setupAnim would overwrite the leg, so it is hidden here and KickLegLayer draws it kicking.
            boolean right = e.getMainArm() == HumanoidArm.RIGHT;
            (right ? pm.rightLeg : pm.leftLeg).visible = false;
            (right ? pm.rightPants : pm.leftPants).visible = false;
        }
        if (f == NONE)
            return;
        if (f.pitchDeg != 0f) {
            // Tip about the horizontal axis across the body, turned to the facing: vanilla yaws the model by
            // 180 - bodyRot after this event, so the tip is wrapped in that yaw and its inverse.
            float yaw = 180f - Mth.rotLerp(event.getPartialTick(), e.yBodyRotO, e.yBodyRot);
            var pose = event.getPoseStack();
            pose.translate(0f, CombatPoseTuning.PIVOT_Y, 0f);
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            pose.mulPose(Axis.XP.rotationDegrees(f.pitchDeg));
            pose.mulPose(Axis.YP.rotationDegrees(-yaw));
            pose.translate(0f, -CombatPoseTuning.PIVOT_Y, 0f);
        }
        if (f.hand != null && event.getRenderer().getModel() instanceof HumanoidModel<?> model) {
            if (parryPose == null)
                parryPose = HumanoidModel.ArmPose.valueOf("FEEDBACK_PARRY");
            HumanoidArm arm = f.hand == InteractionHand.MAIN_HAND ? e.getMainArm() : e.getMainArm().getOpposite();
            if (arm == HumanoidArm.RIGHT)
                model.rightArmPose = parryPose;
            else
                model.leftArmPose = parryPose;
        }
    }

    /** First person: the weapon hand for a parry; both hands for a stagger. */
    @SubscribeEvent
    public static void onHand(RenderHandEvent event) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null)
            return;
        Frame f = frame(mc.player, event.getPartialTick());
        if (f == NONE)
            return;
        var pose = event.getPoseStack();
        if (f.hand == event.getHand()) {
            HumanoidArm arm = f.hand == InteractionHand.MAIN_HAND ? mc.player.getMainArm() : mc.player.getMainArm().getOpposite();
            float side = arm == HumanoidArm.RIGHT ? 1f : -1f;
            float up = CombatPoseTuning.HAND_UP * (1f - 0.4f * f.heaviness) * f.raise - CombatPoseTuning.HAND_DROOP * f.droop;
            pose.translate(-side * CombatPoseTuning.HAND_ACROSS * f.raise, up, -CombatPoseTuning.HAND_FORWARD * f.raise);
            pose.mulPose(Axis.ZP.rotationDegrees(side * CombatPoseTuning.HAND_ROLL_DEG * f.raise));
        } else if (f.hand == null) {
            pose.translate(0f, -CombatPoseTuning.HAND_STAGGER_DROP * f.recoil,
                    CombatPoseTuning.HAND_STAGGER_BACK * f.recoil);
        }
    }
}
