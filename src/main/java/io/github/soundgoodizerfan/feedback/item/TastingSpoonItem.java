// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.modular.prep.DishProfile;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A hand tool that reads a dish: use it with a dish in the other hand and the dish's profile comes back as words, one line per axis
 * ({@link DishProfile#AXES}, the same figures a villager judges), and as exact figures with an instrument at the truth tier. An
 * instrument is a yield technology, not a key: nothing needs it, it only saves guessing. A tasting wears the spoon a point (a hand tool is consumable by nature).
 */
public class TastingSpoonItem extends Item {

    public TastingSpoonItem(Properties properties) {
        super(properties.stacksTo(1).durability(120));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack spoon = player.getItemInHand(hand);
        ItemStack dish = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!DishProfile.is(dish))
            return InteractionResultHolder.pass(spoon);
        if (level.isClientSide)
            return InteractionResultHolder.success(spoon);
        ModularFigures f = DishProfile.read(dish, level);
        if (f == null)
            return InteractionResultHolder.pass(spoon);
        boolean exact = Instruments.exact(player, Quantity.FIGURE);
        player.displayClientMessage(Component.translatable("tooltip.feedback.tasting.title", dish.getHoverName()), false);
        for (DishProfile.Axis axis : DishProfile.AXES) {
            float v = f.get(axis.fig());
            int band = Math.min(3, Math.max(0, (int) ((v - axis.lo()) / Math.max(1e-3f, axis.hi() - axis.lo()) * 4f)));
            String name = axis.fig().name().toLowerCase(java.util.Locale.ROOT);
            Component line = Component.translatable("village.feedback.term." + name + "." + band);
            player.displayClientMessage(exact
                    ? Component.translatable("tooltip.feedback.tasting.exact", Component.translatable("village.feedback.noun." + name), line, String.format(java.util.Locale.ROOT, "%.2f", v))
                    : Component.translatable("tooltip.feedback.tasting.line", Component.translatable("village.feedback.noun." + name), line), false);
        }
        spoon.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        return InteractionResultHolder.consume(spoon);
    }
}
