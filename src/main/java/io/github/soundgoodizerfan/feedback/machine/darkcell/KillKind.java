// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.darkcell;

/** The ways a dark cell can kill what is in it, and how much of a kill's XP a Catalyst Tap keeps from each. */
public enum KillKind {
    FALL("fall", 0.6f),
    HEAT("heat", 0.4f),
    CRUSH("crush", 1.0f),
    FUMES("fumes", 0.4f),
    SHOCK("shock", 0.8f),
    RADIATION("radiation", 0.2f);

    private final String id;
    private final float xpShare;

    KillKind(String id, float xpShare) {
        this.id = id;
        this.xpShare = xpShare;
    }

    public String id() {
        return id;
    }

    /** The share of a machine kill's XP that reaches a tap, before the flat machine tax. Invented. */
    public float xpShare() {
        return xpShare;
    }
}
