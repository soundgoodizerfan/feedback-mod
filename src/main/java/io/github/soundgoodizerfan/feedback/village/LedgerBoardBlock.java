/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.village;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The ledger board: a notice board for the village, placed by the bell or on the plaza. Right-click to read the town's orders, contracts and notices. */
public class LedgerBoardBlock extends Block implements EntityBlock {

    public LedgerBoardBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof LedgerBoardBlockEntity))
            return InteractionResult.PASS;
        sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new BoardMenu(id, inv, pos), Component.translatable("block.feedback.ledger_board")),
                buf -> buf.writeBlockPos(pos));
        if (sp.containerMenu instanceof BoardMenu menu)
            menu.open(sp);
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                                                   net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((LedgerBoardBlockEntity) be).tickServer();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LedgerBoardBlockEntity(pos, state);
    }
}
