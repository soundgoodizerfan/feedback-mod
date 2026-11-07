// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.sculk;

import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The death-capture organ: a node on the XP wire that takes the XP of anything dying within reach into its run.
 * It holds nothing and ticks nothing; {@link SculkSites} finds it when a death happens.
 */
public class CatalystTapBlockEntity extends SculkNodeBlockEntity {

    public CatalystTapBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CATALYST_TAP.get(), pos, state);
    }

    @Override
    protected void joined() {
        SculkSites.addTap(level, worldPosition);
    }
}
