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
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * Experience as a quantity, in XPu (feedback_mechanics.md 1.2): vanilla's experience points, an integer for the
 * same reason {@link Fu} is. Level is not quantity but quantity over reservoir capacity, so nothing here knows a
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
