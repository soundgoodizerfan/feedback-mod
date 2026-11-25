// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.radio;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import net.minecraft.server.level.ServerPlayer;

/**
 * Who is holding the talk key on a radio set right now (server side), and who to tell when they let go. The voice chat compat reads {@link #keyed}
 * for every microphone packet and listens for releases to end a transmission; this class knows nothing of the voice chat, so the radio's state
 * and arithmetic ({@link RadioSets}) exist and run whether or not that mod is installed.
 */
public final class RadioNet {

    private static final Set<UUID> KEYED = ConcurrentHashMap.newKeySet();
    private static final CopyOnWriteArrayList<Consumer<UUID>> RELEASED = new CopyOnWriteArrayList<>();

    private RadioNet() {
    }

    public static boolean keyed(UUID player) {
        return KEYED.contains(player);
    }

    /** The key went down or up on {@code player}'s client; it only counts while they hold a radio set. */
    public static void key(ServerPlayer player, boolean down) {
        boolean on = down && !RadioSets.held(player).isEmpty();
        if (on)
            KEYED.add(player.getUUID());
        else if (KEYED.remove(player.getUUID()))
            RELEASED.forEach(c -> c.accept(player.getUUID()));
    }

    /** A person left or lost their set: their transmission ends. */
    public static void drop(UUID player) {
        if (KEYED.remove(player))
            RELEASED.forEach(c -> c.accept(player));
    }

    /** Calls {@code listener} with the speaker whenever a transmission ends. */
    public static void onRelease(Consumer<UUID> listener) {
        RELEASED.add(listener);
    }
}
