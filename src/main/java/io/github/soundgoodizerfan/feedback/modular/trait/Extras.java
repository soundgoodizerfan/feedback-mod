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

import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.ModularStacks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Shared by the ground and crop traits: break extra blocks the normal way and bill the tool. */
final class Extras {

    /** Breaking an extra block fires the break event again; this lets it through. */
    private static boolean breaking;

    private Extras() {
    }

    static boolean active() {
        return breaking;
    }

    /** Breaks each of {@code spots} that is in {@code tag} and right for the tool, at {@code wear} extra each. */
    static void dig(ServerPlayer player, ItemStack tool, List<BlockPos> spots, TagKey<Block> tag, int wear) {
        breaking = true;
        try {
            for (BlockPos spot : spots) {
                var state = player.level().getBlockState(spot);
                if (ModularStacks.broken(tool))
                    return;
                if (state.is(tag) && tool.isCorrectToolForDrops(state) && player.gameMode.destroyBlock(spot))
                    tool.hurtAndBreak(ModularStacks.cap(tool, wear), player, EquipmentSlot.MAINHAND);
            }
        } finally {
            breaking = false;
        }
    }
}
