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
package io.github.soundgoodizerfan.feedback.modular;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A sickle: cutting a crop or a leaf takes its neighbours on the same level too. One extra point
 * of wear for every four taken, so reaping a field costs an edge what the work is worth.
 */
public class ModularSickle extends ModularItem {

    public ModularSickle(Properties properties) {
        super(properties);
    }

    private static boolean reapable(BlockState state) {
        return state.is(BlockTags.CROPS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.MINEABLE_WITH_HOE);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.mineBlock(stack, level, state, pos, miner);
        if (level.isClientSide || !reapable(state) || ModularStacks.broken(stack))
            return result;
        int taken = 0;
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1)))
            if (!near.equals(pos) && reapable(level.getBlockState(near)) && level.destroyBlock(near.immutable(), true, miner))
                taken++;
        if (taken >= 4)
            stack.hurtAndBreak(taken / 4, miner, EquipmentSlot.MAINHAND);
        return result;
    }
}
