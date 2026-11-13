// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.blueprint;

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
 * Where a modular item is drawn before it is made. Like
 * a crafting grid it keeps nothing: the drawing lives in the screen until it is printed, and the
 * paper comes back when the screen closes. See {@link BlueprintTableMenu}.
 */
public class BlueprintTableBlock extends Block {

    public BlueprintTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide)
            player.openMenu(new SimpleMenuProvider((id, inventory, p) ->
                    new BlueprintTableMenu(id, inventory, ContainerLevelAccess.create(level, pos)),
                    Component.translatable("block.feedback.blueprint_table")));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
