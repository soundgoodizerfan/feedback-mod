// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The scoop's width. Digging a shovel block also digs the {@code side} blocks to each side of it,
 * across the way the wielder faces, each the normal way. Cost: {@code wear_per_block} extra wear
 * for each, on top of a head that swings slower. Sneaking digs one.
 */
public final class Scoop implements ModularTrait {

    @Override
    public void onBlockBreak(BlockEvent.BreakEvent event, ItemStack weapon, Params params) {
        if (Extras.active() || !(event.getPlayer() instanceof ServerPlayer player) || player.isShiftKeyDown()
                || !event.getState().is(BlockTags.MINEABLE_WITH_SHOVEL))
            return;
        int side = (int) params.get("side", 1);
        Direction across = player.getDirection().getClockWise();
        List<BlockPos> spots = new ArrayList<>();
        for (int i = 1; i <= side; i++) {
            spots.add(event.getPos().relative(across, i));
            spots.add(event.getPos().relative(across, -i));
        }
        Extras.dig(player, weapon, spots, BlockTags.MINEABLE_WITH_SHOVEL, (int) params.get("wear_per_block", 1));
    }
}
