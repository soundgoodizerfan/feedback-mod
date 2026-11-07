// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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

    /** Named for its size: a small magazine, a drum, a belt. */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.feedback.magazine." + GunAmmo.size(stack).getSerializedName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Magazine m = rounds(stack);
        tooltip.add(Component.translatable("magazine.feedback.rounds", m.rounds().size(),
                GunAmmo.size(stack).capacity()).withStyle(ChatFormatting.GRAY));
        if (!m.isEmpty())
            tooltip.add(Component.translatable("magazine.feedback.bore", m.boreMm()).withStyle(ChatFormatting.GRAY));
    }
}
