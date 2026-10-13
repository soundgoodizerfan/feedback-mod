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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A magazine: carries a {@link Magazine} record of rounds. Loaded by crafting, swapped into a gun with reload. */
public class MagazineItem extends Item {

    public MagazineItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static Magazine rounds(ItemStack stack) {
        return stack.getOrDefault(FWeapons.MAGAZINE.get(), Magazine.EMPTY);
    }

    /** Use unloads: every round back to the player as a cartridge, the magazine left empty. */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,
                                                                     net.minecraft.world.entity.player.Player player,
                                                                     net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Magazine m = rounds(stack);
        if (m.isEmpty())
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            for (Cartridge round : m.rounds()) {
                ItemStack out = new ItemStack(FWeapons.CARTRIDGE_ITEM.get());
                out.set(FWeapons.CARTRIDGE.get(), round);
                GunAmmo.give(player, out);
            }
            stack.remove(FWeapons.MAGAZINE.get());
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Magazine m = rounds(stack);
        tooltip.add(Component.translatable("magazine.feedback.rounds", m.rounds().size(),
                io.github.soundgoodizerfan.feedback.core.FTuning.GUN_MAGAZINE_CAPACITY).withStyle(ChatFormatting.GRAY));
        if (!m.isEmpty())
            tooltip.add(Component.translatable("magazine.feedback.bore", m.boreMm()).withStyle(ChatFormatting.GRAY));
    }
}
