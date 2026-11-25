// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.voicechat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.radio.RadioNet;
import io.github.soundgoodizerfan.feedback.radio.RadioSets;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The radio set's voice, on Simple Voice Chat's published addon API (compile-only; the mod loads this class itself, nothing here is reached without
 * it). While a person holds push to talk on a radio set, their proximity voice is replaced: each microphone packet goes, by a static channel,
 * to every other person holding a set on the same band in the same level, within the reach their two antennas make ({@link RadioSets#quality}); a
 * link near the edge of its reach loses packets in proportion to how far gone it is ({@link RadioSets#dropShare}), which is how it sounds
 * garbled, at no cost to decode a packet. A transmission ends when the key is let go ({@link RadioNet#onRelease}).
 * <p>
 * Design from the addon API's published documentation only (plugin entry, microphone packet event, static channels); nothing of the mod's source was read.
 */
@ForgeVoicechatPlugin
public class RadioPlugin implements VoicechatPlugin {

    /** One static channel per listener, keeping its targets. */
    private final Map<UUID, StaticAudioChannel> channels = new ConcurrentHashMap<>();
    private volatile VoicechatServerApi api;

    @Override
    public String getPluginId() {
        return Feedback.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
        registration.registerEvent(VoicechatServerStoppedEvent.class, e -> {
            channels.clear();
            api = null;
        });
        RadioNet.onRelease(speaker -> channels.values().forEach(StaticAudioChannel::flush));
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoicechatConnection from = event.getSenderConnection();
        if (from == null || !(from.getPlayer().getPlayer() instanceof ServerPlayer speaker) || !RadioNet.keyed(speaker.getUUID()))
            return;
        ItemStack set = RadioSets.held(speaker);
        if (set.isEmpty()) {
            RadioNet.drop(speaker.getUUID());
            return;
        }
        api = event.getVoicechat();
        if (event.isCancellable())
            event.cancel(); // the radio replaces the proximity voice
        for (ServerPlayer listener : speaker.server.getPlayerList().getPlayers()) {
            if (listener == speaker || listener.level() != speaker.level())
                continue;
            ItemStack theirs = RadioSets.held(listener);
            if (theirs.isEmpty())
                continue;
            float quality = RadioSets.quality(set, theirs, speaker.distanceTo(listener));
            if (quality <= 0f || speaker.getRandom().nextFloat() < RadioSets.dropShare(quality))
                continue;
            StaticAudioChannel channel = channelFor(listener);
            if (channel != null)
                channel.send(event.getPacket());
        }
    }

    private StaticAudioChannel channelFor(ServerPlayer listener) {
        VoicechatServerApi a = api;
        if (a == null)
            return null;
        return channels.computeIfAbsent(listener.getUUID(), id -> {
            VoicechatConnection connection = a.getConnectionOf(id);
            if (connection == null)
                return null;
            StaticAudioChannel channel = a.createStaticAudioChannel(UUID.randomUUID());
            channel.addTarget(connection);
            return channel;
        });
    }
}
