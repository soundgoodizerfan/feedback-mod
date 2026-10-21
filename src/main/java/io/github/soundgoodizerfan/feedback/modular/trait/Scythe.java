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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The scythe's sweep. Cutting a crop, leaf or plant also cuts the ring of such blocks two out on
 * its level, on top of the ring one out every sickle takes: a five-by-five. Cost: one wear per
 * {@code per_wear} blocks taken in the outer ring, a heavy slow swing, and a blow on a mob lands
 * at {@code mob_mult}. Sneaking cuts one.
 */
public final class Scythe implements ModularTrait {

    private static boolean reapable(BlockState state) {
        return state.is(BlockTags.CROPS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.MINEABLE_WITH_HOE);
    }

    @Override
    public void onBlockBreak(BlockEvent.BreakEvent event, ItemStack weapon, Params params) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isShiftKeyDown() || !reapable(event.getState())
                || ModularStacks.broken(weapon))
            return;
        BlockPos at = event.getPos();
        int taken = 0;
        for (BlockPos near : BlockPos.betweenClosed(at.offset(-2, 0, -2), at.offset(2, 0, 2))) {
            if (Math.max(Math.abs(near.getX() - at.getX()), Math.abs(near.getZ() - at.getZ())) == 2
                    && reapable(player.level().getBlockState(near)) && player.level().destroyBlock(near.immutable(), true, player))
                taken++;
        }
        int wear = taken / Math.max(1, (int) params.get("per_wear", 3));
        if (wear > 0)
            weapon.hurtAndBreak(ModularStacks.cap(weapon, wear), player, EquipmentSlot.MAINHAND);
    }

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        event.setAmount(event.getAmount() * params.get("mob_mult", 0.7f));
    }
}
