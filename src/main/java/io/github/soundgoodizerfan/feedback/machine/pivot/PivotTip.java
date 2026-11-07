// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.pivot;

/**
 * What the arm does to each cell it sweeps. The tip is a dumb tool: it applies its operation to whatever stands in the cell, ripe or not,
 * and knows no crop, animal or recipe. The cutting, breaking and striking tips take their strength and wear from a seated machine head
 * (the Tool Table's {@code machine_head}); the others need none.
 */
public enum PivotTip {
    /** Cuts plants, leaves, cane, kelp and the wool of sheep. */
    CUT("cut", true),
    /** Breaks blocks whose hardness the head can overcome; one too hard only wears the head. */
    BREAK("break", true),
    /** Places a block item from the input slot where it can stand: seed, sapling, anything. */
    PLANT("plant", false),
    /** Uses the input item on whatever is there: feed on an animal, bone meal on a crop, shears on a sheep. */
    FEED("feed", false),
    /** Strikes creatures with the head's weight. */
    STRIKE("strike", true),
    /** Sweeps dropped items into the mast's output slots. */
    COLLECT("collect", false);

    private final String id;
    private final boolean needsHead;

    PivotTip(String id, boolean needsHead) {
        this.id = id;
        this.needsHead = needsHead;
    }

    public String id() {
        return id;
    }

    public boolean needsHead() {
        return needsHead;
    }
}
