// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
        if (level.isClientSide || !reapable(state) || ModularStacks.broken(stack)
                || ModularTraits.of(stack).map(a -> a.params().get("sweep", 1f)).orElse(1f) == 0)
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
