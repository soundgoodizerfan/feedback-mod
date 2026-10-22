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
package io.github.soundgoodizerfan.feedback.combat.grab;

/**
 * The grab's figures: a short-rope {@code Tether} with no reel, its source a gloved hand (ruled: a
 * grab needs a source, not a free hand). Placeholders to be felt in play.
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
    /** Blocks per tick of the yank a too-heavy target gives the grabber. */
    public static final double YANK = 0.45;

    private GrabTuning() {
    }
}
