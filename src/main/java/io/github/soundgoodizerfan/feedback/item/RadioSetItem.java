// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.List;

import io.github.soundgoodizerfan.feedback.radio.RadioSets;
import io.github.soundgoodizerfan.feedback.radio.RadioTuning;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A hand-held radio set. Sneak-use steps its band; use with copper wire in the other hand winds a hundred millimetres more onto its antenna, up to
 * its limit, and a made antenna ({@code ModularAntenna}) used with the set, or clicked onto it, replaces the antenna with its own length and conductor; each band wants an antenna of its own length (a quarter wave), and the closer the length the better the set hears and is heard. Held to
 * talk, the voice chat compat ({@code compat/voicechat}) carries a speaker's voice to every set on the same band within reach.
 */
public class RadioSetItem extends Item {

    public RadioSetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack set = player.getItemInHand(hand);
        if (level.isClientSide)
            return InteractionResultHolder.success(set);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!player.isShiftKeyDown() && other.is(io.github.soundgoodizerfan.feedback.registry.FTags.RADIO_ANTENNA_WIRE)) {
            int now = RadioSets.antennaMm(set);
            if (now >= RadioTuning.MAX_ANTENNA_MM) {
                player.displayClientMessage(Component.translatable("message.feedback.radio.antenna_full"), true);
                return InteractionResultHolder.fail(set);
            }
            other.shrink(1);
            set.set(FWeapons.RADIO_ANTENNA_MM.get(), Math.min(RadioTuning.MAX_ANTENNA_MM, now + RadioTuning.ANTENNA_MM_PER_WIRE));
            player.displayClientMessage(Component.translatable("message.feedback.radio.antenna", RadioSets.antennaMm(set)), true);
            return InteractionResultHolder.success(set);
        }
        if (player.isShiftKeyDown()) {
            int next = RadioSets.band(set) % RadioTuning.BANDS + 1;
            set.set(FWeapons.RADIO_BAND.get(), next);
            player.displayClientMessage(Component.translatable("message.feedback.radio.band", next), true);
            return InteractionResultHandling.consume(set);
        }
        return InteractionResultHolder.pass(set);
    }

    /** A battery on the cursor is loaded into the set to transmit on ({@link io.github.soundgoodizerfan.feedback.radio.RadioCell}). */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack set, ItemStack other, net.minecraft.world.inventory.Slot slot, net.minecraft.world.inventory.ClickAction action,
                                            Player player, net.minecraft.world.entity.SlotAccess access) {
        if (other.getItem() instanceof io.github.soundgoodizerfan.feedback.modular.item.ModularAntenna && action == net.minecraft.world.inventory.ClickAction.SECONDARY
                && slot.allowModification(player) && RadioSets.attach(set, other)) {
            other.shrink(1);
            player.displayClientMessage(Component.translatable("message.feedback.radio.antenna_fitted", RadioSets.antennaMm(set)), true);
            return true;
        }
        return io.github.soundgoodizerfan.feedback.radio.RadioCell.load(set, other, slot, action, player, access);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.feedback.radio.band", RadioSets.band(stack)));
        tooltip.add(Component.translatable("tooltip.feedback.radio.antenna", RadioSets.antennaMm(stack)));
        tooltip.add(Component.translatable(io.github.soundgoodizerfan.feedback.radio.RadioCell.charged(stack) ? "tooltip.feedback.radio.cell" : "tooltip.feedback.radio.no_cell"));
    }

    /** InteractionResultHolder shortcuts. */
    private static final class InteractionResultHandling {
        static InteractionResultHolder<ItemStack> consume(ItemStack s) {
            return InteractionResultHolder.consume(s);
        }
    }
}
