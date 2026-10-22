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
package io.github.soundgoodizerfan.feedback.combat.feel;

/**
 * Every figure of the impact feel, in one place so it can be dialled in game. The per-impact figures
 * (how long, how bright, how hard) live in {@link Feel}; these are the constants behind them.
 * Feel is a view effect: nothing here changes the tick rate or any rotation, so the crosshair never moves.
 * There is no hit-stop: a freeze was tried and removed (docs/feedback_combat_spec.md 9.3).
 */
public final class FeelTuning {

    /** Preset for a light parry or glancing impact. */
    public static final Feel LIGHT = new Feel(0.55f, 1.5f, 0.04f, false, 8);
    /** Preset for a heavy parry or a big hit. */
    public static final Feel HEAVY = new Feel(0.9f, 4f, 0.10f, true, 24);

    /** Largest camera roll accepted, degrees. */
    public static final float MAX_ROLL_DEG = 20f;
    /** Largest FOV pop accepted, as a fraction of the field of view removed at the peak. */
    public static final float MAX_FOV_POP = 0.5f;
    /** Most spark particles accepted per impact. */
    public static final int MAX_SPARKS = 64;

    /** How long the full-screen flash takes to fade to nothing, milliseconds. About one to two frames at 60 fps. */
    public static final float FLASH_MS = 70f;
    /** Flash colour, RGB. White for the impact frame. */
    public static final int FLASH_RGB = 0xFFFFFF;
    /** Share of the flash a bystander sees (the two parties get all of it). */
    public static final float BYSTANDER_FLASH = 0.35f;
    /** A bystander farther than this many blocks from the impact sees and hears nothing. */
    public static final double BYSTANDER_RANGE = 16.0;

    /** The camera roll decays as exp(-t/this), milliseconds: how long the lurch takes to settle. */
    public static final float ROLL_DECAY_MS = 180f;
    /** The FOV pop decays as exp(-t/this), milliseconds. Shorter than the roll so it reads as a punch. */
    public static final float FOV_DECAY_MS = 120f;

    /** Sparks fly out at about this speed, blocks per tick; heavy impacts scale it by {@link #HEAVY_SPARK_BOOST}. */
    public static final float SPARK_SPEED = 0.35f;
    public static final float HEAVY_SPARK_BOOST = 1.5f;
    /** How far in front of the eye the sparks and sound sit for the command, blocks. */
    public static final double COMMAND_DISTANCE = 2.0;

    /** Sound loudness by band. */
    public static final float LIGHT_VOLUME = 0.9f, HEAVY_VOLUME = 1.4f;
    /** Sound pitch by band, before the random spread. */
    public static final float LIGHT_PITCH = 1.1f, HEAVY_PITCH = 0.85f;
    /** Random pitch spread either side, so repeated hits do not machine-gun one sample. */
    public static final float PITCH_SPREAD = 0.06f;

    /** Parry raised: loudness and pitch of the swish heard around the fighter. */
    public static final float RAISE_VOLUME = 0.7f, RAISE_PITCH = 1.3f;
    /** Roll begun: the whoosh. */
    public static final float ROLL_VOLUME = 0.8f, ROLL_PITCH = 1.0f;
    /** A window that caught nothing: low and dull, so it cannot be mistaken for the bright parry hit. */
    public static final float WHIFF_VOLUME = 0.8f, WHIFF_PITCH = 0.6f;
    /** Share of the whiff volume a bystander hears. */
    public static final float WHIFF_BYSTANDER = 0.3f;
    /** The whiff's puff: particle count, and how far in front of the eye (blocks) it sits. */
    public static final int WHIFF_PUFF_COUNT = 4;
    public static final double WHIFF_PUFF_REACH = 0.8;
    /** A press the fighter cannot use (recovering, roll cooldown, nothing that parries): a small click to that player only. */
    public static final float DENY_VOLUME = 0.5f, DENY_PITCH = 0.7f;
    /** Client ticks between deny clicks, so a held or hammered key is not a machine gun. */
    public static final int DENY_MIN_TICKS = 10;
    /** Play a subtle tick to the fighter when recovery or the roll cooldown ends and they are free again. */
    public static final boolean READY_CUE = true;
    public static final float READY_VOLUME = 0.35f, READY_PITCH = 1.8f;

    private FeelTuning() {
    }
}
