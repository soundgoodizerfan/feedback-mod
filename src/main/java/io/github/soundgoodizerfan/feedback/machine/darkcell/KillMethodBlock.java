// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.darkcell;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A block (with or without a block entity) that is a kill method when set in a dark cell's wall. */
public interface KillMethodBlock {

    KillMethod method(Level level, BlockPos pos, BlockState state);
}
