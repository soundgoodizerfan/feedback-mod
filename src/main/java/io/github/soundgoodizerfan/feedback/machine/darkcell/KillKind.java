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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
