// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.punch;

import io.github.soundgoodizerfan.feedback.Feedback;

/**
 * The Knuckleblaster's figures. Every number is a placeholder to be felt in play. Ticks are server ticks (20 a second);
 * impact is joules, the unit the parry's and the kick's stagger already read.
 */
public final class PunchTuning {

    /** Technique id the Knuckleblaster grants: a heavy punch, and a shockwave when the press is held. */
    public static final net.minecraft.resources.ResourceLocation HEAVY_PUNCH = Feedback.id("heavy_punch");

    /** Blocks the fist reaches from the eye, and the grace added round the target's box. */
    public static final double REACH = 3.0, GRACE = 0.3;
    /** Ticks before the next press (the fist is out, then drawn back). */
    public static final int COOLDOWN = 22;
    /** Stamina a punch and a shockwave spend. */
    public static final float STAMINA = 12f, SHOCK_STAMINA = 18f;
    /** Damage of the fist. A kick is 2: this is the weapon that hits. */
    public static final float DAMAGE = 6f;
    /** Joules a punch carries per Mu of the puncher's stand-in mass, and its knockback bounds (see {@code KickTuning}, which reads the same way). */
    public static final float JOULES_PER_PUNCHER_MU = 2f;
    public static final float KNOCK_PER_SPECIFIC_IMPACT = 0.9f, KNOCK_MIN = 0.3f, KNOCK_MAX = 2.4f;
    /** Wear points a landed punch and a shockwave cost the arm (tools wear with use). */
    public static final int WEAR_PUNCH = 2, WEAR_SHOCK = 6;

    /** Ticks (a second) the press must stay down after the click punch before the shells fire (client), and how long after a punch the server still takes it. */
    public static final int SHOCK_HOLD_TICKS = 20, SHOCK_WINDOW_TICKS = 30;
    /** Ticks the arm is spent after a shockwave, in place of {@link #COOLDOWN}. */
    public static final int SHOCK_COOLDOWN = 40;
    /** The shockwave: radius about a point {@link #SHOCK_AHEAD} blocks ahead of the chest, damage to creatures (the wearer takes none), its push, and the speed it turns a projectile at over the speed it came in. */
    public static final double SHOCK_RADIUS = 3.5, SHOCK_AHEAD = 1.2;
    public static final float SHOCK_DAMAGE = 1f, SHOCK_KNOCK = 1.1f, SHOCK_SPEED = 1.5f;
    /** Upward pop a shockwave gives a light creature, blocks per tick. */
    public static final double SHOCK_POP = 0.35;

    private PunchTuning() {
    }
}
