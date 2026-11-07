// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.thermal;

/** A cold store a heat pump beside it can hold cold (food spec 8, Refrigeration); found by this, not by block. */
public interface Refrigerable {

    /** The pump is running now: hold the store at {@code setpoint} (or the ice's, if colder) for a short lapse. */
    void holdCold(io.github.soundgoodizerfan.feedback.core.unit.Tu setpoint);
}
