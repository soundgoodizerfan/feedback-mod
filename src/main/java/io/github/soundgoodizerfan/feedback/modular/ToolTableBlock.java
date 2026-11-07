// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * What the benches share: a block that opens
 * its block entity's menu, with a hand hammer that it holds, and drops what it holds. Every bench is the one bench screen: stock is shaped into
 * parts, parts are fitted to parts and tools are edited in place, all in {@link ToolTableMenu}; guns and dishes have their own tables.
 * Nothing is put together in a crafting grid.
 */
public abstract class ToolTableBlock extends BaseEntityBlock {

    public ToolTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ToolTableBlockEntity table))
            return InteractionResult.PASS;
        if (!level.isClientSide)
            player.openMenu(table, buf -> buf.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ToolTableBlockEntity table)
            Containers.dropContents(level, pos, table);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
