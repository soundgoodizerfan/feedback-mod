// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.pivot;

import net.minecraft.world.item.Item;

/** A tip for the pivot mast's arm: one item per {@link PivotTip}. Seated by right-click, taken back with a sneaking empty hand. */
public class PivotTipItem extends Item {

    private final PivotTip tip;

    public PivotTipItem(PivotTip tip, Properties properties) {
        super(properties);
        this.tip = tip;
    }

    public PivotTip tip() {
        return tip;
    }
}
