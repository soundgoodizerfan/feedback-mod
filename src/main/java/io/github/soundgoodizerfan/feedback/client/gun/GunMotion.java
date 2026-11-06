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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import io.github.soundgoodizerfan.feedback.client.GunClient;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunLayout;
import io.github.soundgoodizerfan.feedback.weapon.MagazineSize;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * How a held gun moves: every motion is procedural, built from the gun's own figures and parts,
 * because a modular gun has no single model to author clips for.
 *
 * <p>From TacZ (GPL-3.0, read at its {@code 1.21.1} branch, commit {@code 7f5b112}; design only,
 * nothing copied): the shot kick on the held model as a timestamp with a fast rise and an
 * ease-out-cubic decay over about 0.3 s, a little random twist, and almost none of it when aimed
 * ({@code FirstPersonRenderGunEvent}); turn lag clamped with {@code tanh} so a flick does not throw
 * the gun; a dip on jumping and landing; a springy aim instead of a linear one; a slide that snaps
 * back, returns slower and bounces once, and stays open on an empty self-loader. <em>Not</em> taken:
 * its per-gun keyframe clips and Lua state machine (a gun assembled from parts has no clip to
 * author), its fixed 0.18 s slide cycle (here the slide's travel follows the gun's own cycle and a
 * manual action takes its action's own time), and its threaded second-order dynamics class (a
 * per-frame spring does the job). Reload is a short timeline built in code per feed type.
 *
 * <p>Two layers: {@link #onHand} moves the whole gun in view space (aim, sprint, sway, landing);
 * {@link #item} turns it about the hand in item space (kick, reload tilt); {@link #parts} says
 * where each moving part is (slide, bolt, lever, pump, hinge, cylinder, magazine).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class GunMotion {

    /** Where the moving parts are, all 0 at rest. Fractions of their travel unless named otherwise. */
    record Parts(float slide, float boltLift, float boltBack, float lever, float pump, float breakOpen,
                 float cylinderOut, float cylinderTurn, float magDrop, boolean magOut, float cover, float hammer) {
        static final Parts REST = new Parts(0, 0, 0, 0, 0, 0, 0, 0, 0, false, 0, 0);
    }

    /** One hand's recent history: its last shot and its reload. */
    static final class Hand {
        long shotMs = -1_000_000;
        int cycleMs = 150;
        float kickDeg;
        int shots;
        long reloadStart, reloadEnd;
        /** A small random twist chosen per shot. */
        float twist;
    }

    private static final Hand MAIN = new Hand(), OFF = new Hand();

    // View-space state, eased every frame.
    private static float aimPos, aimVel;
    private static float sprint;
    private static float swayYaw, swayPitch;
    private static float lastYaw, lastPitch;
    private static long lastFrameMs;
    private static boolean wasOnGround = true;
    private static double lastVy;
    /** Landing or take-off bump: amplitude and the wall-clock ms it started. */
    private static float bump;
    private static long bumpMs;

    /** Vanilla's first-person hand pivot in view space (ItemInHandRenderer.applyItemArmTransform), blocks. */
    private static final float HAND_ACROSS = 0.56f, HAND_DOWN = -0.52f;
    /** Where an aimed sight line sits on screen: a touch under the eye, view-space blocks. */
    private static final float SIGHT_Y = -0.05f;

    private GunMotion() {
    }

    /** The client fired {@code off}'s gun: kick and cycle from now. */
    public static void shot(boolean off, int cycleMs, float kickDeg) {
        Hand h = off ? OFF : MAIN;
        h.shotMs = System.currentTimeMillis();
        h.cycleMs = Math.max(cycleMs, 40);
        h.kickDeg = kickDeg;
        h.shots++;
        h.twist = (float) (Math.random() * 2 - 1);
    }

    /** A reload began and should end at {@code untilMs}. Both hands: the server runs them back to back. */
    public static void reload(long untilMs) {
        long now = System.currentTimeMillis();
        for (Hand h : new Hand[]{MAIN, OFF}) {
            h.reloadStart = now;
            h.reloadEnd = untilMs;
        }
    }

    public static void reloadStop() {
        MAIN.reloadEnd = OFF.reloadEnd = 0;
    }

    static Hand hand(boolean off) {
        return off ? OFF : MAIN;
    }

    /** 0 to 1 over [a, b], smoothed. */
    private static float ramp(float t, float a, float b) {
        float x = Mth.clamp((t - a) / (b - a), 0f, 1f);
        return x * x * (3 - 2 * x);
    }

    /** Up over [a, b], held, down over [c, d]. */
    private static float hold(float t, float a, float b, float c, float d) {
        return ramp(t, a, b) * (1 - ramp(t, c, d));
    }

    private static float reloadPhase(Hand h, long now) {
        if (h.reloadEnd <= now || h.reloadEnd <= h.reloadStart)
            return -1;
        return (now - h.reloadStart) / (float) (h.reloadEnd - h.reloadStart);
    }

    /**
     * Where the moving parts are for {@code view} in {@code hand} (null: someone else's gun, or one
     * in a slot, which only shows what its components say: an empty self-loader open).
     */
    static Parts parts(GunView view, Hand h, long now) {
        boolean lockOpen = view.selfLoading() && view.rounds() == 0;
        if (h == null)
            return lockOpen ? new Parts(1, 0, 0, 0, 0, 0, 0, 0, 0, false, 0, 0) : Parts.REST;
        float s = (now - h.shotMs) / 1000f;
        float cycle = h.cycleMs / 1000f;
        float slide = 0, boltLift = 0, boltBack = 0, lever = 0, pump = 0, hammer = 0;
        GunLayout.Action action = GunLayout.Action.of(view.action());
        if (view.selfLoading()) {
            // Snap back, ride home slower, one small bounce; all inside the gun's own cycle.
            float back = Math.min(0.035f, cycle * 0.25f), home = Math.min(0.09f, cycle * 0.55f);
            if (s < back)
                slide = s / back;
            else if (s < back + home)
                slide = 1 - ramp(s, back, back + home);
            else if (s < back + home + 0.06f)
                slide = -0.06f * Mth.sin((s - back - home) / 0.06f * Mth.PI);
            if (lockOpen && s >= back)
                slide = 1;
        } else if (action.manualMs() > 0 && !view.action().equals("none")) {
            float t = s / (action.manualMs() / 1000f);
            if (t < 1.05f) {
                switch (view.action()) {
                    case "bolt" -> {
                        boltLift = hold(t, 0.05f, 0.22f, 0.8f, 0.97f);
                        boltBack = hold(t, 0.25f, 0.45f, 0.55f, 0.77f);
                    }
                    case "lever" -> lever = hold(t, 0.05f, 0.4f, 0.55f, 0.95f);
                    case "pump" -> pump = hold(t, 0.05f, 0.4f, 0.5f, 0.9f);
                    default -> {
                    }
                }
                hammer = 1 - ramp(t, 0.6f, 0.9f);
            }
        }
        if (action.hammer() && view.has("F"))
            hammer = Math.max(hammer, 1 - ramp(s, 0f, 0.05f));
        // A revolver turns a chamber per shot, at once.
        float turn = (h.shots - 1 + ramp(s, 0f, 0.07f)) * 60f;

        float magDrop = 0, cover = 0, breakOpen = 0, out = 0;
        boolean magOut = false;
        float p = reloadPhase(h, now);
        if (p >= 0) {
            if (view.boxMagazine() || (view.mag() == null && !view.has("F") && !view.has("E") && !view.has("b")
                    && !view.has("Y") && !view.action().equals("break") && view.selfLoading())) {
                // Out, gone, a fresh one up and seated.
                float drop = ramp(p, 0.12f, 0.32f);
                float rise = 1 - ramp(p, 0.48f, 0.72f);
                magDrop = p < 0.4f ? drop : rise;
                magOut = p > 0.3f && p < 0.5f;
            }
            if (view.has("F"))
                out = hold(p, 0.08f, 0.22f, 0.8f, 0.92f);
            if (view.action().equals("break"))
                breakOpen = hold(p, 0.06f, 0.2f, 0.84f, 0.96f);
            if (view.has("Y") || view.mag() == MagazineSize.BELT)
                cover = hold(p, 0.1f, 0.25f, 0.8f, 0.92f);
            if (!view.selfLoading() && view.action().equals("bolt") && !view.has("E") && !view.boxMagazine()) {
                boltLift = Math.max(boltLift, hold(p, 0.02f, 0.12f, 0.88f, 0.97f));
                boltBack = Math.max(boltBack, hold(p, 0.12f, 0.24f, 0.8f, 0.9f));
            }
        }
        if (out > 0)
            turn += (now % 100_000) / 1000f * 360f * out;
        return new Parts(slide, boltLift, boltBack, lever, pump, breakOpen, out, turn, magDrop, magOut, cover, hammer);
    }

    /** The shot kick and the reload tilt, turned about the hand ({@code hand} in model pixels, before the scale). */
    static void item(PoseStack pose, GunView view, Hand h, long now) {
        float[] at = view.hand();
        float a = Mth.clamp(aimPos, 0f, 1f);
        float s = (now - h.shotMs) / 1000f;
        float k = 0;
        if (s >= 0 && s < 0.32f)
            k = s < 0.02f ? s / 0.02f : (float) Math.pow(1 - (s - 0.02f) / 0.3f, 3);
        float calm = ClientSettings.reduceMotion() ? 0.3f : 1f;
        float scale = Mth.clamp(h.kickDeg, 0.5f, 10f) * (1 - 0.8f * a) * calm;
        float pitch = k * scale * 1.6f;
        float back = k * Mth.clamp(h.kickDeg * 0.15f, 0.25f, 1.25f) * (1 - 0.7f * a) * calm;
        float twist = k * h.twist * scale * 0.4f;

        float p = reloadPhase(h, now);
        float tilt = p < 0 ? 0 : hold(p, 0f, 0.16f, 0.84f, 1f) * calm;
        // A tube is fed from below with the gun rolled further over; a cylinder swings out to the left.
        float roll = tilt * (view.has("E") ? 38f : view.has("F") ? -24f : 22f);
        float lift = tilt * (view.has("F") ? 18f : 10f);
        if (p >= 0 && view.has("E")) {
            // Thumb a round in every beat of the reload.
            int beat = io.github.soundgoodizerfan.feedback.core.FTuning.GUN_RELOAD_ROUND_MS;
            float within = ((now - h.reloadStart) % beat) / (float) beat;
            lift += tilt * 4f * Mth.sin(within * Mth.PI);
        }

        pose.translate(at[0], at[1], at[2]);
        pose.translate(0, -tilt * 0.75f, back);
        pose.mulPose(Axis.XP.rotationDegrees(pitch + lift));
        pose.mulPose(Axis.YP.rotationDegrees(twist));
        pose.mulPose(Axis.ZP.rotationDegrees(roll + twist * 0.5f));
        pose.translate(-at[0], -at[1], -at[2]);
    }

    /** Ease every view-space motion toward what the player is doing. */
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null)
            return;
        long now = System.currentTimeMillis();
        float dt = lastFrameMs == 0 ? 0 : Math.min(now - lastFrameMs, 50) / 1000f;
        lastFrameMs = now;
        if (dt <= 0)
            return;

        // Aim: a spring toward the key, a touch under critical so it settles with a little give.
        float target = GunClient.aim() > 0 ? ramp(GunClient.aim(), 0f, 1f) : 0f;
        for (int i = 0; i < 4; i++) {
            float d = dt / 4, w = 22f, z = 0.82f;
            aimVel += (w * w * (target - aimPos) - 2 * z * w * aimVel) * d;
            aimPos += aimVel * d;
        }

        boolean holding = player.getMainHandItem().getItem() instanceof GunItem || player.getOffhandItem().getItem() instanceof GunItem;
        boolean wantSprint = holding && player.isSprinting() && GunClient.aim() < 0.05f && !GunClient.reloading();
        sprint += ((wantSprint ? 1 : 0) - sprint) * Math.min(1f, dt * 9f);

        // Turn lag: how fast the view turns, squashed by tanh so a flick leans the gun and no more.
        float yaw = player.getViewYRot(1f), pitch = player.getViewXRot(1f);
        float yawRate = Mth.wrapDegrees(yaw - lastYaw) / dt, pitchRate = (pitch - lastPitch) / dt;
        lastYaw = yaw;
        lastPitch = pitch;
        float k = Math.min(1f, dt * 14f);
        swayYaw += ((float) Math.tanh(yawRate / 420f) - swayYaw) * k;
        swayPitch += ((float) Math.tanh(pitchRate / 420f) - swayPitch) * k;

        boolean onGround = player.onGround();
        if (onGround && !wasOnGround) {
            bump = Mth.clamp((float) -lastVy * 1.4f, 0.15f, 1f);
            bumpMs = now;
        } else if (!onGround && wasOnGround && player.getDeltaMovement().y > 0) {
            bump = -0.45f;
            bumpMs = now;
        }
        wasOnGround = onGround;
        lastVy = player.getDeltaMovement().y;
    }

    /** View-space motion of a held gun: aim to the middle of the screen, sprint low, lag on turns, dip on landing. */
    @SubscribeEvent
    public static void onHand(RenderHandEvent event) {
        var mc = Minecraft.getInstance();
        ItemStack stack = event.getItemStack();
        if (mc.player == null || !(stack.getItem() instanceof GunItem))
            return;
        boolean main = event.getHand() == InteractionHand.MAIN_HAND;
        HumanoidArm arm = main ? mc.player.getMainArm() : mc.player.getMainArm().getOpposite();
        float side = arm == HumanoidArm.RIGHT ? 1f : -1f;
        PoseStack pose = event.getPoseStack();
        float a = Mth.clamp(aimPos, -0.05f, 1.08f);
        float calm = ClientSettings.reduceMotion() ? 0.3f : 1f;

        if (main && a != 0) {
            // Solve the aim from the drawn gun: its centre line to the middle, its sight line to just under the eye.
            GunView view = GunView.of(stack);
            float[] grip = GunRenderer.firstPersonGrip(view);
            float across = HAND_ACROSS + grip[0] / 16f;
            float line = HAND_DOWN + grip[1] / 16f + (view.sightLine() - view.hand()[1]) * GunRenderer.FIRST_PERSON_SCALE;
            pose.translate(-side * across * a, (SIGHT_Y - line) * a, 0.04f * a);
        }
        float loose = (1 - 0.85f * Mth.clamp(a, 0f, 1f)) * calm;
        pose.translate(-swayYaw * 0.04f * loose, swayPitch * 0.03f * loose, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-swayYaw * 4f * loose));
        pose.mulPose(Axis.XP.rotationDegrees(-swayPitch * 3f * loose));

        float t = (System.currentTimeMillis() - bumpMs) / 1000f;
        if (t < 0.6f)
            pose.translate(0, -bump * 0.07f * (float) Math.exp(-t * 7f) * Mth.cos(t * 14f) * loose, 0);

        if (sprint > 0.001f) {
            float sp = sprint * calm;
            pose.translate(-side * 0.08f * sp, -0.1f * sp, 0.06f * sp);
            pose.mulPose(Axis.YP.rotationDegrees(side * 32f * sp));
            pose.mulPose(Axis.XP.rotationDegrees(-14f * sp));
        }
    }
}
