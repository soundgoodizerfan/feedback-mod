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

/**
 * A fitting set in a dark cell's wall that kills. It answers one question: how much harm it does, in health points a cycle, to a mob with
 * these facts. A mob it cannot harm (fumes against the undead, fire against a blaze) it answers zero. It knows nothing of mob ids.
 */
public interface KillMethod {

    KillKind kind();

    /** Health points of harm one cycle does to a mob with {@code facts}; zero if this method does nothing to it. */
    float damage(MobFacts facts);

    /** Whether the method can work this cycle at all (powered, fed, hot). */
    default boolean ready() {
        return true;
    }

    /** Called once after a cycle in which it killed {@code killed} mobs of total mass {@code mass}: spend gas, wear the head. */
    default void used(int killed, float mass) {
    }
}
