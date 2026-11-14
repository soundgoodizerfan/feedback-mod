// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.combat;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.tether.GrappleHook;
import io.github.soundgoodizerfan.feedback.combat.tether.RopeWrap;
import io.github.soundgoodizerfan.feedback.item.GrappleItem;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.net.GrappleReleasePayload;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The grapple swing, run where the player's motion actually lives: their own client. Each tick, after the player has moved, a taut
 * line cancels the velocity running away from its pivot and draws the stretch back ({@link GrappleHook#constrain}); a slack line does nothing.
 *
 * <ul>
 * <li><b>Bends.</b> The pivot is the last bend the server laid ({@link RopeWrap}) and the line that is left is the rope minus
 * the part already used up between anchor and bend.
 * <li><b>Winch and pay-out.</b> Holding use with the grapple in hand winches in, the drum spinning up over
 * {@link ModularTuning#GRAPPLE_SPIN_UP_TICKS}; the pay-out key lets line out up to the grapple's longest line. Winching near the pivot
 * drags speed down so the drum does not slam the player into the anchor.
 * <li><b>Pumping.</b> Airborne on a taut line, a held direction adds speed up to a cap, and air drag is cut while on a line and
 * for a moment after, so a swing keeps what it is given.
 * <li><b>Jump.</b> A jump press lets every line go with an upward impulse (the server drops the hooks).
 * <li><b>Two lines</b> are solved together over a few passes, not one after the other.
 * </ul>
 *
 * <p>Ideas from yyon's Grappling Hook Mod (GPL-3.0): motor ramp, slow near the anchor, input while swinging, air-friction takeover,
 * jump off the rope. Not taken: its config scalars, look-steered "smart motor", rocket and wall-run extras, its trusting position sync
 * (strain and parting stay on the server in {@link GrappleHook}).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class GrappleSwing {

    /** Lets line out while held. */
    public static final KeyMapping PAY_OUT = new KeyMapping("key.feedback.grapple_pay_out", InputConstants.KEY_X, "key.categories.feedback");

    /** The line the player's own client runs per hook (by entity id): the whole rope from anchor, shortened by the reel, lengthened by pay-out. */
    private static final java.util.Map<Integer, Float> ropes = new java.util.HashMap<>();
    private static boolean swinging;
    private static boolean jumpHeld;
    /** Set on a jump off the line until the server has dropped the hooks, so the lines do not pull the player back. */
    private static int detached;
    private static int coast;
    private static int spin;

    private GrappleSwing() {
    }

    /** The whole rope the local player has out on {@code hookId}, or {@code fallback} for someone else's hook. */
    public static float rope(int hookId, float fallback) {
        return ropes.getOrDefault(hookId, fallback);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null || mc.isPaused() || player.isSpectator())
            return;
        boolean jump = mc.screen == null && mc.options.keyJump.isDown();
        boolean jumpPressed = jump && !jumpHeld;
        jumpHeld = jump;
        var hooks = new java.util.ArrayList<>(GrappleHook.allOf(mc.level, player));
        hooks.removeIf(h -> !h.anchored() || h.swingRope() <= 0f);
        if (hooks.isEmpty()) {
            Vec3 v = player.getDeltaMovement();
            // Letting go while moving throws the swing's momentum on (boosted, capped).
            if (swinging) {
                if (v.length() >= ModularTuning.GRAPPLE_RELEASE_MIN && !player.onGround()) {
                    Vec3 boosted = v.scale(ModularTuning.GRAPPLE_RELEASE_BOOST);
                    player.setDeltaMovement(boosted.length() > ModularTuning.GRAPPLE_RELEASE_MAX ? boosted.scale(ModularTuning.GRAPPLE_RELEASE_MAX / boosted.length()) : boosted);
                }
                coast = ModularTuning.GRAPPLE_COAST_TICKS;
            } else if (coast > 0 && !player.onGround() && !player.isInWater() && !player.isFallFlying()) {
                keepAir(player);
                coast--;
            } else
                coast = 0;
            swinging = false;
            detached = 0;
            spin = 0;
            ropes.clear();
            return;
        }
        swinging = true;
        if (detached > 0) {
            // Jumped off: the server is dropping the hooks; no pull meanwhile.
            detached--;
            return;
        }
        if (jumpPressed) {
            Vec3 v = player.getDeltaMovement();
            if (!player.onGround())
                player.setDeltaMovement(v.x, Math.max(v.y, 0) + ModularTuning.GRAPPLE_JUMP_IMPULSE, v.z);
            PacketDistributor.sendToServer(new GrappleReleasePayload());
            detached = 10;
            return;
        }
        ropes.keySet().retainAll(hooks.stream().map(h -> h.getId()).toList());
        boolean grappleInHand = player.getMainHandItem().getItem() instanceof GrappleItem || player.getOffhandItem().getItem() instanceof GrappleItem;
        // Holding use with the grapple in hand winches every line in; the drum takes a moment to come up to speed.
        boolean reeling = mc.screen == null && mc.options.keyUse.isDown() && grappleInHand || WhipClient.winching();
        boolean payOut = mc.screen == null && PAY_OUT.isDown();
        spin = reeling ? Math.min(spin + 1, ModularTuning.GRAPPLE_SPIN_UP_TICKS) : 0;
        float drum = 0.3f + 0.7f * spin / ModularTuning.GRAPPLE_SPIN_UP_TICKS;
        Vec3 mid = player.position().add(0, player.getBbHeight() * 0.5, 0);
        int n = hooks.size();
        Vec3[] pivots = new Vec3[n];
        float[] eff = new float[n];
        boolean taut = false;
        for (int i = 0; i < n; i++) {
            GrappleHook hook = hooks.get(i);
            List<Vec3> bends = hook.bends();
            Vec3 anchor = hook.position();
            double fixed = RopeWrap.fixedLength(anchor, bends);
            pivots[i] = bends.isEmpty() ? anchor : bends.get(bends.size() - 1);
            float rope = ropes.getOrDefault(hook.getId(), 0f);
            if (rope <= 0f || rope > hook.maxRope())
                rope = hook.swingRope();
            if (reeling)
                rope = Math.max((float) (fixed + ModularTuning.GRAPPLE_STOP_DISTANCE), rope - hook.reelSpeed() * drum);
            if (payOut)
                rope = Math.min(hook.maxRope(), rope + ModularTuning.GRAPPLE_PAYOUT);
            ropes.put(hook.getId(), rope);
            eff[i] = Math.max((float) ModularTuning.GRAPPLE_STOP_DISTANCE, (float) (rope - fixed));
            taut |= pivots[i].subtract(mid).length() >= eff[i] - 0.25;
        }
        if (taut && !player.onGround())
            pump(player);
        for (int pass = 0; pass < (n > 1 ? ModularTuning.GRAPPLE_JOINT_PASSES : 1); pass++)
            for (int i = 0; i < n; i++)
                player.setDeltaMovement(GrappleHook.constrain(pivots[i].subtract(mid), eff[i], player.getDeltaMovement()));
        if (reeling)
            for (Vec3 pivot : pivots)
                if (pivot.subtract(mid).length() < ModularTuning.GRAPPLE_BRAKE_RADIUS) {
                    player.setDeltaMovement(player.getDeltaMovement().scale(ModularTuning.GRAPPLE_BRAKE_KEEP));
                    break;
                }
        if (!player.onGround())
            keepAir(player);
        player.fallDistance = 0f;
    }

    /** A held direction adds horizontal speed on a taut line, never past the cap it is already above. */
    private static void pump(net.minecraft.client.player.LocalPlayer player) {
        float f = player.input.forwardImpulse, l = player.input.leftImpulse;
        if (f == 0f && l == 0f)
            return;
        double yaw = Math.toRadians(player.getYRot()), sin = Math.sin(yaw), cos = Math.cos(yaw);
        double dx = l * cos - f * sin, dz = f * cos + l * sin;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len > 1) {
            dx /= len;
            dz /= len;
        }
        Vec3 v = player.getDeltaMovement();
        double nx = v.x + dx * ModularTuning.GRAPPLE_AIR_ACCEL, nz = v.z + dz * ModularTuning.GRAPPLE_AIR_ACCEL;
        double now = Math.sqrt(nx * nx + nz * nz), before = Math.sqrt(v.x * v.x + v.z * v.z);
        double limit = Math.max(before, ModularTuning.GRAPPLE_AIR_CAP);
        if (now > limit) {
            nx *= limit / now;
            nz *= limit / now;
        }
        player.setDeltaMovement(nx, v.y, nz);
    }

    /** Takes vanilla's airborne drag (0.91 a tick) back up to {@link ModularTuning#GRAPPLE_AIR_KEEP}. */
    private static void keepAir(net.minecraft.client.player.LocalPlayer player) {
        double k = ModularTuning.GRAPPLE_AIR_KEEP / 0.91;
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x * k, v.y, v.z * k);
    }
}
