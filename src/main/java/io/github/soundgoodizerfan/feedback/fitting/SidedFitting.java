// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting;

/**
 * A fitting that occupies one face of its holder -- a {@link SensorFitting}, an
 * {@link AdapterFitting} or a {@link RedstoneAnalogFitting}, and only ever one of those per face, the same one-per-side rule
 * GTCEu's covers use.
 * <p>
 * Sealed rather than left open: {@link Fittable} stores exactly one of these per
 * {@code Direction}, and a third implementor showing up later would be a fourth fitting kind,
 * not a variation on this one -- see {@link Fitting} for why that gets its own type instead.
 */
public sealed interface SidedFitting extends Fitting permits SensorFitting, AdapterFitting, RedstoneAnalogFitting {
}
