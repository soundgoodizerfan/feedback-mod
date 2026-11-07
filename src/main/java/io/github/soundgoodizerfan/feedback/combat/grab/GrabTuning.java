// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.grab;

/**
 * The grab's figures: a short-rope {@code Tether} with no reel. It needs the skill; a worn glove improves it. Placeholders to be felt in play.
 */
public final class GrabTuning {

    /** Blocks from the eye the hand reaches to take hold. */
    public static final double REACH = 2.0;
    /** The tether's rope length, blocks: the held body is kept within this of the grabber. */
    public static final float ROPE = 1.5f;
    /** Damage-taken multiplier on a held body (a held limb is open). Modest on purpose. */
    public static final float VULNERABILITY = 1.25f;
    /** Ticks a hold lasts before the hand tires and lets go (3 s). */
    public static final int MAX_TICKS = 60;
    /** The hold breaks when the held body is farther than this, blocks (the grabber rolled or was flung). */
    public static final double BREAK_DISTANCE = 3.0;
    /** Blocks per tick the rope draws a body that has strayed past {@link #ROPE} back in. Walking is 0.2, so a walk is followed; a roll (1.4) breaks the hold. */
    public static final double PULL = 0.35;
    /** One hit on the grabber of at least this much damage (hp) knocks the hand loose. */
    public static final float BREAK_DAMAGE = 4f;
    /** A target heavier than the grabber by more than this factor (BodyMass) cannot be held: it yanks the grabber instead. */
    public static final float MASS_RATIO = 2f;
    /** What a worn glove adds to the grab: ticks of hold (2 s), and the factor on the heaviest target that can be held. */
    public static final int GLOVE_HOLD = 40;
    public static final float GLOVE_MASS = 1.5f;
    /** Blocks per tick of the yank a too-heavy target gives the grabber. */
    public static final double YANK = 0.45;

    /** Throw: blocks per tick for equal masses (times thrower mass over target mass, within the bounds), the lift, and ticks the thrower is left recovering. */
    public static final double THROW_SPEED = 1.0, THROW_MIN = 0.25, THROW_MAX = 1.8, THROW_UP = 0.3;
    public static final int THROW_RECOVERY = 14;

    private GrabTuning() {
    }
}
