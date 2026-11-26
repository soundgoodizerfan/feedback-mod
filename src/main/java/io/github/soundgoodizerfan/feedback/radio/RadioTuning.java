// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.radio;

/** Figures for the radio set. Stand-ins until played. */
public final class RadioTuning {

    private RadioTuning() {
    }

    /** Bands a set can be tuned to, 1 to this. */
    public static final int BANDS = 8;
    /** A band's antenna length in mm is {@code QUARTER_WAVE_MM_AT_BAND_ONE / band}: a higher band is a shorter wave and wants a shorter antenna. */
    public static final int QUARTER_WAVE_MM_AT_BAND_ONE = 2400;
    /** Antenna length one wire adds, mm, and the longest antenna a set holds. */
    public static final int ANTENNA_MM_PER_WIRE = 100, MAX_ANTENNA_MM = 2500;
    /** How wide a band's resonance is, as a share of its own quarter wave: the antenna's gain falls off as a bell of this width. */
    public static final float RESONANCE_WIDTH = 0.35f;
    /** The gain of an antenna far off its band (a stub still hears a little). */
    public static final float GAIN_FLOOR = 0.15f;
    /** Reach, blocks, between two sets with perfectly tuned antennas. */
    public static final float BASE_REACH_BLOCKS = 160f;
    /** Link quality (one at no distance, zero at the edge of reach) below which packets start to drop, and the share dropped at the very edge. */
    public static final float GARBLE_BELOW = 0.35f, DROP_AT_EDGE = 0.85f;
    /** The microphone's hearing, blocks; the loudness a relayed sound keeps and its pitch (a little flat: the band is narrow); the sounds relayed per speaker per tick. */
    public static final int HEAR_BLOCKS = 16, RELAY_PER_TICK = 4;
    public static final float RELAY_VOLUME = 0.7f, RELAY_PITCH = 0.9f;
}
