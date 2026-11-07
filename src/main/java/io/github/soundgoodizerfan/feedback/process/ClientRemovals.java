// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.List;

/**
 * The client's copy of the removal table -- {@link ClientDeformations}'s own reasoning, unchanged,
 * for the removal family. See that class for why this exists and why it must not import JEI.
 */
public final class ClientRemovals {

    private static List<Removal> entries = List.of();
    private static Runnable listener = () -> {
    };

    private ClientRemovals() {
    }

    public static List<Removal> get() {
        return entries;
    }

    public static void set(List<Removal> incoming) {
        entries = List.copyOf(incoming);
        listener.run();
    }

    /** Called whenever the table is replaced. One listener is enough; only JEI wants this. */
    public static void onChanged(Runnable onChanged) {
        listener = onChanged;
        onChanged.run();
    }
}
