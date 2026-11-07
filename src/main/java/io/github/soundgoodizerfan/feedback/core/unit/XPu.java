// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * Experience as a quantity, in XPu (feedback_mechanics.md 1.2): vanilla's experience points, an integer for the
 * same reason {@link Fu} is. Level is not quantity but quantity over a container's capacity ({@code core.xp.XpStore}), so nothing here knows a
 * level. Not a power currency: no engine converts it to Work; it only conditions a process already running (the
 * Enchanting Table spends it per session).
 */
public record XPu(int value) implements Unit {

    @Override
    public float raw() {
        return value;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.xpu";
    }
}
