// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.function.Supplier;

import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Fits a Controller upgrade (Memory Drum, Card Rack, Escapement) into whichever {@link Fittable}
 * was clicked that says it will honour one -- same shape as {@link RefractoryLiningItem}, with the
 * fitting supplied so the three share a class. The holder decides; nothing here names a block.
 */
public class ControllerUpgradeItem extends Item {

    private final Supplier<UpgradeFitting> fitting;

    public ControllerUpgradeItem(Properties properties, Supplier<UpgradeFitting> fitting) {
        super(properties);
        this.fitting = fitting;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (!(be instanceof Fittable fittable))
            return InteractionResult.PASS;

        UpgradeFitting upgrade = fitting.get();
        if (!fittable.canAdd(upgrade))
            return InteractionResult.PASS;

        if (!level.isClientSide) {
            fittable.addUpgrade(upgrade);
            upgrade.onAttached();

            Player player = context.getPlayer();
            if (player != null && !player.getAbilities().instabuild)
                stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
