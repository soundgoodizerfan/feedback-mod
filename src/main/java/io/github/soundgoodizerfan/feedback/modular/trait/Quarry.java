// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import java.util.ArrayList;
import java.util.List;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The heavy pick's width. Mining a pickaxe block also mines the blocks around it in the plane of
 * the face struck (3x3 for {@code radius} 1), each the normal way. Cost: {@code wear_per_block}
 * extra wear for each. Sneaking mines one.
 */
public final class Quarry implements ModularTrait {

    @Override
    public void onBlockBreak(BlockEvent.BreakEvent event, ItemStack weapon, Params params) {
        if (Extras.active() || !(event.getPlayer() instanceof ServerPlayer player) || player.isShiftKeyDown()
                || !event.getState().is(BlockTags.MINEABLE_WITH_PICKAXE))
            return;
        // Face struck; if the ray misses the block (lag), fall back to the nearest look axis.
        Direction face = player.pick(player.blockInteractionRange() + 1, 1f, false) instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(event.getPos())
                ? hit.getDirection() : Direction.orderedByNearest(player)[0];
        int r = (int) params.get("radius", 1);
        boolean flat = face.getAxis() == Direction.Axis.Y;
        Direction u = flat ? Direction.EAST : Direction.UP;
        Direction v = flat ? Direction.SOUTH : face.getClockWise();
        List<BlockPos> spots = new ArrayList<>();
        for (int a = -r; a <= r; a++)
            for (int b = -r; b <= r; b++)
                if (a != 0 || b != 0)
                    spots.add(event.getPos().relative(u, a).relative(v, b));
        Extras.dig(player, weapon, spots, BlockTags.MINEABLE_WITH_PICKAXE, (int) params.get("wear_per_block", 1));
    }
}
