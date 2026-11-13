// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.instrument;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.item.ItemTuning;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/** Misuse that is not a reading: a hard fall knocks every instrument the player carries off true. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class InstrumentEvents {

    private InstrumentEvents() {
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getDistance() < ItemTuning.DRIFT_FALL_BLOCKS)
            return;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!(stack.getItem() instanceof Instrument item))
                continue;
            float sign = player.getRandom().nextBoolean() ? 1 : -1;
            for (Quantity quantity : Quantity.values())
                if (item.canRead(quantity))
                    Instruments.nudge(stack, item, quantity, sign * ItemTuning.DRIFT_STEPS_PER_MISUSE);
        }
    }
}
