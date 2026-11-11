// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import net.minecraft.world.item.Item;

/**
 * Ties two bodies at a joint voxel -- select the joint voxel, then a voxel of the other body beside it; no new body is made
 * and nothing is consumed. The click is handled by {@code MicroCellBlock#useItemOn}, which hands it to {@link MicroLinks}.
 */
public class JointLinkItem extends Item {

    public JointLinkItem(Properties properties) {
        super(properties);
    }
}
