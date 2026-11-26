// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.net.FoodClockPayload;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The clock food spoils by: game time minus every tick the server spent with nobody online, so a world nobody
 * is in does not rot its food (a dedicated 1.21.1 server keeps ticking when empty). The offset only grows
 * while the server is empty, so it is constant for as long as anyone is connected: it is sent once at login,
 * and the client's copy never goes stale. Used for every food stamp in {@link Cooking}.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class FoodClock extends SavedData {

    private static final String NAME = "feedback_food_clock", EMPTY = "Empty";

    /** Ticks the server has run with no player connected. */
    private long empty;

    /** The client's copy of the server's offset, set by {@link FoodClockPayload} at login. */
    private static volatile long clientEmpty;

    private static FoodClock of(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(FoodClock::new, FoodClock::load), NAME);
    }

    private static FoodClock load(CompoundTag tag, HolderLookup.Provider lookup) {
        FoodClock clock = new FoodClock();
        clock.empty = tag.getLong(EMPTY);
        return clock;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        tag.putLong(EMPTY, empty);
        return tag;
    }

    /** Game time with the empty-server ticks taken out: the "now" every food stamp is read against. */
    public static long now(Level level) {
        return level.getGameTime() - (level instanceof ServerLevel server ? of(server).empty : clientEmpty);
    }

    public static void setClient(long empty) {
        clientEmpty = empty;
    }

    @SubscribeEvent
    private static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getPlayerCount() > 0)
            return;
        FoodClock clock = of(event.getServer().overworld());
        clock.empty++;
        clock.setDirty();
    }

    @SubscribeEvent
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            PacketDistributor.sendToPlayer(player, new FoodClockPayload(of(player.serverLevel()).empty));
    }
}
