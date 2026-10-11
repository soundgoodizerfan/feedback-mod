/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.core.rotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

import io.github.soundgoodizerfan.feedback.Feedback;

/**
 * Per-level registry of live rotation networks.
 * <p>
 * Networks are runtime-only and nothing here is saved. On load every node re-announces itself
 * and the networks rebuild from the blocks, which are the real source of truth. Momentum is the
 * one thing lost across a reload, which is a reasonable thing to lose when the chunk was not
 * ticking anyway.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public class RotationNetworks {

    // Not a WeakHashMap: the value reaches the key through its nodes, so it would never clear.
    private static final Map<LevelAccessor, RotationNetworks> PER_LEVEL = new HashMap<>();

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        PER_LEVEL.remove(event.getLevel());
    }

    private final Map<Long, RotationNetwork> networks = new HashMap<>();
    private long nextId;

    public static RotationNetworks of(Level level) {
        return PER_LEVEL.computeIfAbsent(level, l -> new RotationNetworks());
    }

    public RotationNetwork create() {
        RotationNetwork network = new RotationNetwork(nextId++);
        networks.put(network.id, network);
        return network;
    }

    public void discard(long id) {
        networks.remove(id);
    }

    /** Advance every network one tick. Copied first, because a network can discard itself. */
    public void tickAll() {
        if (networks.isEmpty())
            return;
        List<RotationNetwork> snapshot = new ArrayList<>(networks.values());
        for (RotationNetwork network : snapshot)
            network.tick();
    }
}
