// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
