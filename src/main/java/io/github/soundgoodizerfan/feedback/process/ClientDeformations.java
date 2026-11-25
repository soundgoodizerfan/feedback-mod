// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.List;

/**
 * The client's copy of the deformation table, kept only so a recipe browser can print it.
 *
 * <h2>Why a copy exists at all</h2>
 * {@link DeformationTable} is datapack data and therefore server-side; philosophy 10 nonetheless
 * says a <em>requirement</em> is free and exact, which means the figures have to reach the browser
 * somehow. So the server ships the table on datapack sync and this holds it.
 * <p>
 * Nothing here participates in the simulation. The hammer never reads this, and no outcome is ever
 * computed from it -- it is a printed handbook, not a second source of truth.
 * <p>
 * This class must not import JEI: it is loaded even when JEI is absent.
 */
public final class ClientDeformations {

    private static List<Deformation> entries = List.of();

    private ClientDeformations() {
    }

    public static List<Deformation> get() {
        return entries;
    }

    public static void set(List<Deformation> incoming) {
        entries = List.copyOf(incoming);
    }
}
