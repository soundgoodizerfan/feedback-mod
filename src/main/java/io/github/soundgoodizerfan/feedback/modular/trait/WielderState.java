// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Per-wielder server state for a trait, keyed by UUID and dropped when the entity dies or the
 * player logs out, so a trait keeps nothing on the item and nothing leaks. Server thread only.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class WielderState<T> {

    private static final List<WielderState<?>> ALL = new ArrayList<>();

    private final Map<UUID, T> map = new HashMap<>();

    public WielderState() {
        synchronized (ALL) {
            ALL.add(this);
        }
    }

    public T get(UUID id) {
        return map.get(id);
    }

    public void put(UUID id, T value) {
        map.put(id, value);
    }

    public void remove(UUID id) {
        map.remove(id);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        ALL.forEach(s -> s.map.remove(event.getEntity().getUUID()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ALL.forEach(s -> s.map.remove(event.getEntity().getUUID()));
    }
}
