// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.hit;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Barehanded combat mode. While on, a fighter's attacks and fist skills treat both hands as empty,
 * whatever they hold. The client toggles it and tells the server ({@code BareModePayload}); each side
 * keeps its own flag here. Holds no client class, so common code may ask.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class BareMode {

    /** The local player's flag; set only by the client. */
    public static volatile boolean clientOn;
    private static final Set<UUID> SERVER = new HashSet<>();

    private BareMode() {
    }

    public static boolean on(Player player) {
        return player.level().isClientSide ? clientOn && player.isLocalPlayer() : SERVER.contains(player.getUUID());
    }

    public static void set(Player player, boolean on) {
        if (on)
            SERVER.add(player.getUUID());
        else
            SERVER.remove(player.getUUID());
    }

    /** What {@code player} holds in the main hand as far as combat is concerned. */
    public static ItemStack mainHand(Player player) {
        return on(player) ? ItemStack.EMPTY : player.getMainHandItem();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SERVER.remove(event.getEntity().getUUID());
    }
}
