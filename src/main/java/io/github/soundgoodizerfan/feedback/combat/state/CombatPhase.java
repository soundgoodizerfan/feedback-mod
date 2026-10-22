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
