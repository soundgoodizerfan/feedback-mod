// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.radio;

import io.github.soundgoodizerfan.feedback.Feedback;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

/**
 * A set's microphone hears the room (roadmap: compat voice radio, the part that needs no voice mod): while a person holds push to talk on a set,
 * every sound made within {@link RadioTuning#HEAR_BLOCKS} of them (footsteps, doors, machines, mobs) is relayed to everyone holding a set on the same
 * band in the same level, as a quiet, slightly flat copy at the listener's own ear. The link is the one the voice uses: {@link RadioSets#quality}
 * sets how loud it comes through and {@link RadioSets#dropShare} how much of it is lost near the edge of reach. Nothing here touches the voice
 * chat; with it installed the voice goes as well. Each speaker relays at most {@link RadioTuning#RELAY_PER_TICK} sounds a tick, so a noisy
 * machine hall does not flood a listener.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class RadioHearing {

    private RadioHearing() {
    }

    /** The game tick a speaker last relayed in, and how many sounds they have relayed in it. */
    private static final Map<UUID, long[]> TICKS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onSound(PlayLevelSoundEvent.AtPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getSound() == null || event.getSource() == SoundSource.MUSIC
                || event.getSource() == SoundSource.MASTER)
            return;
        Vec3 at = event.getPosition();
        double reach = (double) RadioTuning.HEAR_BLOCKS * RadioTuning.HEAR_BLOCKS;
        for (ServerPlayer speaker : level.players()) {
            if (!RadioNet.keyed(speaker.getUUID()) || speaker.distanceToSqr(at) > reach)
                continue;
            ItemStack set = RadioSets.held(speaker);
            if (set.isEmpty() || !within(level, speaker))
                continue;
            for (ServerPlayer listener : level.getServer().getPlayerList().getPlayers()) {
                if (listener == speaker || listener.level() != level)
                    continue;
                ItemStack theirs = RadioSets.held(listener);
                if (theirs.isEmpty())
                    continue;
                float quality = RadioNet.link(speaker, set, listener, theirs);
                if (quality <= 0f || level.random.nextFloat() < RadioSets.dropShare(quality)) {
                    if (quality > -0.5f)
                        static_(level, listener, quality);
                    continue;
                }
                static_(level, listener, quality);
                listener.connection.send(new ClientboundSoundPacket(event.getSound(), SoundSource.PLAYERS, listener.getX(), listener.getY(), listener.getZ(),
                        Math.min(1f, event.getNewVolume() * RadioTuning.RELAY_VOLUME * quality), event.getNewPitch() * RadioTuning.RELAY_PITCH,
                        level.random.nextLong()));
            }
        }
    }

    /** A marginal link lets a click of static through (a placeholder vanilla sound), more of it the nearer the edge. */
    private static void static_(ServerLevel level, ServerPlayer listener, float quality) {
        if (quality >= RadioTuning.GARBLE_BELOW)
            return;
        float share = (RadioTuning.GARBLE_BELOW - Math.max(0f, quality)) / RadioTuning.GARBLE_BELOW;
        if (level.random.nextFloat() < share * RadioTuning.STATIC_CHANCE)
            listener.connection.send(new ClientboundSoundPacket(net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvents.SCULK_CLICKING), SoundSource.PLAYERS,
                    listener.getX(), listener.getY(), listener.getZ(), RadioTuning.STATIC_VOLUME, 1.4f, level.random.nextLong()));
    }

    /** Spends one of the speaker's relays for this tick; false once they are all spent. */
    private static boolean within(ServerLevel level, ServerPlayer speaker) {
        long now = level.getGameTime();
        long[] slot = TICKS.computeIfAbsent(speaker.getUUID(), id -> new long[] {now, 0});
        if (slot[0] != now) {
            slot[0] = now;
            slot[1] = 0;
        }
        return ++slot[1] <= RadioTuning.RELAY_PER_TICK;
    }
}
