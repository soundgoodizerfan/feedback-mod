// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

/**
 * How an energy weapon's shot looks, told to the clients with every beam, pellet and impact. Chosen from what the gun is
 * ({@code ElectricGunSpec.style}): never an id, and never a rule: the server decides nothing by it.
 */
public final class EnergyStyle {

    /** A thin hard beam. */
    public static final byte LASER = 1;
    /** A slow glowing blob with a trailing glow. */
    public static final byte PLASMA = 2;
    /** A thick blue discharge that crackles. */
    public static final byte PULSE = 3;
    /** A jagged branching bolt. */
    public static final byte ARC = 4;

    private EnergyStyle() {
    }
}
