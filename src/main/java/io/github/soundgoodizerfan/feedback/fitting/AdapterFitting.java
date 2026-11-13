// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting;

/**
 * A fitting that grants its holder a second energy type to run on -- a motor bolted to a
 * mechanical machine so it also takes electricity, a coil dropped into a crucible so it also
 * heats from electricity rather than only a flame.
 * <p>
 * Sided like a {@link SensorFitting} (it occupies a face, physically), addon-shaped like an
 * {@code Upgrade} (it doesn't read anything, it changes what the holder can do). Empty for now
 * on purpose: what an adapter actually converts to what is a different pairing for every energy
 * combination (electrical→mechanical, electrical→thermal, and philosophy §11's "local
 * conversion covers" lists more), and generalizing that before a second concrete adapter exists
 * to compare against would be guessing at a shape instead of finding one.
 */
public non-sealed interface AdapterFitting extends SidedFitting {
}
