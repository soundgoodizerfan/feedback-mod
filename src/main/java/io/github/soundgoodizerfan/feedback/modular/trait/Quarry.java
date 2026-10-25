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
package io.github.soundgoodizerfan.feedback.modular.trait;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

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
