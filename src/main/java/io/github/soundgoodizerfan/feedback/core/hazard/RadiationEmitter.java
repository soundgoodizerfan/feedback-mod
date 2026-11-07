// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

/**
 * A block entity that is itself a radiation source: a running reactor core, a rod in its channel, the slag of a breached core.
 * {@link Radiation#rate} sums {@link #radiationRu} with the blocks and containers in reach, through the same walls.
 */
public interface RadiationEmitter {

    /** The dose rate this gives at point blank, in Ru. */
    float radiationRu();
}
