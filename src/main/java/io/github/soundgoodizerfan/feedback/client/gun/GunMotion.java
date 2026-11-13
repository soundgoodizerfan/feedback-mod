// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import io.github.soundgoodizerfan.feedback.weapon.GunTuning;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.ClientSettings;
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
 * <p>Two layers: {@link #onHand} draws the held gun and moves it in view space (bob, aim, sprint, sway, landing);
 * {@link #item} turns it about the hand in item space (kick, reload tilt); {@link #parts} says
 * where each moving part is (slide, bolt, lever, pump, hinge, cylinder, magazine).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class GunMotion {

    /** Where the moving parts are, all 0 at rest. Fractions of their travel unless named otherwise. */
    record Parts(float slide, float boltLift, float boltBack, float lever, float pump, float breakOpen,
                 float cylinderOut, float cylinderTurn, float magDrop, boolean magOut, float cover, float hammer, float vent,
                 float feed, float fed, float fan, float feedOf, float grab) {
        static final Parts REST = new Parts(0, 0, 0, 0, 0, 0, 0, 0, 0, false, 0, 0, -1, -1, 0, -1, 0, -1);
    }

    /** One hand's recent history: its last shot and its reload. */
    static final class Hand {
        long shotMs = -1_000_000;
        int cycleMs = 150;
        float kickDeg;
        int shots;
        long reloadStart, reloadEnd;
        /** Rounds the reload under way puts in (a gun with its own feed), for the hand that loads them one at a time. */
        int reloadRounds;
        /** Whether the last shot was fanned: the free hand slapped the hammer for it. */
        boolean fanned;
        /** When a core breech was last broken open to vent. */
        long ventMs = -1_000_000;
        /** When a pulse gun's pump slider was last worked. */
        long pumpMs = -1_000_000;
        /** A small random twist chosen per shot. */
        float twist;
    }

    private static final Hand MAIN = new Hand(), OFF = new Hand();

    // View-space state, eased every frame.
    private static float aimPos, aimVel;
    /** A shot (the gun pointed and firing) holds the sprint carry off for this long, so the firing pose has priority. */
    private static final long SPRINT_AFTER_SHOT_MS = 600;
    private static float sprint;
    private static boolean hadGun;
    private static float swayYaw, swayPitch;
    private static float lastYaw, lastPitch;
    private static long lastFrameMs;
    private static boolean wasOnGround = true;
    private static double lastVy;
    /** Landing or take-off bump: amplitude and the wall-clock ms it started. */
    private static float bump;
    private static long bumpMs;

    /** Vanilla's first-person hand pivot in view space (ItemInHandRenderer.applyItemArmTransform), blocks. */
    private static final float HAND_ACROSS = 0.56f, HAND_DOWN = -0.52f, HAND_FORWARD = -0.72f;

    private GunMotion() {
    }

    /** The next shot of {@code off}'s gun is (or is not) fanned. */
    public static void fanning(boolean off, boolean on) {
        (off ? OFF : MAIN).fanned = on;
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
    public static void reload(long untilMs, int rounds) {
        long now = System.currentTimeMillis();
        for (Hand h : new Hand[]{MAIN, OFF}) {
            h.reloadStart = now;
            h.reloadEnd = untilMs;
            h.reloadRounds = rounds;
        }
    }

    /** How long a core breech vent takes, ms: the server keeps the gun hot, and times its sounds, by the same beats ({@code ElectricMechanism.reloadKey}). */
    private static final int VENT_MS = 900;

    /** The reload key broke a core breech open to vent: the barrels tip down and come back. */
    public static void vent() {
        long now = System.currentTimeMillis();
        if (ventPhase(MAIN, now) >= 0f)
            return;
        MAIN.ventMs = now;
        OFF.ventMs = now;
    }

    /** How long a pulse gun's pump stroke takes, ms. */
    private static final int PUMP_MS = 380;

    /**
     * A forend slammed back and thrown forward, 0 to 1 over a stroke: a hard yank (decelerating into the stop), a held beat,
     * a throw that speeds up to the stop, and a small rebound past rest. The click lands where the throw ends (t = 0.55).
     */
    private static float jerk(float t) {
        if (t < 0.15f) {
            float x = 1f - t / 0.15f;
            return 1f - x * x * x;
        }
        if (t < 0.4f)
            return 1f;
        if (t < 0.55f) {
            float x = (t - 0.4f) / 0.15f;
            return 1f - x * x;
        }
        if (t < 0.75f)
            return -0.1f * Mth.sin((t - 0.55f) / 0.2f * Mth.PI);
        return 0f;
    }

    /** The pump key worked a pulse gun's slider: it draws back and runs home. */
    public static void pump() {
        MAIN.pumpMs = System.currentTimeMillis();
    }

    /** 0 to 1 through a vent, or -1 when none is under way. */
    private static float ventPhase(Hand h, long now) {
        float t = (now - h.ventMs) / (float) VENT_MS;
        return t < 0f || t >= 1f ? -1f : t;
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

    private static float lin(float t, float a, float b) {
        return Mth.clamp((t - a) / (b - a), 0f, 1f);
    }

    /** 0 to 1 over [a, b], all its speed at the start: a part hit hard, then coasting to a stop. */
    private static float snapOut(float t, float a, float b) {
        float x = 1f - lin(t, a, b);
        return 1f - x * x * x;
    }

    /** 0 to 1 over [a, b], all its speed at the end: a part thrown home and stopped dead. */
    private static float slamIn(float t, float a, float b) {
        float x = lin(t, a, b);
        return x * x;
    }

    /** Snapped out over [a, b], held, slammed back over [c, d]: the rough version of {@link #hold}. */
    private static float jolt(float t, float a, float b, float c, float d) {
        return snapOut(t, a, b) * (1f - slamIn(t, c, d));
    }

    /**
     * A core breech vented, 0 at rest and 1 down: the support hand comes over the top and slams down on the barrels (they give at
     * 0.18 to 0.26, a little past the stop), they shake while the steam goes, the hand takes the forend from below and throws them
     * shut at 0.6 to 0.72, and they bounce once on the latch.
     */
    private static float ventBreak(float t) {
        if (t < 0.18f)
            return 0f;
        if (t < 0.26f)
            return 1.12f * snapOut(t, 0.18f, 0.26f);
        if (t < 0.6f) {
            float k = (t - 0.26f) / 0.34f;
            return 1f + 0.05f * Mth.sin(t * 90f) * (1f - k);
        }
        if (t < 0.72f)
            return 1f - slamIn(t, 0.6f, 0.72f);
        if (t < 0.86f)
            return -0.07f * Mth.sin((t - 0.72f) / 0.14f * Mth.PI);
        return 0f;
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
            return lockOpen ? new Parts(1, 0, 0, 0, 0, 0, 0, 0, 0, false, 0, 0, -1, -1, 0, -1, 0, -1) : Parts.REST;
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
                        boltLift = jolt(t, 0.05f, 0.22f, 0.8f, 0.97f);
                        boltBack = jolt(t, 0.25f, 0.45f, 0.55f, 0.77f);
                    }
                    case "lever" -> lever = jolt(t, 0.05f, 0.4f, 0.55f, 0.95f);
                    case "pump" -> pump = jolt(t, 0.05f, 0.4f, 0.5f, 0.9f);
                    default -> {
                    }
                }
                hammer = 1 - ramp(t, 0.6f, 0.9f);
            }
        }
        if (view.electric() && view.has("p")) {
            float t = (now - h.pumpMs) / (float) PUMP_MS;
            if (t >= 0f && t < 1f)
                pump = jerk(t);
        }
        if (action.hammer() && view.has("F"))
            hammer = Math.max(hammer, 1 - ramp(s, 0f, 0.05f));
        float fan = -1, n0 = 0, grab = -1;
        // A fanned gun is slapped on its hammer by the free hand once a cycle, and for two cycles after the last shot.
        if (h.fanned && view.type() == GunLayout.Type.HANDGUN && s / cycle < 3f)
            fan = s / cycle;
        // A revolver turns a chamber per shot, at once.
        float turn = (h.shots - 1 + ramp(s, 0f, 0.07f)) * 60f;

        float magDrop = 0, cover = 0, breakOpen = 0, out = 0, vent = -1, feed = -1, fed = 0;
        boolean magOut = false;
        float p = reloadPhase(h, now);
        if (p >= 0) {
            if (view.boxMagazine() || (view.mag() == null && !view.has("F") && !view.has("E") && !view.has("b")
                    && !view.has("Y") && !view.action().equals("break") && view.selfLoading())) {
                // Out, gone, a fresh one up and seated.
                float drop = snapOut(p, 0.12f, 0.32f);
                float rise = 1 - slamIn(p, 0.48f, 0.72f);
                magDrop = p < 0.4f ? drop : rise;
                magOut = p > 0.3f && p < 0.5f;
            }
            if (view.has("F")) {
                // A top-break keeps its cylinder in the frame (the frame tips instead); any other revolver swings it out.
                out = view.action().equals("break") ? 0f : jolt(p, 0.08f, 0.22f, 0.8f, 0.92f);
                // The cylinder is open from 0.22 to 0.8 of the reload; the free hand puts a round in each chamber in turn.
                int n = h.reloadRounds;
                n0 = n;
                if (n > 0) {
                    // Fetch every round in one go (0 to 0.2 of the window), put them in one at a time, then take the hand away (0.9 to 1).
                    float win = (p - 0.22f) / 0.58f;
                    if (win >= 0f && win < 0.2f)
                        grab = win / 0.2f;
                    else if (win >= 0.2f && win < 0.9f) {
                        float u = (win - 0.2f) / 0.7f * n;
                        fed = Mth.clamp((float) Math.floor(u), 0f, n - 1f);
                        feed = u - fed;
                    } else if (win >= 0.9f) {
                        fed = n;
                        grab = 1f + Mth.clamp((win - 0.9f) / 0.1f, 0f, 1f);
                    }
                }
            }
            if (view.action().equals("break"))
                breakOpen = jolt(p, 0.04f, 0.2f, 0.8f, 0.94f);
            if (view.has("Y") || view.mag() == MagazineSize.BELT)
                cover = jolt(p, 0.1f, 0.25f, 0.8f, 0.92f);
            if (!view.selfLoading() && view.action().equals("bolt") && !view.has("E") && !view.boxMagazine()) {
                boltLift = Math.max(boltLift, jolt(p, 0.02f, 0.12f, 0.88f, 0.97f));
                boltBack = Math.max(boltBack, jolt(p, 0.12f, 0.24f, 0.8f, 0.9f));
            }
        }
        if (view.electric() && view.has("s") && view.variant("s").equals("core_breech")) {
            float vp = ventPhase(h, now);
            if (vp >= 0f) {
                breakOpen = ventBreak(vp);
                vent = vp;
            }
        }
        if ((out > 0 || breakOpen > 0) && view.has("F") && h.reloadRounds > 0) // the next empty chamber comes round to the top before each round goes in
            turn = -60f * (view.rounds() - 1 + (feed < 0f ? fed : fed + ramp(feed, 0f, 0.2f)));
        return new Parts(slide, boltLift, boltBack, lever, pump, breakOpen, out, turn, magDrop, magOut, cover, hammer, vent, feed, fed, fan, n0, grab);
    }

    /** The shot kick and the reload tilt, turned about the hand ({@code hand} in model pixels, before the scale). */
    static void item(PoseStack pose, GunView view, Hand h, long now) {
        float[] at = view.hand();
        float a = Mth.clamp(aimPos, 0f, 1f);
        float s = (now - h.shotMs) / 1000f;
        // The hand rides the recoil out and back in a fraction of the gun's cycle: a fast gun twitches, a slow one heaves. A pulse gun's
        // own kick is small, so its hand still moves a readable amount (the view's kick is unchanged).
        float d = Mth.clamp(h.cycleMs / 1000f * 0.9f, 0.1f, 0.32f);
        float k = 0;
        if (s >= 0 && s < d)
            k = s < 0.02f ? s / 0.02f : (float) Math.pow(1 - (s - 0.02f) / (d - 0.02f), 3);
        float calm = ClientSettings.reduceMotion() ? 0.3f : 1f;
        boolean pulse = view.pulse() != null;
        float scale = Mth.clamp(h.kickDeg, pulse ? 3f : 0.5f, 10f) * (1 - 0.8f * a) * calm;
        float pitch = k * scale * 1.6f;
        float back = k * Mth.clamp(h.kickDeg * 0.15f, pulse ? 0.6f : 0.25f, 1.25f) * (1 - 0.7f * a) * calm;
        float twist = k * h.twist * scale * 0.4f;

        float p = reloadPhase(h, now);
        float tilt = p < 0 ? 0 : hold(p, 0f, 0.16f, 0.84f, 1f) * calm;
        // A tube is fed from below with the gun rolled further over; a cylinder swings out to the left.
        float roll = tilt * (view.has("E") ? 38f : view.has("F") ? -24f : 22f);
        float lift = tilt * (view.has("F") ? 18f : 10f);
        if (p >= 0 && view.has("E")) {
            // Thumb a round in every beat of the reload.
            int beat = io.github.soundgoodizerfan.feedback.weapon.GunTuning.GUN_RELOAD_ROUND_MS;
            float within = ((now - h.reloadStart) % beat) / (float) beat;
            lift += tilt * 4f * Mth.sin(within * Mth.PI);
        }

        // Breaking a core breech open lifts the gun a little, the way a shotgun is tipped to clear it.
        float vp = ventPhase(h, now);
        if (vp >= 0f)
            lift += ventBreak(vp) * 8f * calm;

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

        boolean holding = GunLike.is(player.getMainHandItem().getItem()) || GunLike.is(player.getOffhandItem().getItem());
        // Taking up a gun again (picked up after being knocked away) starts its motion from rest.
        if (holding && !hadGun) {
            aimPos = aimVel = sprint = 0;
            MAIN.reloadEnd = OFF.reloadEnd = 0;
            MAIN.shotMs = OFF.shotMs = -1_000_000;
            MAIN.kickDeg = OFF.kickDeg = 0;
            bump = 0;
        }
        hadGun = holding;
        boolean wantSprint = holding && player.isSprinting() && GunClient.aim() < 0.05f && !GunClient.reloading()
                && System.currentTimeMillis() - Math.max(MAIN.shotMs, OFF.shotMs) > SPRINT_AFTER_SHOT_MS;
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

    /**
     * Draws a held gun in first person ourselves, as Scorched Guns does (GPL-3.0, read at {@code master}
     * {@code d0daef8}; design taken, code written here): vanilla's draw is cancelled so the gun can
     * <ul>
     * <li>undo vanilla's view bob on the hand and put back only the share that suits the aim, so a
     * sighted gun stays still while walking;</li>
     * <li>aim by moving the gun until its eye point ({@link GunView#eye}) is the camera, so the eye looks
     * down the sights and the receiver sits behind it;</li>
     * <li>then lag on turns, dip on landing, lower for a sprint, swing for a bludgeon.</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onHand(RenderHandEvent event) {
        var mc = Minecraft.getInstance();
        if (GunLens.rendering()) { // the lens's own world pass shows no hands
            event.setCanceled(true);
            return;
        }
        ItemStack stack = event.getItemStack();
        if (mc.player == null || !GunLike.is(stack.getItem()))
            return;
        event.setCanceled(true);
        boolean main = event.getHand() == InteractionHand.MAIN_HAND;
        HumanoidArm arm = main ? mc.player.getMainArm() : mc.player.getMainArm().getOpposite();
        float side = arm == HumanoidArm.RIGHT ? 1f : -1f;
        PoseStack pose = event.getPoseStack();
        float pt = event.getPartialTick();
        float aimed = ease(Mth.clamp(aimPos, 0f, 1f));
        float a = main ? aimed : 0f;
        float calm = ClientSettings.reduceMotion() ? 0.3f : 1f;
        GunView view = GunView.of(stack);

        pose.pushPose();
        rebob(pose, pt, 1f - aimed);
        if (!main)
            pose.translate(0, -aimed, 0); // the other gun drops out of the way of the sights
        if (a > 0) {
            float[] eye = GunRenderer.firstPersonEye(view, side);
            pose.translate(-HAND_ACROSS * side * a, -HAND_DOWN * a, -HAND_FORWARD * a);
            pose.translate(-eye[0] * a, -eye[1] * a, -eye[2] * a);
        }
        float swing = event.getSwingProgress();
        if (swing > 0) {
            // Vanilla's swing, for the bludgeon: it draws nothing of ours otherwise.
            float root = Mth.sqrt(swing);
            pose.translate(side * -0.4f * Mth.sin(root * Mth.PI), 0.2f * Mth.sin(root * Mth.PI * 2), -0.2f * Mth.sin(swing * Mth.PI));
        }
        pose.translate(HAND_ACROSS * side, HAND_DOWN - 0.6f * event.getEquipProgress(), HAND_FORWARD);
        if (swing > 0) {
            float f = Mth.sin(swing * swing * Mth.PI), f1 = Mth.sin(Mth.sqrt(swing) * Mth.PI);
            pose.mulPose(Axis.YP.rotationDegrees(side * (45f + f * -20f)));
            pose.mulPose(Axis.ZP.rotationDegrees(side * f1 * -20f));
            pose.mulPose(Axis.XP.rotationDegrees(f1 * -80f));
            pose.mulPose(Axis.YP.rotationDegrees(side * -45f));
        }
        // A little roll through the raise, none at either end.
        float through = Mth.sin(a * Mth.PI) * calm;
        pose.mulPose(Axis.ZP.rotationDegrees(side * through * 6f));
        pose.mulPose(Axis.XP.rotationDegrees(through * 3f));

        float loose = (1 - 0.85f * a) * calm;
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
        GunRenderer.renderHeld(stack, view, hand(!main), side, pose, event.getMultiBufferSource(), event.getPackedLight());
        pose.popPose();
    }

    private static float ease(float x) {
        return x * x * (3 - 2 * x);
    }

    /**
     * Take vanilla's view bob off the hand ({@code GameRenderer.bobView} applied it to the whole hand
     * pass, in this order) and put back {@code keep} of it.
     */
    private static void rebob(PoseStack pose, float pt, float keep) {
        var mc = Minecraft.getInstance();
        if (!mc.options.bobView().get() || !(mc.getCameraEntity() instanceof net.minecraft.world.entity.player.Player p))
            return;
        float walked = -(p.walkDist + (p.walkDist - p.walkDistO) * pt);
        float b = Mth.lerp(pt, p.oBob, p.bob);
        float s = Mth.sin(walked * Mth.PI), c = Math.abs(Mth.cos(walked * Mth.PI)), c2 = Math.abs(Mth.cos(walked * Mth.PI - 0.2f));
        pose.mulPose(Axis.XP.rotationDegrees(-c2 * b * 5f));
        pose.mulPose(Axis.ZP.rotationDegrees(-s * b * 3f));
        pose.translate(-s * b * 0.5f, c * b, 0);
        b *= keep;
        pose.translate(s * b * 0.5f, -c * b, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(s * b * 3f));
        pose.mulPose(Axis.XP.rotationDegrees(c2 * b * 5f));
    }
}
