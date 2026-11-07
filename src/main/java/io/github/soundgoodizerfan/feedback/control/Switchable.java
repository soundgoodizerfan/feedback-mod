// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control;

/**
 * Something a control device can start and stop.
 *
 * <h2>Why this interface exists at all</h2>
 * Philosophy 13: the controller is a switch, never a dial. Its only output is starting or
 * stopping a supply, so everything that can be controlled presents exactly this — one boolean,
 * no target, no setpoint, no proportional anything.
 * <p>
 * Keeping it this narrow is what lets slice 2's controller <em>replace</em> the Timer instead of
 * being a new concept bolted alongside it. Anything that can switch a clutch can switch every
 * future actuator, and no actuator ever needs to know what is switching it.
 */
public interface Switchable {

    void setEngaged(boolean engaged);

    boolean isEngaged();
}
