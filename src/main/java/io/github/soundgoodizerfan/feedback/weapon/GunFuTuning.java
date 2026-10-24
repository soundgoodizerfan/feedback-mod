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
package io.github.soundgoodizerfan.feedback.weapon;

/**
 * Figures of the gun-fu techniques (skill spec 14.2). Placeholders to be felt in play. Ticks are server ticks.
 */
public final class GunFuTuning {

    /** Brand: Tu the whip takes out of the barrel when it sears (the metal gives its heat to the skin). */
    public static final float BRAND_COOL_TU = 40f;

    /** Muzzle Stagger: ticks after a whip lands in which a point-blank shot at the same target counts, and how close (blocks) is point blank. */
    public static final int MUZZLE_TICKS = 12;
    public static final double MUZZLE_RANGE = 3.0;
    /** STAND-IN: joules of stagger-impact a kg m/s of the round's impulse carries (the kick's own impact over mass then reads it). */
    public static final float MUZZLE_JOULES_PER_IMPULSE = 40f;

    /** Bayonet Drill: damage per point of the bayonet material's hardness, within the bounds, and ticks per ms of the gun's own cycle. */
    public static final float BAYONET_DAMAGE_PER_HARDNESS = 0.5f, BAYONET_DAMAGE_MIN = 3f, BAYONET_DAMAGE_MAX = 12f;
    public static final int BAYONET_MIN_TICKS = 6;

    /** Recoil Vault: blocks per tick of push per (kg m/s of impulse per Mu of the shooter and gun). STAND-IN gain, real recoil is far too small to feel. */
    public static final float VAULT_GAIN = 3f;
    /** Recoil Vault: gun wear per kg m/s of impulse, times (1 + barrel heat) over condition. */
    public static final float VAULT_WEAR_PER_IMPULSE = 0.0004f;
    /** Recoil Vault: blocks within which a surface in front of the muzzle counts as braced against. */
    public static final double VAULT_SURFACE = 1.5;

    /** Braced Fire: the share of barrel heat bloom a braced shooter keeps. */
    public static final float BRACED_HEAT_KEEP = 0.4f;

    /** Hot Clear: the share of the way from the room to cook-off at which the barrel is hot enough to clear. */
    public static final float HOT_CLEAR_LINE = 0.6f;

    /** Marked Rounds: blocks a close shot may be from, ticks the powder burn lasts, most stacks, and melee damage each stack adds. */
    public static final double MARK_RANGE = 4.0;
    public static final int MARK_TICKS = 200, MARK_MAX = 5;
    public static final float MARK_DAMAGE = 1.5f;

    /** Searing Strike: Tu the held item gives up on a landed blow. */
    public static final float SEAR_COOL_TU = 60f;

    /** Coin Flip: ticks the coin is in the air, its strike radius (blocks), launch speed and gravity (blocks per tick, per tick squared). */
    public static final int COIN_TICKS = 16;
    public static final double COIN_RADIUS = 0.6, COIN_UP = 0.42, COIN_GRAVITY = 0.04;
    /** STAND-IN: mB of a coin (its mass is this times the material's density). */
    public static final float COIN_MB = 16f;
    /** Coin Flip: blocks from the struck coin within which a rebound may take a target (Round the Corner triples it). */
    public static final double COIN_RANGE_BLOCKS = 24.0;
    /** Share of the round's momentum the coin takes (times coin mass over round mass). */
    public static final float COIN_MOMENTUM = 0.5f;
    /** Share of speed a rebound keeps at the least (a heavy coin used to drag it to 20%, leaving a 70-damage shot at 3), and a punched-through round keeps. Energy kept is the square. */
    public static final float COIN_KEEP_MIN = 0.9f, COIN_PUNCH_KEEP = 0.95f;

    private GunFuTuning() {
    }
}
