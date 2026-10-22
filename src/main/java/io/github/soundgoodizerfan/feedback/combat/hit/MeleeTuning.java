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

/**
 * Figures of the melee-branch moves bought in the skill tree (blade flourish, mace quake, spear vault).
 * All placeholders to be felt in play. Ticks are server ticks.
 */
public final class MeleeTuning {

    /** Blade flourish: ticks a landed blade hit takes off the cooldown still running (the roll's, which a parry and a kick also wait on). */
    public static final int FLOURISH_SHAVE = 10;

    /** Mace quake: a blow of at least this much damage (hp) is a full-weight slam; a feeble tap does not shake the ground. */
    public static final float QUAKE_MIN_DAMAGE = 5f;
    /** Blocks around the struck body whose neighbours are shaken. */
    public static final double QUAKE_RADIUS = 2.5;
    /** Knockback strength of the shake on a neighbour, away from the struck body. */
    public static final float QUAKE_KNOCK = 0.4f;

    /** Spear vault: upward and forward speed (blocks per tick) given to a sprinting wielder whose thrust lands, and ticks before the next vault. */
    public static final double VAULT_UP = 0.55, VAULT_FORWARD = 0.35;
    public static final int VAULT_COOLDOWN = 40;

    private MeleeTuning() {
    }
}
