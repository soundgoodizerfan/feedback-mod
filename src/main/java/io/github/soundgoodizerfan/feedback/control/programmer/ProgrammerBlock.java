// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.programmer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Where a Punch Card's program is authored. Right click to open the node editor. */
public class ProgrammerBlock extends Block implements EntityBlock {

    private final boolean electric;

    public ProgrammerBlock(Properties properties) {
        this(properties, false);
    }

    /** {@code electric}: the Electric Programmer, which takes every program medium; the plain one takes the Punch Card alone. */
    public ProgrammerBlock(Properties properties, boolean electric) {
        super(properties);
        this.electric = electric;
    }

    public boolean electric() {
        return electric;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        ProgrammerBlockEntity programmer = ProgrammerBlockEntity.at(level, pos);
        if (programmer == null)
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            programmer.sync();
            player.openMenu(programmer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ProgrammerBlockEntity programmer)
            Containers.dropContents(level, pos, programmer);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProgrammerBlockEntity(pos, state);
    }
}
