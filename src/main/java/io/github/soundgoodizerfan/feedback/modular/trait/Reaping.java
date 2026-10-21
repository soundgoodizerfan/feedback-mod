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

import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The reaping hook's replant. Cutting a crop plants it again from its own drops: a tick later, one
 * dropped item that plants that same block is taken and the crop stands young where it grew.
 * Cost: {@code wear} per replant, and the hook takes one plant at a time ({@code sweep} 0, read by
 * the sickle). Sneaking cuts without replanting.
 */
public final class Reaping implements ModularTrait {

    @Override
    public void onBlockBreak(BlockEvent.BreakEvent event, ItemStack weapon, Params params) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isShiftKeyDown() || !event.getState().is(BlockTags.CROPS))
            return;
        BlockPos at = event.getPos();
        Block crop = event.getState().getBlock();
        var server = player.getServer();
        server.tell(new TickTask(server.getTickCount() + 1, () -> {
            var level = player.level();
            if (!level.getBlockState(at).isAir() || ModularStacks.broken(weapon))
                return;
            for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(1))) {
                ItemStack held = drop.getItem();
                if (held.getItem() instanceof BlockItem seed && seed.getBlock() == crop && crop.defaultBlockState().canSurvive(level, at)) {
                    held.shrink(1);
                    level.setBlockAndUpdate(at, crop.defaultBlockState());
                    weapon.hurtAndBreak(ModularStacks.cap(weapon, (int) params.get("wear", 1)), player, EquipmentSlot.MAINHAND);
                    return;
                }
            }
        }));
    }
}
