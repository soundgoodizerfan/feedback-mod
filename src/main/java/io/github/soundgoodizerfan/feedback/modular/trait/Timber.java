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
import java.util.HashSet;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The felling axe's timber (spec 5.4). Breaking a log fells the connected logs (leaves stay) up to
 * {@code cap} of them within {@code radius} blocks of the first, each broken the normal way so
 * protection and events apply. Cost: {@code wear_per_log} extra wear per log on top of what each
 * break already costs, and it stops short of breaking the tool. Sneaking fells one log.
 */
public final class Timber implements ModularTrait {

    /** Breaking a log from here fires the same event again; this lets it through. */
    private static boolean felling;

    @Override
    public void onBlockBreak(BlockEvent.BreakEvent event, ItemStack weapon, Params params) {
        if (felling || !(event.getPlayer() instanceof ServerPlayer player) || player.isShiftKeyDown()
                || !event.getState().is(BlockTags.LOGS) || !weapon.isCorrectToolForDrops(event.getState()))
            return;
        int cap = (int) params.get("cap", 24);
        int radius = (int) params.get("max_radius", 12);
        int wear = (int) params.get("wear_per_log", 1);
        BlockPos origin = event.getPos();
        Set<BlockPos> seen = new HashSet<>(Set.of(origin));
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(java.util.List.of(origin));
        felling = true;
        try {
            int felled = 0;
            while (!queue.isEmpty() && felled < cap) {
                BlockPos at = queue.poll();
                for (BlockPos next : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                    if (felled >= cap || ModularStacks.broken(weapon))
                        return;
                    if (Math.abs(next.getX() - origin.getX()) > radius || Math.abs(next.getY() - origin.getY()) > radius
                            || Math.abs(next.getZ() - origin.getZ()) > radius || !player.level().getBlockState(next).is(BlockTags.LOGS)
                            || !seen.add(next.immutable()))
                        continue;
                    queue.add(next.immutable());
                    if (player.gameMode.destroyBlock(next)) {
                        felled++;
                        weapon.hurtAndBreak(ModularStacks.cap(weapon, wear), player, EquipmentSlot.MAINHAND);
                    }
                }
            }
        } finally {
            felling = false;
        }
    }
}
