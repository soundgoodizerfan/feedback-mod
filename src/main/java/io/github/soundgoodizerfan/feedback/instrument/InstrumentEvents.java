// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.instrument;

import java.util.ArrayList;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.item.InstrumentBagItem;
import io.github.soundgoodizerfan.feedback.item.ItemTuning;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Misuse that is not a reading: a hard fall knocks every instrument the player carries off true (in either direction), and standing
 * in heat past what an instrument's glass and fill stand ({@link ItemTuning#HEAT_SOAK_TU}) soaks it so it reads high. A bag's
 * contents are carried instruments too.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class InstrumentEvents {

    private InstrumentEvents() {
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getDistance() >= ItemTuning.DRIFT_FALL_BLOCKS)
            knock(player, player.getRandom().nextBoolean() ? 1 : -1);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % ItemTuning.HEAT_SOAK_INTERVAL == 0
                && HeatSource.at(player.level(), player.blockPosition()).value() > ItemTuning.HEAT_SOAK_TU)
            knock(player, 1);
    }

    /** Moves every quantity of every instrument the player carries, in the bag too, by one misuse in {@code sign}'s direction. */
    private static void knock(ServerPlayer player, float sign) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof InstrumentBagItem) {
                var inside = new ArrayList<>(InstrumentBagItem.contents(stack));
                inside.forEach(item -> nudge(item, sign));
                InstrumentBagItem.store(stack, inside);
            } else
                nudge(stack, sign);
        }
    }

    private static void nudge(ItemStack stack, float sign) {
        if (!(stack.getItem() instanceof Instrument item))
            return;
        for (Quantity quantity : Quantity.values())
            if (item.canRead(quantity))
                Instruments.nudge(stack, item, quantity, sign * ItemTuning.DRIFT_STEPS_PER_MISUSE);
    }
}
