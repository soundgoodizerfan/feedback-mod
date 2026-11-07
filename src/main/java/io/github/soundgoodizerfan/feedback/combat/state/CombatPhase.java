// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.state;

/** What a fighter is committed to right now. One at a time; the phase decides what else is allowed. */
public enum CombatPhase {
    IDLE,
    /** The window in which a press turns a hit aside. Nested inside the longer commitment below. */
    PARRY_ACTIVE,
    /** The rest of the parry animation: a whiff or late press leaves the fighter open for it. */
    PARRY_RECOVERY,
    /** Dodging: i-frames come from a window inside this phase, see {@link CombatState#iFrames()}. */
    ROLL,
    ROLL_RECOVERY,
    /** The foot is out: a kick's short swing, then the cooldown ({@code then}). Shown as the leg pose. */
    KICK,
    /** Knocked off a swing by an impact against the fighter's mass. */
    STAGGER
}
