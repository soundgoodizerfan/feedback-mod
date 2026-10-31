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
