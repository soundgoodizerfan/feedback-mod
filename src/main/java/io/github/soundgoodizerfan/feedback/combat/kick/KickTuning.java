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
package io.github.soundgoodizerfan.feedback.combat.kick;

/**
 * The kick's figures. Every number is a placeholder to be felt in play. Ticks are server ticks (20 a
 * second); impact is joules, the unit the parry's stagger already reads ({@code ParryTuning}).
 */
public final class KickTuning {

    /** Blocks from the kicker's waist the foot reaches, and the grace added round the target's box. */
    public static final double REACH = 1.6, GRACE = 0.3;
    /** Height of the foot's line above the kicker's feet: a kick goes low, under a raised shield's face. */
    public static final double WAIST = 0.9;
    /** Ticks before the next kick (and before a parry or roll: it shares the roll's cooldown slot). */
    public static final int COOLDOWN = 30;
    /** Ticks the foot is out (the leg pose, and the phase {@code KICK}); the rest of {@link #COOLDOWN} follows as idle cooldown. */
    public static final int SWING = 7;
    /** Hunger cost of one kick, in vanilla exhaustion (a sprint-jump costs 0.2). */
    public static final float EXHAUSTION = 0.5f;

    /** Damage of the foot itself. Low on purpose: the kick is for position, not killing. */
    public static final float DAMAGE = 2f;
    /** Joules the kick carries per Mu of the kicker's own stand-in mass (gear included, so a heavier kicker kicks harder). */
    public static final float JOULES_PER_KICKER_MU = 1f;

    /** Knockback strength per J/Mu of impact over the target's mass, within the bounds below. A 100 J kick on an 84 Mu zombie is about 0.6. */
    public static final float KNOCK_PER_SPECIFIC_IMPACT = 0.5f;
    public static final float KNOCK_MIN = 0.15f, KNOCK_MAX = 1.6f;
    /** A kick on a free target staggers it only if the unclamped stagger comes to at least this many ticks (a golem shrugs it off). */
    public static final float STAGGER_MIN_RAW = 10f;

    /** Ticks a broken guard stays down, per J/Mu of impact over the target's mass, within the bounds. */
    public static final float DISABLE_PER_SPECIFIC_IMPACT = 60f;
    public static final int DISABLE_MIN = 20, DISABLE_MAX = 100;
    /** A broken guard always staggers for at least this long. */
    public static final int GUARD_STAGGER_MIN = 10;

    private KickTuning() {
    }
}
