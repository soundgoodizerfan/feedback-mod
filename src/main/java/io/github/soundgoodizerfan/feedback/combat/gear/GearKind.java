// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.gear;

/**
 * The three combat curios and the Curios slot each is worn in. A kind also fixes the recipe's part
 * counts (plates of the shell material, straps of the strap material); no two kinds share a count,
 * which is how {@link GearRecipe} tells them apart without naming an item.
 */
public enum GearKind {
    BRACER("bracer", 2, 1),
    SASH("belt", 1, 2),
    BACK("back", 3, 1);

    private final String slot;
    private final int plates, straps;

    GearKind(String slot, int plates, int straps) {
        this.slot = slot;
        this.plates = plates;
        this.straps = straps;
    }

    /** The Curios slot id. */
    public String slot() {
        return slot;
    }

    public int plates() {
        return plates;
    }

    public int straps() {
        return straps;
    }
}
