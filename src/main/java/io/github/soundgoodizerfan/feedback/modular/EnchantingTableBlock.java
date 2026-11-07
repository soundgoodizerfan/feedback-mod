// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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

    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide)
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new EnchantMenu(id, inventory, ContainerLevelAccess.create(level, pos)),
                    Component.translatable("block.feedback.enchanting_table")));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
