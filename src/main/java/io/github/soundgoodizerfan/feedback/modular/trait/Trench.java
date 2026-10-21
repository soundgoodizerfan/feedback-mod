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
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The trench shovel's depth. Digging a shovel block also digs the {@code depth} blocks beyond it,
 * the way the wielder looks (down when looking steeply down or up when steeply up). Cost:
 * {@code wear_per_block} extra wear each, a point more than the scoop's, since a narrow head
 * driven deep takes the strain. Sneaking digs one.
 */
public final class Trench implements ModularTrait {

    @Override
    public void onBlockBreak(BlockEvent.BreakEvent event, ItemStack weapon, Params params) {
        if (Extras.active() || !(event.getPlayer() instanceof ServerPlayer player) || player.isShiftKeyDown()
                || !event.getState().is(BlockTags.MINEABLE_WITH_SHOVEL))
            return;
        Direction along = Math.abs(player.getXRot()) > 50 ? (player.getXRot() > 0 ? Direction.DOWN : Direction.UP) : player.getDirection();
        List<BlockPos> spots = new ArrayList<>();
        for (int i = 1; i <= (int) params.get("depth", 1); i++)
            spots.add(event.getPos().relative(along, i));
        Extras.dig(player, weapon, spots, BlockTags.MINEABLE_WITH_SHOVEL, (int) params.get("wear_per_block", 2));
    }
}
