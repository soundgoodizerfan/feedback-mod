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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The miner's pick (spec 5.4): breaking an ore also breaks the connected ore of the same block, up
 * to {@code cap} more (default 8). Cost: {@code wear_per_block} (default 1) extra wear for each,
 * on top of what breaking it already charges, and it stops short of the tool's last point. Needs
 * the right tool for each block, and sneaking switches it off. Ore is a block tag, never an id.
 */
public final class Vein implements ModularTrait {

    /** The extra breaks fire the break event too; this keeps them from starting a vein of their own. */
    private static boolean breaking;

    @Override
    public void onBlockBreak(BlockEvent.BreakEvent event, ItemStack weapon, Params params) {
        if (breaking || !(event.getPlayer() instanceof ServerPlayer player) || player.isShiftKeyDown())
            return;
        BlockState origin = event.getState();
        if (!origin.is(Tags.Blocks.ORES) || !player.hasCorrectToolForDrops(origin))
            return;
        int cap = (int) params.get("cap", 8);
        int wear = (int) params.get("wear_per_block", 1);

        // Flood fill over the 26 neighbours: veins run diagonally.
        Block block = origin.getBlock();
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> found = new ArrayList<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(event.getPos());
        queue.add(event.getPos());
        while (!queue.isEmpty() && found.size() < cap) {
            BlockPos at = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                if (found.size() >= cap || !seen.add(next.immutable()))
                    continue;
                if (event.getLevel().getBlockState(next).is(block)) {
                    found.add(next.immutable());
                    queue.add(next.immutable());
                }
            }
        }

        breaking = true;
        try {
            for (BlockPos pos : found) {
                // Never the last point: the tool is left broken, not destroyed.
                if (weapon.isEmpty() || weapon.getMaxDamage() - 1 - weapon.getDamageValue() <= wear + 1)
                    break;
                if (!player.hasCorrectToolForDrops(player.level().getBlockState(pos)) || !player.gameMode.destroyBlock(pos))
                    continue;
                weapon.hurtAndBreak(ModularStacks.cap(weapon, wear), player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            }
        } finally {
            breaking = false;
        }
    }
}
