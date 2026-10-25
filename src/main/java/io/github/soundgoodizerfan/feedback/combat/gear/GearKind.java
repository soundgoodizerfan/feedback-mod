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
package io.github.soundgoodizerfan.feedback.combat.gear;

/**
 * The combat curios and the Curios slot each is worn in. A kind also fixes the recipe's part
 * counts (plates of the shell material, straps of the strap material); no two kinds share a count,
 * which is how {@link GearRecipe} tells them apart without naming an item.
 */
public enum GearKind {
    BRACER("bracer", 2, 1),
    SASH("belt", 1, 2),
    BACK("back", 3, 1),
    GAUNTLET("hands", 2, 2);

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
