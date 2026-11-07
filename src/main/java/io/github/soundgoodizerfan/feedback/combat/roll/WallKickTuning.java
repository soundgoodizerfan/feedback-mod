// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.roll;

/** The wall kick's figures. Placeholders to be felt in play. Ticks are server ticks; speeds are blocks per tick. */
public final class WallKickTuning {

    /** How long after sprinting or rolling on the ground the momentum still counts (1.5 s: a sprint-jump into a wall). */
    public static final int MOMENTUM_TICKS = 24;
    /** Speed given away from the wall, and upward (a normal jump is 0.42). */
    public static final double PUSH = 0.5, UP = 0.55;
    /** Stamina one wall kick spends; hunger is paid when it refills ({@code Stamina.EXHAUSTION_PER_POINT}). */
    public static final float STAMINA = 10f;

    private WallKickTuning() {
    }
}
