// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

/**
 * A counter bumped whenever a rack, rail, shuttle head or dock is placed, broken or loaded. A dock keeps its read of the
 * aisle until the counter moves, then reads it again the next time it needs it, so an idle aisle costs nothing.
 */
public final class AisleChanges {

    private static int generation = 1;

    private AisleChanges() {
    }

    public static void bump() {
        generation++;
    }

    public static int generation() {
        return generation;
    }
}
