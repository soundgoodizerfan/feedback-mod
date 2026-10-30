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

import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.Fitting;
import io.github.soundgoodizerfan.feedback.fitting.SidedFitting;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Pries a {@link Fitting} off a {@link Fittable}: the sensor on the clicked face if there is one,
 * otherwise the last upgrade fitted. The fitting drops as its item, as when the holder is broken.
 * A hand tool, so it wears with ordinary use (digest, wear rule).
 */
public class CrowbarItem extends Item {

    public CrowbarItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (!(be instanceof Fittable fittable))
            return InteractionResult.PASS;

        Direction side = context.getClickedFace();
        SidedFitting sided = fittable.getSidedFitting(side);
        List<UpgradeFitting> upgrades = fittable.getUpgrades();
        if (sided == null && upgrades.isEmpty())
            return InteractionResult.PASS;

        if (!level.isClientSide) {
            Fitting removed;
            if (sided != null) {
                removed = sided;
                removed.onRemoved();
                fittable.setSidedFitting(side, null);
            } else {
                UpgradeFitting last = upgrades.get(upgrades.size() - 1);
                removed = last;
                removed.onRemoved();
                fittable.removeUpgrade(last);
            }
            Block.popResource(level, context.getClickedPos().relative(side), removed.getPickItem());
            level.playSound(null, context.getClickedPos(), SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.6f, 0.7f);

            Player player = context.getPlayer();
            if (player != null)
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
