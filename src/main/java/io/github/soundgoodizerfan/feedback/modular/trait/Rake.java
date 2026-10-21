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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The rake's gathering. Breaking a crop draws every dropped item within {@code radius} blocks of
 * it to the wielder's feet a tick later, so a harvest lands in the hand rather than the furrow.
 * Cost: {@code wear_per_stack} wear for each stack drawn, and the light head hits weakly.
 * Sneaking leaves the drops where they fall.
 */
public final class Rake implements ModularTrait {

    @Override
    public void onBlockBreak(BlockEvent.BreakEvent event, ItemStack weapon, Params params) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isShiftKeyDown() || !event.getState().is(BlockTags.CROPS))
            return;
        BlockPos at = event.getPos();
        double radius = params.get("radius", 4f);
        int wear = (int) params.get("wear_per_stack", 1);
        var server = player.getServer();
        server.tell(new TickTask(server.getTickCount() + 1, () -> {
            for (ItemEntity drop : player.level().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(radius))) {
                if (ModularStacks.broken(weapon))
                    return;
                drop.setPos(player.getX(), player.getY(), player.getZ());
                drop.setNoPickUpDelay();
                weapon.hurtAndBreak(ModularStacks.cap(weapon, wear), player, EquipmentSlot.MAINHAND);
            }
        }));
    }
}
