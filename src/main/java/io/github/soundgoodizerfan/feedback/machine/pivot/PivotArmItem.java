// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.pivot;

import net.minecraft.world.item.Item;

/** An arm for the pivot mast: its length in blocks is the field's radius, and the drive load grows with it. */
public class PivotArmItem extends Item {

    private final int length;

    public PivotArmItem(int length, Properties properties) {
        super(properties);
        this.length = length;
    }

    public int length() {
        return length;
    }
}
