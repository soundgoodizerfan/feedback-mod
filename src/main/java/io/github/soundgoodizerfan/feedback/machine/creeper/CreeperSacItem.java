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
package io.github.soundgoodizerfan.feedback.machine.creeper;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The creeper sac, the explosive organ a shear takes from a penned creeper. It holds the charge the pen grew
 * ({@code FDataComponents.CREEPER_CHARGE}). A hand knife in the other hand cuts it into creeper charge, one item per
 * {@link FTuning#CREEPER_CHARGE_PER_ITEM} units; the remainder is lost.
 */
public class CreeperSacItem extends Item {

    public CreeperSacItem(Properties properties) {
        super(properties);
    }

    public static float charge(ItemStack stack) {
        Float c = stack.get(FDataComponents.CREEPER_CHARGE.get());
        return c == null ? 0f : c;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack sac = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!other.is(FModular.HAND_KNIFE.get()))
            return InteractionResultHolder.pass(sac);
        int count = (int) (charge(sac) / FTuning.CREEPER_CHARGE_PER_ITEM);
        if (count <= 0) {
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("message.feedback.creeper.sac_empty"), true);
            return InteractionResultHolder.fail(sac);
        }
        if (!level.isClientSide) {
            other.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND
                    ? net.minecraft.world.entity.EquipmentSlot.OFFHAND : net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            ItemStack out = new ItemStack(io.github.soundgoodizerfan.feedback.registry.FItems.CREEPER_CHARGE.get(), count);
            if (!player.getInventory().add(out))
                player.drop(out, false);
        }
        sac.shrink(1);
        return InteractionResultHolder.sidedSuccess(sac, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        float c = charge(stack);
        String band = c <= 0f ? "empty" : c < FTuning.CREEPER_CAP * 0.25f ? "small" : c < FTuning.CREEPER_CAP * 0.75f ? "medium" : "large";
        lines.add(Component.translatable("tooltip.feedback.creeper.sac." + band));
        lines.add(Component.translatable("tooltip.feedback.creeper.unstable"));
    }
}
