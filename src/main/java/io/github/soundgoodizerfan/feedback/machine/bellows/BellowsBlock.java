// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.bellows;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blows into whatever is touching it.
 *
 * <h2>Contact rather than a facing, deliberately</h2>
 * The same choice the Timer made about what it switches. Beat 2 has no data links and should not
 * pretend to, so the wiring stays visible: a player can see what a bellows feeds by looking at
 * what it is bolted to. A facing property would add a placement rule the placeholder art cannot
 * show, which is the mistake the cogs' meshing rule already avoided once.
 */
public class BellowsBlock extends Block implements EntityBlock {

    public BellowsBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BellowsBlockEntity(pos, state);
    }
}
