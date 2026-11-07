// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.roll;

/**
 * The roll's base figures (implementation plan 2.3). Gear and skills will move them later; the base is
 * free. Ticks are server ticks (20 a second).
 */
public final class RollTuning {

    /** Ticks of the roll itself, and of invulnerability inside it: the whole roll is dodge. */
    public static final int I_FRAMES = 8;
    /** Ticks open after the roll ends. */
    public static final int RECOVERY = 12;
    /** Ticks after the recovery before another roll (or a parry) may start. */
    public static final int COOLDOWN = 30;
    /**
     * Horizontal speed set at the start, blocks per tick. Ground friction (0.6 slip times 0.91) keeps
     * 0.546 of it each tick, so the slide runs v / (1 - 0.546) = 2.2 v blocks: 1.36 gives about 3.
     */
    public static final double SPEED = 1.36;

    private RollTuning() {
    }
}
