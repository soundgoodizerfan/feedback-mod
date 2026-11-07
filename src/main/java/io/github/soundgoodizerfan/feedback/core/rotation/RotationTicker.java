// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.rotation;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Advances every rotation network once per level tick.
 * <p>
 * Momentum belongs to the network, so it has to be advanced once per network. Ticking it from
 * the blocks instead would advance it once per member, and a longer shaft would spin up faster.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public class RotationTicker {

    /** Nodes loaded or placed last tick join their networks before anything ticks. */
    @SubscribeEvent
    public static void onLevelTickStart(LevelTickEvent.Pre event) {
        if (!event.getLevel().isClientSide)
            RotationNetworks.of(event.getLevel()).flushAdded(event.getLevel());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide)
            return;
        RotationNetworks.of(event.getLevel()).tickAll();
    }
}
