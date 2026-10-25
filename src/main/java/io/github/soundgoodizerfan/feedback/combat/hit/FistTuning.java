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
package io.github.soundgoodizerfan.feedback.combat.hit;

/** Figures of the unarmed stance chain ({@link FistStances}). Placeholders to be felt in play. */
public final class FistTuning {

    /** Jab: blocks of extra reach while standing. */
    public static final double JAB_REACH = 0.6;
    /** Vertical speed (blocks per tick) past which a fighter in the air counts as rising or falling. */
    public static final double AIR_SPEED = 0.1;
    /** Lunge: stagger multiplier, and ticks the fighter is guarded (invulnerable) after it lands. */
    public static final float LUNGE_STAGGER = 1.5f;
    public static final int LUNGE_GUARD = 4;
    /** Sweep: blocks around the fighter, half-angle of the arc in degrees, and the share of damage each extra body takes. */
    public static final double SWEEP_RADIUS = 3.0;
    public static final float SWEEP_HALF_DEG = 70f, SWEEP_SHARE = 0.6f;
    /** Chop: blocks per tick the target is driven down. Uppercut (a cast): blocks per tick it is launched up, about seven blocks of height. */
    public static final double DRIVE = 0.8, LAUNCH = 1.25;
    /** Combo: ticks after one move in which the next link counts, and the damage it gains as a fraction. */
    public static final int COMBO_TICKS = 30;
    public static final float COMBO_BONUS = 0.3f;

    private FistTuning() {
    }
}
