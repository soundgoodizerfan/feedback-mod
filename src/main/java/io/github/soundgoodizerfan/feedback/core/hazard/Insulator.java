// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Something worn or held that stands between a hand and a hot or live thing.
 *
 * <h2>A property of the item, not a list of items</h2>
 * {@link Protection} asks a stack whether it implements this; it never asks which item it is. A
 * pair of gloves and a pair of tongs protect in different ways (a glove holds the heat off the
 * palm, tongs hold the piece away from the hand entirely and do nothing about a wire), and each
 * says so with its own two answers rather than the caller knowing either.
 */
public interface Insulator {

    /**
     * The hottest thing, in Tu, this lets a hand hold or touch without being hurt, in the room
     * {@code level} is in. Below the contact band's floor everything is safe anyway, so an answer
     * lower than that simply protects nothing.
     */
    float handlesUpToTu(ItemStack stack, Level level);

    /**
     * The highest voltage of a bare conductor this stops, or {@code 0} when it stops none. Tongs
     * hold a piece away from the hand and are no use against a wire; a dry glove is.
     */
    default float shockProofVolts(ItemStack stack, Level level) {
        return 0f;
    }
}
