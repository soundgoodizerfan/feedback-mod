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
package io.github.soundgoodizerfan.feedback.modular;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Enchanting Table, which replaces vanilla's the way the Furnace replaces its own
 * ({@code docs/specs/feedback_enchanting_spec.md} 4): same model, loot and recipe, but it opens {@link EnchantMenu}.
 * shortcut: no block entity (the book on the vanilla table was its animation) -- the menu holds the part
 * and gives it back on close, as vanilla's did; add one when the table must keep something or charge.
 */
public class EnchantingTableBlock extends Block {

    public EnchantingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide)
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new EnchantMenu(id, inventory, ContainerLevelAccess.create(level, pos)),
                    Component.translatable("block.feedback.enchanting_table")));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
