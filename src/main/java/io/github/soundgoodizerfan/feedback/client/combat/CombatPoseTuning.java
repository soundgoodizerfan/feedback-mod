// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.combat;

import io.github.soundgoodizerfan.feedback.client.gun.GunStance;

/**
 * Every angle, offset and duration of the procedural combat poses ({@link CombatPose}, implementation
 * plan 2.6 and section 0 "Animation"). Placeholder until seen in game. Angles are degrees unless the
 * name says radians (the model parts take radians). "Heaviness" is the 0..1 heft figure the parry
 * window already uses ({@code ParryTuning.heaviness}); a pose end named LIGHT is a dagger, HEAVY a
 * greatsword, and the figure between them is linear like the window's.
 */
public final class CombatPoseTuning {

    private CombatPoseTuning() {
    }

    // ---- parry raise (third person arm, and first person hand) ----

    /** Ticks the weapon takes to come up from the ready position to full raise: light end and heavy end. A heavy blade is slow to bring up. */
    static final float RAISE_TICKS_LIGHT = 1f, RAISE_TICKS_HEAVY = 4f;
    /** Arm pitch at full raise, radians up from hanging (1.57 is level forward, more is above it). A heavy weapon is held lower. */
    static final float LIFT_LIGHT_RAD = 2.2f, LIFT_HEAVY_RAD = 1.6f;
    /** Arm swing across the body at full raise, radians of yRot (right arm turns one way, left the other). */
    static final float ACROSS_RAD = 0.7f;
    /** Extra droop at the middle of the recovery, radians of xRot, at heaviness 1 (scaled down to 0 at heaviness 0). The arm sags below ready and comes back. */
    static final float DROOP_RAD = 0.45f;

    // ---- first person hand, for the same phases ----

    /** Hand offset at full raise, blocks: up, across the body (toward the centre), forward (away from the eye). Lift scales by 1 - 0.4 * heaviness. */
    static final float HAND_UP = 0.35f, HAND_ACROSS = 0.3f, HAND_FORWARD = 0.15f;
    /** Hand roll at full raise, degrees, about the view axis, tilting the weapon across the body. */
    static final float HAND_ROLL_DEG = 22f;
    /** How far a heavy weapon's hand sags at the middle of recovery, blocks, at heaviness 1. */
    static final float HAND_DROOP = 0.3f;
    /** How far a stagger jolts the hands, blocks, back toward the eye and down. */
    static final float HAND_STAGGER_BACK = 0.2f, HAND_STAGGER_DROP = 0.15f;

    // ---- roll ----

    /** Height of the pivot the body turns about, blocks above the feet (about the belly). */
    static final float PIVOT_Y = 0.9f;

    // ---- kick ----

    /** Leg pitch at the top of the kick, radians up from hanging (1.57 is level forward). */
    public static final float KICK_LIFT_RAD= 1.5f;

    // ---- stagger ----

    /** Backward pitch of the body at the start of a stagger, degrees. */
    static final float RECOIL_DEG = 14f;
    /** The recoil fades over this many ticks left; a stagger shorter than this starts already part-faded. */
    static final float RECOIL_FADE_TICKS = 8f;

    // ---- long gun stance, third person ({@link GunStance}) ----

    /** How far below the line of sight a carried long gun points, degrees; aimed it is level with it. */
    public static final float GUN_READY_DROP_DEG= 25f;
    /** How far the body turns toward the gun side, degrees, carried and aimed. */
    public static final float GUN_BODY_TURN_IDLE_DEG= 30f, GUN_BODY_TURN_AIM_DEG = 45f;
    /** Where the grip sits along the shooting arm from the shoulder, and how far the support hand reaches from its shoulder, in model pixels. */
    public static final float GUN_GRIP_PX= 7f, GUN_SUPPORT_REACH_PX = 10f;
}
